package uk.ac.ebi.gxa.loader.service;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF;
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
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Iterators.*;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static uk.ac.ebi.gxa.utils.CountIterator.zeroTo;
import static uk.ac.ebi.gxa.utils.FileUtil.extension;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 *
 * @author pashky
 */
public class AtlasNetCDFUpdaterService extends AtlasLoaderService {

    public AtlasNetCDFUpdaterService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    private static class CBitSet extends BitSet implements Comparable<CBitSet> {
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

    private static class CPair<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends Pair<T1, T2> implements Comparable<CPair<T1, T2>> {
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
            for (Property prop : assay.getProperties())
                efs.add(prop.getName());

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

            // TODO: keep the knowledge about filename's meaning in ONE place
            final File originalNetCDF = new File(getAtlasNetCDFDirectory(experimentAccession), experiment.getExperimentID() + "_" + arrayDesign.getArrayDesignID() + ".nc");


            listener.setProgress("Reading existing NetCDF");

            final List<Assay> arrayDesignAssays = assaysByArrayDesign.get(arrayDesignAccession);
            getLog().info("Starting NetCDF for " + experimentAccession +
                    " and " + arrayDesignAccession + " (" + arrayDesignAssays.size() + " assays)");

            // TODO: create an entity encapsulating the following values: it is what we read from NetCDF
            EfvTree<CPair<String, String>> matchedEfvs = null;
            final List<Assay> leaveAssays;
            DataMatrixStorage storage;
            String[] uEFVs;

            NetCDFProxy reader = null;
            try {
                reader = new NetCDFProxy(originalNetCDF);
                leaveAssays = new ArrayList<Assay>(arrayDesignAssays.size());
                final long[] oldAssays = reader.getAssays();
                for (int i = 0; i < oldAssays.length; ++i) {
                    for (Assay assay : arrayDesignAssays)
                        if (assay.getAssayID() == oldAssays[i]) {
                            leaveAssays.add(assay);
                            oldAssays[i] = -1; // mark it as used for later filtering
                            break;
                        }
                }

                if (oldAssays.length == leaveAssays.size()) {
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

                    EfvTree<CBitSet> newEfvPats = getEfvPatternsFromAssays(leaveAssays);
                    matchedEfvs = matchEfvs(oldEfvPats, newEfvPats);
                }

                uEFVs = reader.getUniqueFactorValues();

                String[] deAccessions = reader.getDesignElementAccessions();
                storage = new DataMatrixStorage(
                        leaveAssays.size() + (matchedEfvs != null ? uEFVs.length * 2 : 0), // expressions + pvals + tstats
                        deAccessions.length, 1);
                for (int i = 0; i < deAccessions.length; ++i) {
                    final float[] values = reader.getExpressionDataForDesignElementAtIndex(i);
                    final float[] pval = reader.getPValuesForDesignElement(i);
                    final float[] tstat = reader.getTStatisticsForDesignElement(i);
                    storage.add(deAccessions[i], concat(
                            transform(
                                    filter(
                                            zeroTo(values.length),
                                            new Predicate<Integer>() {
                                                public boolean apply(@Nonnull Integer j) {
                                                    return oldAssays[j] == -1;   // skips deleted assays
                                                }
                                            }),
                                    new Function<Integer, Float>() {
                                        public Float apply(@Nonnull Integer j) {
                                            return values[j];
                                        }
                                    }),
                            asList(pval).iterator(),
                            asList(tstat).iterator()));
                }

            } catch (IOException e) {
                getLog().error("Error reading NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);
                throw new AtlasLoaderException(e);
            } finally {
                closeQuietly(reader);
            }

            try {
                if (!originalNetCDF.delete())
                    throw new AtlasLoaderException("Can't delete original NetCDF file " + originalNetCDF);

                listener.setProgress("Writing new NetCDF");
                NetCDFCreator netCdfCreator = new NetCDFCreator();

                netCdfCreator.setAssays(leaveAssays);

                for (Assay assay : leaveAssays)
                    for (Sample sample : getAtlasDAO().getSamplesByAssayAccession(experimentAccession, assay.getAccession()))
                        netCdfCreator.setSample(assay, sample);

                Map<String, DataMatrixStorage.ColumnRef> dataMap = new HashMap<String, DataMatrixStorage.ColumnRef>();
                for (int i = 0; i < leaveAssays.size(); ++i)
                    dataMap.put(leaveAssays.get(i).getAccession(), new DataMatrixStorage.ColumnRef(storage, i));

                netCdfCreator.setAssayDataMap(dataMap);

                if (matchedEfvs != null) {
                    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
                    for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedEfvs.getNameSortedList()) {
                        final int oldPos = Arrays.asList(uEFVs).indexOf(efEfv.getPayload().getFirst() + "||" + efEfv.getPayload().getSecond());
                        pvalMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                                new DataMatrixStorage.ColumnRef(storage, leaveAssays.size() + oldPos));
                        tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                                new DataMatrixStorage.ColumnRef(storage, leaveAssays.size() + uEFVs.length + oldPos));
                    }
                    netCdfCreator.setPvalDataMap(pvalMap);
                    netCdfCreator.setTstatDataMap(tstatMap);
                }

                netCdfCreator.setArrayDesign(arrayDesign);
                netCdfCreator.setExperiment(experiment);
                netCdfCreator.setVersion(version);

                netCdfCreator.createNetCdf(getAtlasNetCDFDirectory(experimentAccession));
                getLog().info("Successfully finished NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);

                if (matchedEfvs != null)
                    listener.setRecomputeAnalytics(false);
            } catch (NetCDFCreatorException e) {
                getLog().error("Error writing NetCDF for " + experimentAccession +
                        " and " + arrayDesignAccession);
                throw new AtlasLoaderException(e);
            }
        }

        //create or update idf file
        //
        try {
            File idfFile = null;
            File experimentFolder = getAtlasNetCDFDirectory(experimentAccession);
            File[] allIdfFiles = experimentFolder.listFiles(extension("idf", true));
            File[] allSdrfFiles = experimentFolder.listFiles(extension("sdrf", true));
            File[] allNcdfFiles = experimentFolder.listFiles(extension("nc", false));

            List<String> netCdfFiles = new ArrayList<String>();
            for (File file : allNcdfFiles) {
                netCdfFiles.add(file.getName());
            }

            if (allIdfFiles.length > 0)
                idfFile = allIdfFiles[0];

            List<String> sdrfFiles = new ArrayList<String>();
            for (File file : allSdrfFiles) {
                sdrfFiles.add(file.getName());
            }
            if (sdrfFiles.isEmpty()) {
                File emptySdrfFile = new File(experimentFolder, "empty.sdrf");
                emptySdrfFile.createNewFile();
                FileWriter writer = new FileWriter(emptySdrfFile);
                writer.write("hello");
                writer.close();
                sdrfFiles.add("empty.sdrf");
            }

            if (null == idfFile) {
                idfFile = new File(experimentFolder, experimentAccession + ".idf");
                idfFile.createNewFile();
            }

            IDF idf = new IDF();

            idf.addComment("ArrayExpressAccession", experiment.getAccession());
            idf.netCDFFile = netCdfFiles;

            idf.sdrfFile = sdrfFiles;

            BufferedWriter idfWriter = new BufferedWriter(new FileWriter(idfFile));
            idfWriter.write(idf.toString());
            idfWriter.close();
        } catch (Exception ex) {
            throw new AtlasLoaderException(ex);
        }

    }
}
