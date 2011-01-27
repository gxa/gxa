package uk.ac.ebi.gxa.loader.service;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.Maker;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Lists.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO.getNetCDFLocation;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 *
 * @author pashky
 */
public class AtlasNetCDFUpdaterService extends AtlasLoaderService {
    public static final Logger log = LoggerFactory.getLogger(AtlasNetCDFUpdaterService.class);
    
    public AtlasNetCDFUpdaterService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    static class CBitSet extends BitSet implements Comparable<CBitSet> {
        private CBitSet(int nbits) {
            super(nbits);
        }

        public int compareTo(CBitSet o) {
            for (int i = 0; i < Math.max(size(), o.size()); ++i) {
                boolean b1 = get(i);
                boolean b2 = o.get(i);
                if (b1 != b2)
                    return b1 ? 1 : -1;
            }
            return 0;
        }
    }

    static class CPair<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends Pair<T1, T2> implements Comparable<CPair<T1, T2>> {
        private CPair(T1 first, T2 second) {
            super(first, second);
        }

        public int compareTo(CPair<T1, T2> o) {
            int d = getFirst().compareTo(o.getFirst());
            return d != 0 ? d : getSecond().compareTo(o.getSecond());
        }
    }

    private EfvTree<CPair<String, String>> matchEfvs(EfvTree<CBitSet> from, EfvTree<CBitSet> to) {
        final List<EfvTree.Ef<CBitSet>> fromTree = matchEfvsSort(from);
        final List<EfvTree.Ef<CBitSet>> toTree = matchEfvsSort(to);

        EfvTree<CPair<String, String>> result = new EfvTree<CPair<String, String>>();
        for (EfvTree.Ef<CBitSet> toEf : toTree) {
            boolean matched = false;
            for (EfvTree.Ef<CBitSet> fromEf : fromTree)
                if (fromEf.getEfvs().size() == toEf.getEfvs().size()) {
                    int i;
                    for (i = 0; i < fromEf.getEfvs().size(); ++i)
                        if (!fromEf.getEfvs().get(i).getPayload().equals(toEf.getEfvs().get(i).getPayload()))
                            break;
                    if (i == fromEf.getEfvs().size()) {
                        for (i = 0; i < fromEf.getEfvs().size(); ++i)
                            result.put(toEf.getEf(), toEf.getEfvs().get(i).getEfv(),
                                    new CPair<String, String>(fromEf.getEf(), fromEf.getEfvs().get(i).getEfv()));
                        matched = true;
                    }
                }
            if (!matched)
                return null;
        }
        return result;
    }

    private List<EfvTree.Ef<CBitSet>> matchEfvsSort(EfvTree<CBitSet> from) {
        final List<EfvTree.Ef<CBitSet>> fromTree = from.getNameSortedTree();
        for (EfvTree.Ef<CBitSet> ef : fromTree) {
            Collections.sort(ef.getEfvs(), new Comparator<EfvTree.Efv<CBitSet>>() {
                public int compare(EfvTree.Efv<CBitSet> o1, EfvTree.Efv<CBitSet> o2) {
                    return o1.getPayload().compareTo(o2.getPayload());
                }
            });
        }
        return fromTree;
    }


    private EfvTree<CBitSet> getEfvPatternsFromAssays(final List<Assay> assays) {
        Set<String> efs = new HashSet<String>();
        for (Assay assay : assays)
            efs.addAll(assay.getPropertyNames());

        EfvTree<CBitSet> efvTree = new EfvTree<CBitSet>();
        int i = 0;
        for (Assay assay : assays) {
            for (final String propName : efs) {
                String value = assay.getPropertySummary(propName);
                efvTree.getOrCreate(propName, value, new Maker<CBitSet>() {
                    public CBitSet make() {
                        return new CBitSet(assays.size());
                    }
                }).set(i, true);
            }
            ++i;
        }

        return efvTree;
    }

    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        String experimentAccession = cmd.getAccession();

        listener.setAccession(experimentAccession);

        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(experimentAccession);

        ListMultimap<String, Assay> assaysByArrayDesign = ArrayListMultimap.create();
        for (Assay assay : assays) {
            String adAcc = assay.getArrayDesignAccession();
            if (null != adAcc)
                assaysByArrayDesign.put(adAcc, assay);
        }

        Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
        final String version = "NetCDF Updater";

