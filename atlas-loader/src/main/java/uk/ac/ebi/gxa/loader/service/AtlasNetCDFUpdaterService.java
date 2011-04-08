package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.CBitSet;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.Experiment;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static uk.ac.ebi.gxa.utils.CollectionUtil.distinct;
import static uk.ac.ebi.gxa.utils.CollectionUtil.multiget;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 *
 * @author pashky
 */
public class AtlasNetCDFUpdaterService {

    public static final Logger log = LoggerFactory.getLogger(AtlasNetCDFUpdaterService.class);
    protected AtlasDAO atlasDAO;
    protected AtlasNetCDFDAO atlasNetCDFDAO;

    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        Experiment experiment = getAtlasDAO().getExperimentByAccession(cmd.getAccession());
        String experimentAccession = experiment.getAccession();

        listener.setAccession(experimentAccession);

        List<Assay> allAssays = getAtlasDAO().getAssaysByExperimentAccession(experimentAccession);

        Map<String, Map<Long, Assay>> assaysByArrayDesign = new HashMap<String, Map<Long, Assay>>();
        for (Assay assay : allAssays) {
            Map<Long, Assay> assays = assaysByArrayDesign.get(assay.getArrayDesignAccession());
            if (assays == null) {
                assaysByArrayDesign.put(assay.getArrayDesignAccession(), assays = new HashMap<Long, Assay>());
            }
            assays.put(assay.getAssayID(), assay);
        }

