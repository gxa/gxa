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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class NetCDF2MAGETAB {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void loadFileToCache(File file, AtlasLoadCache cache) throws IOException {
        NetCDFProxy proxy = new NetCDFProxy(file);

        Experiment experiment = new Experiment();

        String experimentAccession = proxy.getExperiment();

        experiment.setAccession(experimentAccession);
        experiment.setDescription(proxy.getExperimentDescription());
        experiment.setLab(proxy.getExperimentLab());
        experiment.setPerformer(proxy.getExperimentPerformer());
        experiment.setPubmedID(proxy.getExperimentPubmedID());
        experiment.setArticleAbstract(proxy.getArticleAbstract());

        cache.setExperiment(experiment);

        int numOfAssays = proxy.getAssays().length;

        String[] designElementAccessions = proxy.getDesignElementAccessions();
        //original usage pattern assumes DataMatrixStorage width=Assays+UniueEFVs(for PVAL)+UniqueEFVs(for TSTAT)
        //let me create data storages for each entity
        DataMatrixStorage storage = new DataMatrixStorage(numOfAssays, 1, 1);

        for (int iDesignElement = 0; iDesignElement != designElementAccessions.length; iDesignElement++) {
            final float[] expressionData = proxy.getExpressionDataForDesignElementAtIndex(iDesignElement);
            storage.add(designElementAccessions[iDesignElement], new Iterator<Float>() {
                private int iCurrent = 0;

                public void remove() throws IllegalStateException {
                    throw new IllegalStateException("not implemented");
                }

                public Float next() {
                    return expressionData[iCurrent++];
                }

                public boolean hasNext() {
                    return iCurrent < expressionData.length;
                }
            });
        }

        String[] assayAccessions = new String[proxy.getAssays().length];
        int iAssay = 0;
        for (long assayID : proxy.getAssays()) {
            Assay assay = new Assay();
            assay.setAccession(proxy.getAssayAccessions()[iAssay]);
            assay.setExperimentAccession(experimentAccession);
            assay.setArrayDesignAccession(proxy.getArrayDesignAccession());

            for (String factor : proxy.getFactors()) {
                String[] factorValueOntologies = proxy.getFactorValueOntologies(factor);
                String ontologies = factorValueOntologies.length > iAssay ?
                        factorValueOntologies[iAssay] : null;
                assay.addProperty(factor, factor, proxy.getFactorValues(factor)[iAssay], true, ontologies);
            }

            cache.setAssayDataMatrixRef(assay, storage, iAssay);
            cache.setDesignElements(assay.getArrayDesignAccession(), Arrays.asList(designElementAccessions));

            cache.addAssay(assay);

            assayAccessions[iAssay] = assay.getAccession();
            iAssay++;
        }

        int[][] sampleToAssayMatrix = proxy.getSamplesToAssays();
        int iSample = 0;
        for (long sampleID : proxy.getSamples()) {
            Sample sample = new Sample();
            sample.setAccession(proxy.getSampleAccessions()[iSample]);

            int iAssay2 = 0;
            for (int isLinked : sampleToAssayMatrix[iSample]) {
                if (1 == isLinked) {
                    sample.addAssayAccession(assayAccessions[iAssay2]);
                }
                iAssay2++;
            }

            for (String factor : proxy.getCharacteristics()) {
                String value = proxy.getCharacteristicValues(factor)[iSample];
                String[] characteristicValueOntologies = proxy.getCharacteristicValueOntologies(factor);
                String efoTerms = characteristicValueOntologies.length > iSample ?
                        characteristicValueOntologies[iSample] : null;
                sample.addProperty(factor, factor, value, false, efoTerms);
            }

            cache.addSample(sample);
            iSample++;
        }

        //load analytics to cache
        int numOfPVs = proxy.getUniqueFactorValues().length;
        DataMatrixStorage pvalStorage = new DataMatrixStorage(numOfPVs, 1, 1);
        DataMatrixStorage tstatStorage = new DataMatrixStorage(numOfPVs, 1, 1);

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();

        int iDesignElementIndex = 0;
        for (String designElement : proxy.getDesignElementAccessions()) {
            final float[] pvalData = proxy.getPValuesForDesignElement(iDesignElementIndex);
            final float[] tstatData = proxy.getTStatisticsForDesignElement(iDesignElementIndex);
            pvalStorage.add(designElement, new Iterator<Float>() { //factorize me!
                private int iCurrent = 0;

                public void remove() throws IllegalStateException {
                    throw new IllegalStateException("not implemented");
                }

                public Float next() {
                    return pvalData[iCurrent++];
                }

                public boolean hasNext() {
                    return iCurrent < pvalData.length;
                }
            });
            tstatStorage.add(designElement, new Iterator<Float>() { //factorize me!
                private int iCurrent = 0;

                public void remove() throws IllegalStateException {
                    throw new IllegalStateException("not implemented");
                }

                public Float next() {
                    return tstatData[iCurrent++];
                }

                public boolean hasNext() {
                    return iCurrent < tstatData.length;
                }
            });
            iDesignElementIndex++;
        }

        for (int iUniquePV = 0; iUniquePV != numOfPVs; iUniquePV++) {
            String[] uniqueFactorValue = proxy.getUniqueFactorValues()[iUniquePV].split("[||]");

            String uP = uniqueFactorValue[0];
            String uPV = (uniqueFactorValue.length > 1) ? uniqueFactorValue[2] : ""; //[1]-empty string

            pvalMap.put(Pair.create(uP, uPV), new DataMatrixStorage.ColumnRef(pvalStorage, iUniquePV));
            tstatMap.put(Pair.create(uP, uPV), new DataMatrixStorage.ColumnRef(tstatStorage, iUniquePV));
        }

        cache.setPvalDataMap(pvalMap);
        cache.setTstatDataMap(tstatMap);
    }
}