        for (String arrayDesignAccession : assaysByArrayDesign.keySet()) {
            ArrayDesign arrayDesign = getAtlasDAO().getArrayDesignByAccession(arrayDesignAccession);

            final File netCDFLocation = getNetCDFLocation(getAtlasNetCDFDirectory(experimentAccession), experiment, arrayDesign);
            listener.setProgress("Reading existing NetCDF");

            final List<Assay> arrayDesignAssays = assaysByArrayDesign.get(arrayDesignAccession);
            log.info("Starting NetCDF for " + experimentAccession +
                    " and " + arrayDesignAccession + " (" + arrayDesignAssays.size() + " assays)");

            NetCDFData netCDF = readNetCDF(this, netCDFLocation, arrayDesignAssays);

            listener.setProgress("Writing updated NetCDF");

            writeNetCDF(netCDFLocation, netCDF, listener, experiment, version, arrayDesign);

            listener.setProgress("Successfully updated the NetCDF");
        }
    }

    private static NetCDFData readNetCDF(AtlasNetCDFUpdaterService atlasNetCDFUpdaterService, File source, List<Assay> arrayDesignAssays) throws AtlasLoaderException {
        NetCDFProxy reader = null;
        try {
            reader = new NetCDFProxy(source);

            NetCDFData result = new NetCDFData();

            final List<Integer> usedAssays = new ArrayList<Integer>();
            final long[] assays = reader.getAssays();
            for (int i = 0; i < assays.length; ++i) {
                for (Assay assay : arrayDesignAssays)
                    if (assay.getAssayID() == assays[i]) {
                        result.assays.add(assay);
                        usedAssays.add(i);
                        break;
                    }
            }

            if (assays.length == result.assays.size()) {
                // We depend on the enclosing class from here
                EfvTree<CBitSet> oldEfvPats = new EfvTree<CBitSet>();
                for (String ef : reader.getFactors()) {
                    String[] efvs = reader.getFactorValues(ef);
                    for (String efv : new HashSet<String>(Arrays.asList(efvs))) {
                        CBitSet pattern = new CBitSet(efvs.length);
                        for (int i = 0; i < efvs.length; ++i)
                            pattern.set(i, efvs[i].equals(efv));
                        oldEfvPats.put(ef, efv, pattern);
                    }
                }

                EfvTree<CBitSet> newEfvPats = atlasNetCDFUpdaterService.getEfvPatternsFromAssays(result.assays);
                result.matchedEfvs = atlasNetCDFUpdaterService.matchEfvs(oldEfvPats, newEfvPats);
                // We depend on the enclosing class up to here
            }

            result.uEFVs = reader.getUniqueFactorValues();

            String[] deAccessions = reader.getDesignElementAccessions();
            result.storage = new DataMatrixStorage(
                    result.assays.size() + (result.matchedEfvs != null ? result.uEFVs.length * 2 : 0), // expressions + pvals + tstats
                    deAccessions.length, 1);
            for (int i = 0; i < deAccessions.length; ++i) {
                final float[] values = reader.getExpressionDataForDesignElementAtIndex(i);
                final float[] pval = reader.getPValuesForDesignElement(i);
                final float[] tstat = reader.getTStatisticsForDesignElement(i);
                result.storage.add(deAccessions[i], concat(
                        multiget(asList(values), usedAssays).iterator(),
                        asList(pval).iterator(),
                        asList(tstat).iterator()));
            }
            return result;
        } catch (IOException e) {
            log.error("Error reading NetCDF file: " + source, e);
            throw new AtlasLoaderException(e);
        } finally {
            closeQuietly(reader);
        }
    }

    private void writeNetCDF(File target, NetCDFData data, AtlasLoaderServiceListener listener, Experiment experiment, String version, ArrayDesign arrayDesign) throws AtlasLoaderException {
        try {
            listener.setProgress("Writing new NetCDF");
            NetCDFCreator netCdfCreator = new NetCDFCreator();

            netCdfCreator.setAssays(data.assays);

            for (Assay assay : data.assays)
                for (Sample sample : getAtlasDAO().getSamplesByAssayAccession(experiment.getAccession(), assay.getAccession()))
                    netCdfCreator.setSample(assay, sample);

            Map<String, DataMatrixStorage.ColumnRef> dataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
            for (int i = 0; i < data.assays.size(); ++i)
                dataMap.put(data.assays.get(i).getAccession(), new DataMatrixStorage.ColumnRef(data.storage, i));

            netCdfCreator.setAssayDataMap(dataMap);

            if (data.matchedEfvs != null) {
                Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                for (EfvTree.EfEfv<CPair<String, String>> efEfv : data.matchedEfvs.getNameSortedList()) {
                    final int oldPos = Arrays.asList(data.uEFVs).indexOf(efEfv.getPayload().getFirst() + "||" + efEfv.getPayload().getSecond());
                    pvalMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                            new DataMatrixStorage.ColumnRef(data.storage, data.assays.size() + oldPos));
                    tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                            new DataMatrixStorage.ColumnRef(data.storage, data.assays.size() + data.uEFVs.length + oldPos));
                }
                netCdfCreator.setPvalDataMap(pvalMap);
                netCdfCreator.setTstatDataMap(tstatMap);
            }

            netCdfCreator.setArrayDesign(arrayDesign);
            netCdfCreator.setExperiment(experiment);
            netCdfCreator.setVersion(version);

            final File tempFile = File.createTempFile(target.getName(), ".tmp");
            netCdfCreator.createNetCdf(tempFile);
            if (!target.delete() || !tempFile.renameTo(target))
                throw new AtlasLoaderException("Can't update original NetCDF file " + target);

            log.info("Successfully finished NetCDF for " + experiment.getAccession() +
                    " and " + arrayDesign.getAccession());

            if (data.matchedEfvs != null)
                listener.setRecomputeAnalytics(false);
        } catch (NetCDFCreatorException e) {
            log.error("Error writing NetCDF file: " + target, e);
            throw new AtlasLoaderException(e);
        } catch (IOException e) {
            log.error("Error writing NetCDF file: " + target, e);
            throw new AtlasLoaderException(e);
        }
    }

    /**
     * Picks a list of elements according to the list of indices supplied
     *
     * @param source  the source to pick elements from
     * @param indices the indices to get
     * @return a list of floats according to the list of indices supplied
     */
    private static <T> List<T> multiget(final List<T> source, List<Integer> indices) {
        return transform(indices,
                new Function<Integer, T>() {
                    public T apply(@Nonnull Integer j) {
                        return source.get(j);
                    }
                });
    }
}
