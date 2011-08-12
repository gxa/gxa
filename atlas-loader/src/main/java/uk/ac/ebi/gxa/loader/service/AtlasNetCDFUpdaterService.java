package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

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
    private AtlasDataDAO atlasDataDAO;

    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        atlasDAO.startSession();
        try {
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
                final ArrayDesign arrayDesign = atlasDAO.getArrayDesignByAccession(entry.getKey());

                final NetCDFDescriptor descriptor = atlasDataDAO.getNetCDFDescriptor(experiment, arrayDesign);
                listener.setProgress("Reading existing NetCDF");

                final Map<String, Assay> assayMap = entry.getValue();
                log.info("Starting NetCDF for " + experiment.getAccession() +
                        " and " + entry.getKey() + " (" + assayMap.size() + " assays)");
                NetCDFData data = readNetCDF(atlasDAO, descriptor, assayMap);

                listener.setProgress("Writing updated NetCDF");
                writeNetCDF(data, experiment, arrayDesign);

                if (data.isAnalyticsTransferred())
                    listener.setRecomputeAnalytics(false);
                listener.setProgress("Successfully updated the NetCDF");
            }
        } finally {
            atlasDAO.finishSession();
        }
    }

    private static NetCDFData readNetCDF(AtlasDAO dao, NetCDFDescriptor descriptor, Map<String, Assay> knownAssays) throws AtlasLoaderException {
        NetCDFProxy proxy = null;
        try {
            proxy = descriptor.createProxy();

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

            // TODO: this is commented out because it is *broken* and needs to be rewritten
            // behaviour after commenting code below: *any* netcdf update will result in analytics reset
            // TODO: the getValuePatterns(proxy, data.getAssays()) code, would you need it,
            // see rev. 48f0df44ce1fbaea42dff50167827d0138bd4eb1 for an attempt to fix it
            // and rev. 05be531ebb5a93df06d6045f982d0b25e4008a11 for nearly-original version

//            if (assayAccessions.length == data.getAssays().size()) {
//                data.matchValuePatterns(getValuePatterns(proxy, data.getAssays()));
//            }

            // Get unique values
            List<KeyValuePair> uniqueValues = proxy.getUniqueValues();
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
        } catch (AtlasDataException e) {
            log.error("Error reading NetCDF file: " + descriptor.getFileName(), e);
            throw new AtlasLoaderException(e);
        } catch (IOException e) {
            log.error("Error reading NetCDF file: " + descriptor.getFileName(), e);
            throw new AtlasLoaderException(e);
        } finally {
            closeQuietly(proxy);
        }
    }

    private void writeNetCDF(NetCDFData data, Experiment experiment, ArrayDesign arrayDesign) throws AtlasLoaderException {
        try {
            final NetCDFCreator netCdfCreator = atlasDataDAO.getNetCDFCreator(experiment, arrayDesign);

            netCdfCreator.setAssays(experiment.getAssaysForDesign(arrayDesign));

            for (Assay assay : experiment.getAssaysForDesign(arrayDesign)) {
                for (Sample sample : assay.getSamples()) {
                    netCdfCreator.setSample(assay, sample);
                }
            }

            netCdfCreator.setAssayDataMap(data.getAssayDataMap());
            netCdfCreator.setPvalDataMap(data.getPValDataMap());
            netCdfCreator.setTstatDataMap(data.getTStatDataMap());

            netCdfCreator.createNetCdf();

            log.info("Successfully finished NetCDF for " + experiment.getAccession() + " and " + arrayDesign.getAccession());
        } catch (NetCDFCreatorException e) {
            log.error("Error writing NetCDF file for " + experiment.getAccession() + " and " + arrayDesign.getAccession(), e);
            throw new AtlasLoaderException(e);
        }
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }
}
