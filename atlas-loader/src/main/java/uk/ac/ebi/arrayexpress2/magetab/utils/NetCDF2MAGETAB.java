package uk.ac.ebi.arrayexpress2.magetab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NetCDF2MAGETAB {
    private final static Logger log = LoggerFactory.getLogger(NetCDF2MAGETAB.class);

    public static void loadNcdfToCache(AtlasLoadCache cache, NetCDFProxy proxy) throws IOException {
        Experiment experiment = new Experiment();

        String experimentAccession = proxy.getExperiment();

        experiment.setAccession(experimentAccession);
        experiment.setDescription(proxy.getExperimentDescription());
        experiment.setLab(proxy.getExperimentLab());
        experiment.setPerformer(proxy.getExperimentPerformer());
        experiment.setPubmedID(proxy.getExperimentPubmedID());
        experiment.setArticleAbstract(proxy.getArticleAbstract());

        cache.setExperiment(experiment);

        final int numOfAssays = proxy.getAssays().length;

        //original usage pattern assumes DataMatrixStorage width=Assays+UniqueEFVs(for PVAL)+UniqueEFVs(for TSTAT)
        //let me create data storages for each entity
        DataMatrixStorage storage = new DataMatrixStorage(numOfAssays, 1, 1);

        List<String> designElements = Arrays.asList(proxy.getDesignElementAccessions());

        for (int i = 0; i < designElements.size(); i++) {
            storage.add(designElements.get(i), proxy.getExpressionDataForDesignElementAtIndex(i));
        }

        String[] assayAccessions = new String[proxy.getAssays().length];
        for (int i = 0; i < proxy.getAssays().length; i++) {
            Assay assay = new Assay();
            assay.setAccession(proxy.getAssayAccessions()[i]);
            assay.setExperimentAccession(experimentAccession);
            assay.setArrayDesignAccession(proxy.getArrayDesignAccession());

            for (String factor : proxy.getFactors()) {
                String[] factorValueOntologies = proxy.getFactorValueOntologies(factor);
                String ontologies = factorValueOntologies.length != 0 ?
                        factorValueOntologies[i] : null;
                assay.addProperty(factor, proxy.getFactorValues(factor)[i], true, ontologies);
            }

            cache.setAssayDataMatrixRef(assay, storage, i);
            cache.setDesignElements(assay.getArrayDesignAccession(), designElements);

            cache.addAssay(assay);

            assayAccessions[i] = assay.getAccession();
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
                        false, efoTerms);
            }

            cache.addSample(sample);
        }

        //load analytics to cache
        final String[] uniqueFactorValues = proxy.getUniqueFactorValues();
        DataMatrixStorage pvalStorage = new DataMatrixStorage(uniqueFactorValues.length, 1, 1);
        DataMatrixStorage tstatStorage = new DataMatrixStorage(uniqueFactorValues.length, 1, 1);

        for (int i = 0; i < designElements.size(); i++) {
            pvalStorage.add(designElements.get(i), proxy.getPValuesForDesignElement(i));
            tstatStorage.add(designElements.get(i), proxy.getTStatisticsForDesignElement(i));
        }

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (int i = 0; i < uniqueFactorValues.length; i++) {
            Pair<String, String> factorValue = parseFactorValuePair(uniqueFactorValues[i]);
            pvalMap.put(factorValue, new DataMatrixStorage.ColumnRef(pvalStorage, i));
            tstatMap.put(factorValue, new DataMatrixStorage.ColumnRef(tstatStorage, i));
        }
        cache.setPvalDataMap(pvalMap);
        cache.setTstatDataMap(tstatMap);
    }

    private static Pair<String, String> parseFactorValuePair(String uniqueFactorValue) {
        String[] pair = uniqueFactorValue.split("[||]");
        log.debug("The parsed pair is {}", Arrays.asList(pair));
        if (pair.length == 2) {
            log.error("pair[1] should always be \"\", found \"{}\"", pair[1]);
            throw new IllegalStateException("pair[1] should always be \"\"");
        }
        return Pair.create(pair[0], pair.length > 2 ? pair[2] : "");
    }
}
