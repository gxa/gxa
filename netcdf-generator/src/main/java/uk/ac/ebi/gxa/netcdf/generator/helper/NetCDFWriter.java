package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class NetCDFWriter {
    // internal maps - indexes for locations of given assay/design element ids
    private Map<Integer, Integer> assayIndex = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> designElementIndex = new HashMap<Integer, Integer>();

    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void writeNetCDF(NetcdfFileWriteable netCDF, DataSlice dataSlice)
            throws NetCDFGeneratorException {
        try {
            // write assay data
            writeAssayData(
                    netCDF,
                    dataSlice.getAssays());

            // write sample data
            writeSampleData(
                    netCDF,
                    dataSlice.getSamples());

            // write assay/sample mapping data
            writeAssayToSampleData(
                    netCDF,
                    dataSlice.getSamples(),
                    dataSlice.getAssays(),
                    dataSlice.getSampleMappings());

            // write design element data
            writeDesignElementData(
                    netCDF,
                    dataSlice.getDesignElements());

            // write gene data
            writeGeneData(
                    netCDF,
                    dataSlice.getGenes());

            // write design element/gene mapping data
            writeDesignElementToGeneData(
                    netCDF,
                    dataSlice.getDesignElements(),
                    dataSlice.getGenes(),
                    dataSlice.getGeneMappings());

            // write property data
            writePropertyData(
                    netCDF,
                    dataSlice.getExperimentFactorMappings(),
                    dataSlice.getSampleCharacteristicMappings());

            // write expression matrix values
            writeExpressionMatrixValues(
                    netCDF,
                    dataSlice.getExpressionValues());

            // write stats matrix values
            writeStatsValues(
                    netCDF,
                    dataSlice.getDesignElements(),
                    dataSlice.getExpressionAnalysisMappings(),
                    dataSlice.getExperimentFactorMappings());
        }
        catch (IOException e) {
            throw new NetCDFGeneratorException(e);
        }
        catch (InvalidRangeException e) {
            throw new NetCDFGeneratorException(e);
        }
    }

    private void writeAssayData(NetcdfFileWriteable netCDF, List<Assay> assays)
            throws IOException, InvalidRangeException {
        // check dimension is available
        if (netCDF.findDimension("AS") != null) {
            // add assay id data
            ArrayInt as = new ArrayInt.D1(assays.size());
            IndexIterator asIt = as.getIndexIterator();
            int counter = 0;
            for (Assay assay : assays) {
                asIt.setIntNext(assay.getAssayID());

                // record in the index
                assayIndex.put(assay.getAssayID(), counter);
                counter++;
            }
            netCDF.write("AS", as);
        }
        log.debug("Wrote assay data matrix ok.");
    }

    private void writeSampleData(NetcdfFileWriteable netCDF, List<Sample> samples)
            throws IOException, InvalidRangeException {
        if (netCDF.findDimension("BS") != null) {
            // add sample id data
            ArrayInt bs = new ArrayInt.D1(samples.size());
            IndexIterator bsIt = bs.getIndexIterator();
            for (Sample sample : samples) {
                bsIt.setIntNext(sample.getSampleID());
            }
            netCDF.write("BS", bs);
        }
        log.debug("Wrote sample data matrix ok.");
    }

    private void writeAssayToSampleData(NetcdfFileWriteable netCDF,
                                        List<Sample> samples,
                                        List<Assay> assays,
                                        Map<Assay, List<Sample>> assayToSamples)
            throws NetCDFGeneratorException, IOException, InvalidRangeException {
        if (netCDF.findDimension("AS") != null && netCDF.findDimension("BS") != null) {
            // index counters
            int assayIndex = 0;
            int sampleIndex = 0;
            ArrayInt bs2as = new ArrayInt.D2(samples.size(),
                                             assays.size());
            // iterate over assays and samples,
            // and work out which spots in the matrix to set to 1
            for (Assay assay : assays) {
                for (Sample sample : samples) {
                    // insert value
                    if (assayToSamples.get(assay).contains(sample)) {
                        bs2as.setInt(bs2as.getIndex().set(sampleIndex, assayIndex), 1);
                    }
                    else {
                        bs2as.setInt(bs2as.getIndex().set(sampleIndex, assayIndex), 0);
                    }

                    // increment sample index by one
                    sampleIndex++;
                }

                // increment assayIndex up one and reset sampleIndex
                assayIndex++;
                sampleIndex = 0;
            }

            // finally, write
            netCDF.write("BS2AS", bs2as);
        }
        log.debug("Wrote assay2sample data matrix ok.");
    }

    private void writeDesignElementData(NetcdfFileWriteable netCDF,
                                        Map<Integer, String> designElements)
            throws IOException, InvalidRangeException {
        if (netCDF.findDimension("DE") != null) {
            // add design element id data
            ArrayInt de = new ArrayInt.D1(designElements.keySet().size());
            IndexIterator deIt = de.getIndexIterator();
            int counter = 0;
            for (int designElementID : designElements.keySet()) {
                deIt.setIntNext(designElementID);

                // record in the index
                designElementIndex.put(designElementID, counter);
                counter++;
            }
            netCDF.write("DE", de);
        }
        log.debug("Wrote design element data matrix ok.");
    }

    private void writeGeneData(NetcdfFileWriteable netCDF,
                               List<Gene> genes)
            throws IOException, InvalidRangeException, NetCDFGeneratorException {
        if (netCDF.findDimension("GN") != null) {
            // add sample id data
            ArrayInt gn = new ArrayInt.D1(genes.size());
            IndexIterator gnIt = gn.getIndexIterator();
            for (Gene gene : genes) {
                gnIt.setIntNext(gene.getGeneID());
            }
            netCDF.write("GN", gn);
        }
        log.debug("Wrote gene data matrix ok.");
    }

    private void writeDesignElementToGeneData(NetcdfFileWriteable netCDF, Map<Integer, String> designElements,
                                              List<Gene> genes, Map<Integer, List<Gene>> designElementsToGenes)
            throws IOException, InvalidRangeException {
        if (netCDF.findDimension("DE") != null && netCDF.findDimension("GN") != null) {
            // index counters
            int deIndex;
            int gnIndex = 0;

            // map of deIndex to gnIndex
            List<int[]> mappingList = new ArrayList<int[]>();

            // iterate over design elements and genes,
            // and work out which spots in the matrix to set to 1
            for (Integer designElement : designElements.keySet()) {
                for (Gene gene : genes) {
                    // insert value
                    if (designElementsToGenes.get(designElement).contains(gene)) {
                        // got a mapping between design element id='designElement' and gene, lookup position in DE
                        deIndex = designElementIndex.get(designElement);

                        // create a one to one map
                        int[] map = new int[]{deIndex, gnIndex};
                        mappingList.add(map);
                    }

                    // increment gene index by one
                    gnIndex++;
                }

                // reset gene index (next row)
                gnIndex = 0;
            }

            // now we have the list of pairs, write them
            ArrayInt de2gn = new ArrayInt.D2(mappingList.size(), 2);
            for (int i = 0; i < mappingList.size(); i++) {
                de2gn.setInt(de2gn.getIndex().set(i, 0), mappingList.get(i)[0]);
                de2gn.setInt(de2gn.getIndex().set(i, 1), mappingList.get(i)[1]);
            }
            netCDF.write("DE2GN", de2gn);
        }
        log.debug("Wrote designElement2gene data matrix ok.");
    }

    private void writePropertyData(
            NetcdfFileWriteable netCDF,
            Map<String, List<String>> experimentFactorMap,
            Map<String, List<String>> sampleCharacteristicMap)
            throws IOException, InvalidRangeException {
        // check dimensions for assay properties are available
        if (netCDF.findDimension("AS") != null &&
                netCDF.findDimension("EF") != null &&
                netCDF.findDimension("EFlen") != null &&
                netCDF.findDimension("uEFV") != null) {
            // write assay property values
            ArrayChar ef = new ArrayChar.D2(
                    netCDF.findDimension("EF").getLength(),
                    netCDF.findDimension("EFlen").getLength());
            ArrayChar efv = new ArrayChar.D3(
                    netCDF.findDimension("EF").getLength(),
                    netCDF.findDimension("AS").getLength(),
                    netCDF.findDimension("EFlen").getLength());
            ArrayChar uefv = new ArrayChar.D2(
                    netCDF.findDimension("uEFV").getLength(),
                    netCDF.findDimension("EFlen").getLength());
            ArrayInt uefvNum = new ArrayInt.D1(
                    netCDF.findDimension("EF").getLength());

            // track total number of assays
            int assayCount = netCDF.findDimension("AS").getLength();

            // populate ef, efv matrices
            int efIndex = 0;
            int efvIndex = 0;

            // build set of uEFVs
            Set<String> uniqueFactorValues = new LinkedHashSet<String>();

            // loop over factor/factor value mappings
            for (String propertyName : experimentFactorMap.keySet()) {
                // add property name to EF
                ef.setString(efIndex, propertyName);

                // check that number of factor values is divisible by the number of assays
                if (experimentFactorMap.get(propertyName).size() % assayCount != 0) {
                    throw new InvalidRangeException("Cannot reconcile property values for " + propertyName +
                            ": expected a multiple of the number of assays (" + assayCount + "), got " +
                            experimentFactorMap.get(propertyName).size());
                }

                // may be multiple EFVs per assay - if so, we need to concatenate EFVs with a comma
                int repeats = experimentFactorMap.get(propertyName).size() / assayCount;
                int tracker = 0;
                String currentEFV = "";

                if (repeats > 1) {
                    // concatenate multiples with commas
                    // this will occur if there are multiple property values assigned
                    // to the same property for single assay
                    String stats = new StringBuffer()
                            .append("\n\tNumber of Assays: ").append(assayCount)
                            .append("\n\tNumber of Experiment Factor Values (for ").append(propertyName)
                            .append("): ").append(experimentFactorMap.get(propertyName).size()).append(".")
                            .append("\n\t").append(repeats).append(" EFVs at a time will be stored")
                            .toString();
                    log.warn("Multiple property values assigned to the same " +
                            "property (" + propertyName + ") for single assay!" + stats);
                }

                for (String propertyValue : experimentFactorMap.get(propertyName)) {
                    // concatenate strings with commas if we need more
                    currentEFV = currentEFV.concat(propertyValue);
                    if (tracker++ % repeats != 0) {
                        currentEFV = currentEFV.concat("||");
                    }
                    else {
                        // got to the end of the current set of repeats - so add data to EF, EFV matrices
                        // add property value to EFV, indexed by each ef
                        efv.setString(efv.getIndex().set(efIndex, efvIndex), currentEFV);
                        // increment index count on efv axis
                        efvIndex++;

                        // and populate uniqueFactorValues set
                        uniqueFactorValues.add(propertyName.concat("||").concat(propertyValue));

                        // reset tracker and currentEFV
                        tracker=0;
                        currentEFV = "";
                    }
                }

                // increment ef axis up one, and reset efv axis to zero
                efIndex++;
                efvIndex = 0;
            }

            // populate uefv, uefvnum matrices
            int uefvIndex = 0;
            int uefvNumIndex = 0;

            // use unique factor values to populate uefv
            for (String uniqueFactorValue : uniqueFactorValues) {
                uefv.setString(uefv.getIndex().set(uefvIndex), uniqueFactorValue);
                // increment uefv up one
                uefvIndex++;
            }

            // use counts to populate uefvNum
            for (String propertyName : experimentFactorMap.keySet()) {
                uefvNum.setInt(uefvNum.getIndex().set(uefvNumIndex), experimentFactorMap.get(propertyName).size());
                // increment uefvNum up one, indexed by assay
                uefvNumIndex++;
            }

            netCDF.write("EF", ef);
            netCDF.write("EFV", efv);
            netCDF.write("uEFV", uefv);
            netCDF.write("uEFVnum", uefvNum);
        }

        // check dimensions for sample properties are available
        if (netCDF.findDimension("SC") != null &&
                netCDF.findDimension("BS") != null) {
            // write sample data
            ArrayChar sc = new ArrayChar.D2(
                    netCDF.findDimension("SC").getLength(),
                    netCDF.findDimension("SClen").getLength());
            ArrayChar scv = new ArrayChar.D3(
                    netCDF.findDimension("SC").getLength(),
                    netCDF.findDimension("BS").getLength(),
                    netCDF.findDimension("SClen").getLength());

            // track total number of samples
            int sampleCount = netCDF.findDimension("BS").getLength();

            // used to populate sc/scv variables
            int scIndex = 0;
            int scvIndex = 0;

            // loop over characteristic/characteristic value mappings
            for (String propertyName : sampleCharacteristicMap.keySet()) {
                // add property name to SC
                sc.setString(scIndex, propertyName);

                // check that number of factor values is divisible by the number of assays
                if (sampleCharacteristicMap.get(propertyName).size() % sampleCount != 0) {
                    throw new InvalidRangeException("Cannot reconcile property values for " + propertyName +
                            ": expected a multiple of the number of samples (" + sampleCount + "), got " +
                            sampleCharacteristicMap.get(propertyName).size());
                }

                // may be multiple SCVs per sample - if so, we need to concatenate SCVs with a comma
                int repeats = sampleCharacteristicMap.get(propertyName).size() / sampleCount;
                int tracker = 0;
                String currentSCV = "";

                if (repeats > 1) {
                    // concatenate multiples with commas
                    // this will occur if there are multiple property values assigned
                    // to the same property for single assay
                    String stats = new StringBuffer()
                            .append("\n\tNumber of Samples: ")
                            .append(netCDF.findDimension("BS").getLength())
                            .append("\n\tNumber of Sample Characteristic Values (for ").append(propertyName)
                            .append("): ").append(sampleCharacteristicMap.get(propertyName).size()).append(".")
                            .append("\n\t").append(repeats).append(" SCVs at a time will be stored")
                            .toString();
                    log.warn("Multiple property values assigned to the same " +
                            "property (" + propertyName + ") for single sample!" + stats);
                }

                for (String propertyValue : sampleCharacteristicMap.get(propertyName)) {
                    // concatenate strings with commas if we need more
                    tracker++;
                    currentSCV = currentSCV.concat(propertyValue);
                    if (tracker % repeats != 0) {
                        currentSCV = currentSCV.concat("||");
                    }
                    else {
                        // got to the end of the current set of repeats - so add data to EF, EFV matrices
                        // add property value to EFV, indexed by each ef
                        scv.setString(scv.getIndex().set(scIndex, scvIndex), currentSCV);
                        // increment index count on efv axis
                        scvIndex++;

                        // reset tracker and currentEFV
                        tracker=0;
                        currentSCV = "";
                    }
                }

                // increment ef axis up one, and reset efv axis to zero
                scIndex++;
                scvIndex = 0;
            }

            netCDF.write("SC", sc);
            netCDF.write("SCV", scv);
        }
        log.debug("Wrote properties data matrices ok.");
    }

    private void writeExpressionMatrixValues(
            NetcdfFileWriteable netCDF,
            Map<Integer, Map<Integer, Float>> expressionValues)
            throws IOException, InvalidRangeException {
        if (netCDF.findDimension("AS") != null &&
                netCDF.findDimension("DE") != null) {
            ArrayDouble bdc = new ArrayDouble.D2(
                    netCDF.findDimension("DE").getLength(),
                    netCDF.findDimension("AS").getLength());

            // initialise everything to -1000000, default value
            IndexIterator bdcIt = bdc.getIndexIterator();
            while (bdcIt.hasNext()) {
                bdcIt.setDoubleNext(-1000000);
            }

            // loop over expression values doing crafty index lookups
            for (int assayID : expressionValues.keySet()) {
                for (Map.Entry<Integer, Float> ev : expressionValues.get(assayID)
                        .entrySet()) {
                    int asIndex = assayIndex.get(assayID);
                    int deIndex = designElementIndex.get(ev.getKey());

                    bdc.setDouble(bdc.getIndex().set(deIndex, asIndex), ev.getValue());
                }
            }

            netCDF.write("BDC", bdc);
        }
        log.debug("Wrote expression data matrix ok.");
    }

    private void writeStatsValues(NetcdfFileWriteable netCDF,
                                  Map<Integer, String> designElements,
                                  Map<Integer, List<ExpressionAnalysis>> analyses,
                                  Map<String, List<String>> experimentFactorMap)
            throws IOException, InvalidRangeException {
        if (netCDF.findDimension("DE") != null &&
                netCDF.findDimension("uEFV") != null) {
            int deMaxLength = netCDF.findDimension("DE").getLength();
            int uefvMaxLength = netCDF.findDimension("uEFV").getLength();
            ArrayDouble pval = new ArrayDouble.D2(
                    deMaxLength, uefvMaxLength);
            ArrayDouble tstat = new ArrayDouble.D2(
                    deMaxLength, uefvMaxLength);

            // keep track of all design elements with missing expression values
            Set<Integer> unmappedDesignElements = new HashSet<Integer>();
            Set<String> unmappedProperties = new HashSet<String>();

            Set<String> uniqueFactorValues = new LinkedHashSet<String>();
            for (String property : experimentFactorMap.keySet()) {
                for (String propertyValue : experimentFactorMap.get(property)) {
                    uniqueFactorValues.add(property.concat("||").concat(propertyValue));
                }
            }

            // loop over design elements, do positional lookups as we go
            int uefvIndex;
            for (int designElementID : designElements.keySet()) {
                if (analyses.get(designElementID) != null) {
                    // not null, so now loop over values of the assayFactorValueMap,
                    // so  we can lookup analysis by design element id and property value
                    uefvIndex = 0;
                    for (String uniqueFactorValue : uniqueFactorValues) {
                        uefvIndex++;
                        boolean foundAnalysis = false;
                        for (ExpressionAnalysis analysis : analyses.get(designElementID)) {
                            if (analysis.getEfvName().equals(uniqueFactorValue)) {
                                // locate the position of this design element in the DE matrix
                                int deIndex = designElementIndex.get(designElementID);

                                // found the right analysis, add - make sure these are ordered the same as uEFV
                                pval.setDouble(pval.getIndex().set(deIndex, uefvIndex), analysis.getPValAdjusted());
                                tstat.setDouble(tstat.getIndex().set(deIndex, uefvIndex), analysis.getTStatistic());

                                // and quit this loop
                                foundAnalysis = true;
                                break;
                            }
                        }

                        // if we couldn't find an appropriate analysis, warn
                        if (!foundAnalysis) {
                            unmappedProperties.add(designElementID + ":" + uniqueFactorValue);
                        }
                    }
                }
                else {
                    unmappedDesignElements.add(designElementID);
                }
            }

            if (unmappedProperties.size() > 0 || unmappedDesignElements.size() > 0) {
                if (unmappedDesignElements.size() > 0) {
                    // todo - log unmapped design elements to a file
                }

                if (unmappedProperties.size() > 0) {
                    // todo - log unmapped design element/propterty value cells to a file
                }

                // count missing cells
                int count = unmappedProperties.size() +
                        (unmappedDesignElements.size() * uefvMaxLength);
                int total = deMaxLength * uefvMaxLength;
                log.warn("No analysis present for " + count + "/" + total + " " +
                        "design element/factor value pairs: stats matrix cells will " +
                        "default to 0 for each affected cell");
            }

            netCDF.write("PVAL", pval);
            netCDF.write("TSTAT", tstat);
        }
        log.debug("Wrote stats data matrix ok.");
    }
}
