package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

abstract class AtlasNcdfLoaderUtil {
    public static void loadNcdfToCache(Model atlasModel, AtlasLoadCache cache, NetCDFProxy proxy) throws IOException {
        // TODO: 4alf: it is generally a bad idea to get DB id from an external resource. If the NetCDF is not from
        // TODO: 4alf: the same application (e.g. different data release), we may as well screw the DB
        Experiment experiment = atlasModel.createExperiment(proxy.getExperimentId(), proxy.getExperimentAccession());

        experiment.setDescription(proxy.getExperimentDescription());
        experiment.setLab(proxy.getExperimentLab());
        experiment.setPerformer(proxy.getExperimentPerformer());
        experiment.setPubmedIdString(proxy.getExperimentPubmedID());
        experiment.setAbstract(proxy.getAbstract());

        cache.setExperiment(experiment);

        final String[] assayAccessions = proxy.getAssayAccessions();

        //original usage pattern assumes DataMatrixStorage width=Assays+UniqueEFVs(for PVAL)+UniqueEFVs(for TSTAT)
        //let me create data storages for each entity
        DataMatrixStorage storage = new DataMatrixStorage(assayAccessions.length, 1, 1);

        List<String> designElements = Arrays.asList(proxy.getDesignElementAccessions());

        for (int i = 0; i < designElements.size(); i++) {
            storage.add(designElements.get(i), proxy.getExpressionDataForDesignElementAtIndex(i));
        }

        final ArrayDesign arrayDesign = new ArrayDesign(proxy.getArrayDesignAccession());

        for (int i = 0; i < assayAccessions.length; i++) {
            Assay assay = new Assay(assayAccessions[i]);
            assay.setExperiment(experiment);
            assay.setArrayDesign(arrayDesign);

            for (String factor : proxy.getFactors()) {
                String[] factorValueOntologies = proxy.getFactorValueOntologies(factor);
                String ontologies = factorValueOntologies.length != 0 ?
                        factorValueOntologies[i] : null;
                assay.addProperty(factor, proxy.getFactorValues(factor)[i], ontologies);
            }

            cache.setAssayDataMatrixRef(assay, storage, i);
            cache.setDesignElements(assay.getArrayDesign().getAccession(), designElements);

            cache.addAssay(assay);
        }

        int[][] sampleToAssayMatrix = proxy.getSamplesToAssays();
        for (int i = 0; i < proxy.getSampleAccessions().length; i++) {
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
}
