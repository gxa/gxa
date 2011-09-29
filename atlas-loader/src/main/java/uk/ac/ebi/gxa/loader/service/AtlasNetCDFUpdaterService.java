package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static uk.ac.ebi.gxa.utils.CollectionUtil.multiget;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 *
 * @author pashky
 */
public class AtlasNetCDFUpdaterService {
    private static final Logger log = LoggerFactory.getLogger(AtlasNetCDFUpdaterService.class);
    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    @Transactional
    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final Experiment experiment = atlasDAO.getExperimentByAccession(cmd.getAccession());

        listener.setAccession(experiment.getAccession());

        Map<String, Map<String, Assay>> assaysByArrayDesign = new HashMap<String, Map<String, Assay>>();
        for (Assay assay : experiment.getAssays()) {
            Map<String, Assay> assays = assaysByArrayDesign.get(assay.getArrayDesign().getAccession());
            if (assays == null) {
                assaysByArrayDesign.put(assay.getArrayDesign().getAccession(), assays = newHashMap());
            }
            assays.put(assay.getAccession(), assay);
        }

        for (Map.Entry<String, Map<String, Assay>> entry : assaysByArrayDesign.entrySet()) {
            ArrayDesign arrayDesign = atlasDAO.getArrayDesignByAccession(entry.getKey());

            final File netCDFLocation = atlasNetCDFDAO.getNetCDFLocation(experiment, arrayDesign);
            listener.setProgress("Reading existing NetCDF");

            final Map<String, Assay> assayMap = entry.getValue();
            log.info("Starting NetCDF for " + experiment.getAccession() +
                    " and " + entry.getKey() + " (" + assayMap.size() + " assays)");
            NetCDFData data = readNetCDF(netCDFLocation, assayMap);

            listener.setProgress("Writing updated NetCDF");
            writeNetCDF(atlasDAO, netCDFLocation, data, experiment, arrayDesign);

            if (data.isAnalyticsTransferred())
                listener.setRecomputeAnalytics(false);
            listener.setProgress("Successfully updated the NetCDF");
        }
    }

    private static NetCDFData readNetCDF(File source, Map<String, Assay> knownAssays) throws AtlasLoaderException {
        NetCDFProxy proxy = null;
        try {
            proxy = new NetCDFProxy(source);

            NetCDFData data = new NetCDFData();

            final List<Integer> usedAssays = new ArrayList<Integer>();
            final String[] assayAccessions = proxy.getAssayAccessions();
            for (int i = 0; i < assayAccessions.length; ++i) {
                Assay assay = knownAssays.get(assayAccessions[i]);
                if (assay != null) {
                    data.addAssay(assay);
                    usedAssays.add(i);
                }
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

            // TODO: 4alf: we cannot use experiment.getAssays() as we're bound by the ArrayDesign
            netCdfCreator.setAssays(data.getAssays());

            for (Assay assay : data.getAssays()) {
                for (Sample sample : assay.getSamples()) {
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

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }
}
