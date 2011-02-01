package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.CBitSet;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

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
public class AtlasNetCDFUpdaterService extends AtlasLoaderService {
    public static final Logger log = LoggerFactory.getLogger(AtlasNetCDFUpdaterService.class);
    private static final String VERSION = "NetCDF Updater";

    public AtlasNetCDFUpdaterService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        Experiment experiment = getAtlasDAO().getExperimentByAccession(cmd.getAccession());
        String experimentAccession = experiment.getAccession();

        listener.setAccession(experimentAccession);

        List<Assay> allAssays = getAtlasDAO().getAssaysByExperimentAccession(experimentAccession);

        // TODO: add it to the DAO method
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
            NetCDFData data = readNetCDF(netCDFLocation, assayMap);

            listener.setProgress("Writing updated NetCDF");
            writeNetCDF(getAtlasDAO(), netCDFLocation, data, experiment, arrayDesign);

            if (data.isAnalyticsTransferred())
                listener.setRecomputeAnalytics(false);
            listener.setProgress("Successfully updated the NetCDF");
        }
    }

    private static NetCDFData readNetCDF(File source, Map<Long, Assay> knownAssays) throws AtlasLoaderException {
        NetCDFProxy reader = null;
        try {
            reader = new NetCDFProxy(source);

            NetCDFData result = new NetCDFData();

            final List<Integer> usedAssays = new ArrayList<Integer>();
            final long[] assays = reader.getAssays();
            for (int i = 0; i < assays.length; ++i) {
                Assay assay = knownAssays.get(assays[i]);
                if (assay != null) {
                    result.assays.add(assay);
                    usedAssays.add(i);
                }
            }

            if (assays.length == result.assays.size()) {
                result.matchEfvPatterns(getEfvPatterns(reader));
            }

            result.uEFVs = Arrays.asList(reader.getUniqueFactorValues());

            String[] deAccessions = reader.getDesignElementAccessions();
            result.storage = new DataMatrixStorage(result.getWidth(), deAccessions.length, 1);
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

    private static void writeNetCDF(AtlasDAO dao, File target, NetCDFData data, Experiment experiment, ArrayDesign arrayDesign) throws AtlasLoaderException {
        try {
            NetCDFCreator netCdfCreator = new NetCDFCreator();

            netCdfCreator.setAssays(data.assays);

            for (Assay assay : data.assays) {
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
            netCdfCreator.setVersion(VERSION);

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

    private static EfvTree<CBitSet> getEfvPatterns(NetCDFProxy reader) throws IOException {
        EfvTree<CBitSet> patterns = new EfvTree<CBitSet>();
        for (String ef : reader.getFactors()) {
            String[] efvs = reader.getFactorValues(ef);
            for (String efv : distinct(Arrays.asList(efvs))) {
                CBitSet pattern = new CBitSet(efvs.length);
                for (int i = 0; i < efvs.length; i++)
                    pattern.set(i, efvs[i].equals(efv));
                patterns.put(ef, efv, pattern);
            }
        }
        return patterns;
    }
}
