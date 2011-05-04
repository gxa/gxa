package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


class AtlasNcdfLoader {

    public static void loadNcdfToCache(AtlasLoadCache cache, NetCDFProxy proxy) throws IOException {
        Experiment experiment = new Experiment();

        experiment.setAccession(proxy.getExperiment());
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

        final String arrayDesignAccession = proxy.getArrayDesignAccession();
        String[] assayAccessions = readAssayAccessions(proxy);

        for (int i = 0; i < proxy.getAssays().length; i++) {
            Assay assay = new Assay();
            assay.setAccession(assayAccessions[i]);
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
    }

    private static String[] readAssayAccessions(NetCDFProxy proxy) throws IOException {
        final String[] assayAccessions = proxy.getAssayAccessions();
        String[] result = new String[proxy.getAssays().length];
        System.arraycopy(assayAccessions, 0, result, 0, proxy.getAssays().length);
        return result;
    }
}