        for (Map.Entry<String, Map<Long, Assay>> entry : assaysByArrayDesign.entrySet()) {
            ArrayDesign arrayDesign = getAtlasDAO().getArrayDesignByAccession(entry.getKey());

            final File netCDFLocation = getNetCDFDAO().getNetCDFLocation(experiment, arrayDesign);
            listener.setProgress("Reading existing NetCDF");

            final Map<Long, Assay> assayMap = entry.getValue();
            log.info("Starting NetCDF for " + experimentAccession +
                    " and " + entry.getKey() + " (" + assayMap.size() + " assays)");
            NetCDFData data = readNetCDF(getAtlasDAO(), netCDFLocation, assayMap);

            listener.setProgress("Writing updated NetCDF");
            writeNetCDF(getAtlasDAO(), netCDFLocation, data, experiment, arrayDesign);

            if (data.isAnalyticsTransferred())
                listener.setRecomputeAnalytics(false);
            listener.setProgress("Successfully updated the NetCDF");
        }
    }

    private static NetCDFData readNetCDF(AtlasDAO dao, File source, Map<Long, Assay> knownAssays) throws AtlasLoaderException {
        NetCDFProxy proxy = null;
        try {
            proxy = new NetCDFProxy(source);

            NetCDFData data = new NetCDFData();

            final List<Integer> usedAssays = new ArrayList<Integer>();
            final long[] assays = proxy.getAssays();
            for (int i = 0; i < assays.length; ++i) {
                Assay assay = knownAssays.get(assays[i]);
                if (assay != null) {
                    List<Sample> samples = dao.getSamplesByAssayAccession(proxy.getExperimentAccession(), assay.getAccession());
                    data.addAssay(assay, samples);
                    usedAssays.add(i);
                }
            }

            if (assays.length == data.getAssays().size()) {
                data.matchValuePatterns(getValuePatterns(proxy));
            }

            // Get unique values
            List<String> uniqueValues = proxy.getUniqueValues();
            data.setUniqueValues(uniqueValues);

            String[] deAccessions = proxy.getDesignElementAccessions();
            data.setStorage(new DataMatrixStorage(data.getWidth(), deAccessions.length, 1));
            for (int i = 0; i < deAccessions.length; ++i) {
                final float[] values = proxy.getExpressionDataForDesignElementAtIndex(i);
                final float[] pval = proxy.getPValuesForDesignElement(i);
                final float[] tstat = proxy.getTStatisticsForDesignElement(i);
                // Make sure that pval/tstat arrays are big enough if uniqueValues size is greater than proxy.getUniqueFactorValues()
                // i.e. we are in the process of enlarging the uniqueValues set from just efvs to efvs+scvs
                List<Float> pVals = new ArrayList<Float>(asList(pval));
                while (pVals.size() < uniqueValues.size())
                    pVals.add(NetCDFProxy.NA_PVAL_TSTAT); // top up pVals with NA values to the required size
                List<Float> tStats = new ArrayList<Float>(asList(tstat));
                while (tStats.size() < uniqueValues.size())
                    tStats.add(NetCDFProxy.NA_PVAL_TSTAT); // top up tStats with NA values to the required size

                data.addToStorage(deAccessions[i], concat(
                        multiget(asList(values), usedAssays).iterator(),
                        asList(pval).iterator(),
                        asList(tstat).iterator()));
            }
            return data;
        } catch (IOException e) {
            log.error("Error reading NetCDF file: " + source, e);
            throw new AtlasLoaderException(e);
        } finally {
            closeQuietly(proxy);
        }
    }

    private static void writeNetCDF(AtlasDAO dao, File target, NetCDFData data, Experiment experiment, ArrayDesign arrayDesign) throws AtlasLoaderException {
        try {
            NetCDFCreator netCdfCreator = new NetCDFCreator();

            netCdfCreator.setAssays(data.getAssays());

            for (Assay assay : data.getAssays()) {
                List<Sample> samples = dao.getSamplesByAssayAccession(experiment.getAccession(), assay.getAccession());
                for (Sample sample : samples) {
                    netCdfCreator.setSample(assay, sample);
                }
            }

            netCdfCreator.setAssayDataMap(data.getAssayDataMap());
            netCdfCreator.setPvalDataMap(data.getPValDataMap());
            netCdfCreator.setTstatDataMap(data.getTStatDataMap());
            netCdfCreator.setArrayDesign(arrayDesign);
            netCdfCreator.setExperiment(experiment);
            netCdfCreator.setVersion(NetCDFProxy.NCDF_VERSION);

            final File tempFile = File.createTempFile(target.getName(), ".tmp");
            netCdfCreator.createNetCdf(tempFile);
            if (!target.delete() || !tempFile.renameTo(target))
                throw new AtlasLoaderException("Can't update original NetCDF file " + target);

            log.info("Successfully finished NetCDF for " + experiment.getAccession() +
                    " and " + arrayDesign.getAccession());
        } catch (NetCDFCreatorException e) {
            log.error("Error writing NetCDF file: " + target, e);
            throw new AtlasLoaderException(e);
        } catch (IOException e) {
            log.error("Error writing NetCDF file: " + target, e);
            throw new AtlasLoaderException(e);
        }
    }

    private static EfvTree<CBitSet> getValuePatterns(NetCDFProxy reader) throws IOException {
        EfvTree<CBitSet> patterns = new EfvTree<CBitSet>();

        // Store ef-efv patterns
        List<String> efs = Arrays.asList(reader.getFactors());
        for (String ef : efs) {
            List<String> efvs = Arrays.asList(reader.getFactorValues(ef));
            final Set<String> distinctEfvs = distinct(efvs);
            for (String value : distinctEfvs) {
                CBitSet pattern = new CBitSet(efvs.size());
                for (int i = 0; i < efvs.size(); i++)
                    pattern.set(i, efvs.get(i).equals(value));
                patterns.putCaseSensitive(ef, value, pattern);
            }
        }

        // Store sc-scv patterns
        List<String> scs = new ArrayList(Arrays.asList(reader.getCharacteristics()));
        scs.removeAll(efs); // process only scs that aren't also efs
        for (String sc : scs) {
            List<String> scvs = Arrays.asList(reader.getCharacteristicValues(sc));
            final Set<String> distinctScvs = distinct(scvs);
            for (String value : distinctScvs) {
                CBitSet pattern = new CBitSet(scvs.size());
                for (int i = 0; i < scvs.size(); i++)
                    pattern.set(i, scvs.get(i).equals(value));
                patterns.putCaseSensitive(sc, value, pattern);
            }
        }
        return patterns;
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public AtlasNetCDFDAO getNetCDFDAO() {
        return atlasNetCDFDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

}
