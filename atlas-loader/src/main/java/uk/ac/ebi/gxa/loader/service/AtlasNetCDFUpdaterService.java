package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.CBitSet;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.primitives.Floats.asList;
import static uk.ac.ebi.gxa.utils.CollectionUtil.distinct;
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

                listener.setProgress("Reading existing NetCDF");

                final Map<String, Assay> assayMap = entry.getValue();
                log.info("Starting NetCDF for " + experiment.getAccession() +
                        " and " + entry.getKey() + " (" + assayMap.size() + " assays)");
                NetCDFData data = readNetCDF(experiment, arrayDesign, assayMap);

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

    private NetCDFData readNetCDF(Experiment experiment, ArrayDesign arrayDesign, Map<String, Assay> knownAssays) throws AtlasLoaderException {
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {
            final NetCDFData data = new NetCDFData();

            final List<Integer> usedAssays = new ArrayList<Integer>();
            // WARNING! do not change to ewd.getAssays(arrayDesign) because this method expects assays to come in
            // NetCDF-storage order.
            final String[] assayAccessions = ewd.getProxy(arrayDesign).getAssayAccessions();
            for (int i = 0; i < assayAccessions.length; ++i) {
                final Assay assay = knownAssays.get(assayAccessions[i]);
                if (assay != null) {
                    data.addAssay(assay);
                    usedAssays.add(i);
                }
            }

            if (assayAccessions.length == data.getAssays().size()) {
                data.matchValuePatterns(getValuePatterns(ewd, arrayDesign));
            }

            // Get unique values
            final List<KeyValuePair> uniqueValues = ewd.getUniqueValues(arrayDesign);
            data.setUniqueValues(uniqueValues);

            final String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
            data.setStorage(new DataMatrixStorage(data.getWidth(), deAccessions.length, 1));
            for (int i = 0; i < deAccessions.length; ++i) {
                final float[] values = ewd.getExpressionDataForDesignElementAtIndex(arrayDesign, i);
                final float[] pval = ewd.getPValuesForDesignElement(arrayDesign, i);
                final float[] tstat = ewd.getTStatisticsForDesignElement(arrayDesign, i);
                // Make sure that pval/tstat arrays are big enough if uniqueValues size is greater than ewd.getUniqueFactorValues()
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
            log.error("Error reading NetCDF file for: " + experiment.getAccession() + "/" + arrayDesign.getAccession(), e);
            throw new AtlasLoaderException(e);
        } catch (IOException e) {
            log.error("Error reading NetCDF file for: " + experiment.getAccession() + "/" + arrayDesign.getAccession(), e);
            throw new AtlasLoaderException(e);
        } finally {
            ewd.closeAllDataSources();
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
        } catch (AtlasDataException e) {
            log.error("Error writing NetCDF file for " + experiment.getAccession() + " and " + arrayDesign.getAccession(), e);
            throw new AtlasLoaderException(e);
        }
    }

    private static EfvTree<CBitSet> getValuePatterns(ExperimentWithData ewd, ArrayDesign ad) throws AtlasDataException {
        final EfvTree<CBitSet> patterns = new EfvTree<CBitSet>();

        // Store ef-efv patterns
        final List<String> efs = Arrays.asList(ewd.getFactors(ad));
        for (String ef : efs) {
            final List<String> efvs = Arrays.asList(ewd.getFactorValues(ad, ef));
            final Set<String> distinctEfvs = distinct(efvs);
            for (String value : distinctEfvs) {
                final CBitSet pattern = new CBitSet(efvs.size());
                for (int i = 0; i < efvs.size(); i++) {
                    pattern.set(i, efvs.get(i).equals(value));
                }
                patterns.putCaseSensitive(ef, value, pattern);
            }
        }

        // Store sc-scv patterns
        final List<String> scs = new ArrayList<String>(Arrays.asList(ewd.getCharacteristics(ad)));
        scs.removeAll(efs); // process only scs that aren't also efs
        for (String sc : scs) {
            final List<String> scvs = Arrays.asList(ewd.getCharacteristicValues(ad, sc));
            final Set<String> distinctScvs = distinct(scvs);
            for (String value : distinctScvs) {
                final CBitSet pattern = new CBitSet(scvs.size());
                for (int i = 0; i < scvs.size(); i++) {
                    pattern.set(i, scvs.get(i).equals(value));
                }
                patterns.putCaseSensitive(sc, value, pattern);
            }
        }

        return patterns;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

}
