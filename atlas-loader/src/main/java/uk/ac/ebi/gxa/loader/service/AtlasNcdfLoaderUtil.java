package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class AtlasNcdfLoaderUtil {
    private final static Logger log = LoggerFactory.getLogger(AtlasNcdfLoaderUtil.class);

    public static void loadNcdfToCache(Model atlasModel, AtlasLoadCache cache, NetCDFProxy proxy) throws IOException {
        Experiment experiment = atlasModel.createExperiment(proxy.getExperimentAccession(), proxy.getExperimentId());

        experiment.setDescription(proxy.getExperimentDescription());
        experiment.setLab(proxy.getExperimentLab());
        experiment.setPerformer(proxy.getExperimentPerformer());
        experiment.setPubmedIdString(proxy.getExperimentPubmedID());
        experiment.setAbstract(proxy.getAbstract());

        cache.setExperiment(experiment);

        final int numOfAssays = proxy.getAssays().length;

        //original usage pattern assumes DataMatrixStorage width=Assays+UniqueEFVs(for PVAL)+UniqueEFVs(for TSTAT)
        //let me create data storages for each entity
        DataMatrixStorage storage = new DataMatrixStorage(numOfAssays, 1, 1);

        List<String> designElements = Arrays.asList(proxy.getDesignElementAccessions());

        for (int i = 0; i < designElements.size(); i++) {
            storage.add(designElements.get(i), proxy.getExpressionDataForDesignElementAtIndex(i));
        }

        final String arrayDesignAccession = proxy.getArrayDesignAccession();
        String[] assayAccessions = readAssayAccessions(proxy);

        for (int i = 0; i < proxy.getAssays().length; i++) {
            Assay assay = new Assay(assayAccessions[i]);
            assay.setExperimentAccession(experiment.getAccession());
            assay.setArrayDesignAccession(arrayDesignAccession);

            for (String factor : proxy.getFactors()) {
                String[] factorValueOntologies = proxy.getFactorValueOntologies(factor);
                String ontologies = factorValueOntologies.length != 0 ?
                        factorValueOntologies[i] : null;
                assay.addProperty(factor, proxy.getFactorValues(factor)[i], ontologies);
            }

            cache.setAssayDataMatrixRef(assay, storage, i);
            cache.setDesignElements(assay.getArrayDesignAccession(), designElements);

            cache.addAssay(assay);
        }

        int[][] sampleToAssayMatrix = proxy.getSamplesToAssays();
        for (int i = 0; i < proxy.getSamples().length; i++) {
            Sample sample = new Sample();
            sample.setAccession(proxy.getSampleAccessions()[i]);

            for (int j = 0; j < sampleToAssayMatrix[i].length; j++) {
                if (sampleToAssayMatrix[i][j] == 1) {
                    sample.addAssayAccession(assayAccessions[j]);
                }
            }

            for (String characteristic : proxy.getCharacteristics()) {
                String[] characteristicValueOntologies = proxy.getCharacteristicValueOntologies(characteristic);
                String efoTerms = characteristicValueOntologies.length != 0 ?
                        characteristicValueOntologies[i] : null;
                sample.addProperty(characteristic, proxy.getCharacteristicValues(characteristic)[i],
                        efoTerms);
            }

            cache.addSample(sample);
        }

        //load analytics to cache
        final List<String> uniqueFactorValues = proxy.getUniqueFactorValues();
        DataMatrixStorage pvalStorage = new DataMatrixStorage(uniqueFactorValues.size(), 1, 1);
        DataMatrixStorage tstatStorage = new DataMatrixStorage(uniqueFactorValues.size(), 1, 1);

        for (int i = 0; i < designElements.size(); i++) {
            pvalStorage.add(designElements.get(i), proxy.getPValuesForDesignElement(i));
            tstatStorage.add(designElements.get(i), proxy.getTStatisticsForDesignElement(i));
        }

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (int i = 0; i < uniqueFactorValues.size(); i++) {
            Pair<String, String> factorValue = parseFactorValuePair(uniqueFactorValues.get(i));
            pvalMap.put(factorValue, new DataMatrixStorage.ColumnRef(pvalStorage, i));
            tstatMap.put(factorValue, new DataMatrixStorage.ColumnRef(tstatStorage, i));
        }
        cache.setPvalDataMap(pvalMap);
        cache.setTstatDataMap(tstatMap);
    }

    private static String[] readAssayAccessions(NetCDFProxy proxy) throws IOException {
        final String[] assayAccessions = proxy.getAssayAccessions();
        String[] result = new String[proxy.getAssays().length];
        System.arraycopy(assayAccessions, 0, result, 0, proxy.getAssays().length);
        return result;
    }

    private static Pair<String, String> parseFactorValuePair(String uniqueFactorValue) {
        String[] parts = uniqueFactorValue.split("[||]");
        log.debug("The parsed parts are, {}", Arrays.asList(parts));
        switch (parts.length) {
            case 1:
                return Pair.create(parts[0], "");
            case 3:
                return Pair.create(parts[0], parts[2]);
            default:
                log.error("We expect 1 or 3 parts, not {} - got \\{{}\\}", parts.length, Arrays.toString(parts));
                throw new IllegalStateException("We expect 1 or 3 parts, not {} - got " + Arrays.toString(parts));
        }
    }
}
