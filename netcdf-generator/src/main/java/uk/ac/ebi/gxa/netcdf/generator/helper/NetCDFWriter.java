package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;

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
  Map<Integer, Integer> assayIndex = new HashMap<Integer, Integer>();
  Map<Integer, Integer> designElementIndex = new HashMap<Integer, Integer>();

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
          dataSlice.getDesignElements(),
          dataSlice.getGeneMappings());

      // write property data
      writePropertyData(
          netCDF,
          dataSlice.getAssays(),
          dataSlice.getExperimentFactorMappings(),
          dataSlice.getAssayFactorValueMappings(),
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
          dataSlice.getAssays(),
          dataSlice.getAssayFactorValueMappings());
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
    if (netCDF.findDimension("AS") != null &&
        netCDF.findDimension("BS") != null) {
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
                             Map<Integer, String> designElements,
                             Map<Integer, Gene> geneMappings)
      throws IOException, InvalidRangeException, NetCDFGeneratorException {
    if (netCDF.findDimension("GN") != null) {
      ArrayInt gn;

      // check that we have an appropriate mapping
      if (geneMappings.size() != designElements.size()) {
        throw new NetCDFGeneratorException(
            "Mismatched design element index to gene index.  " +
                "Something must have gone wrong during data slicing!");
      }

      // add gene id data for stored design elements
      gn = new ArrayInt.D1(designElements.size());
      IndexIterator gnIt = gn.getIndexIterator();
      for (int designElement : designElements.keySet()) {
        if (geneMappings.get(designElement) == null) {
          gnIt.setIntNext(0);
        }
        else {
          gnIt.setIntNext(geneMappings.get(designElement).getGeneID());
        }
      }

      // write out the data
      netCDF.write("GN", gn);
    }
    log.debug("Wrote gene data matrix ok.");
  }


  private void writePropertyData(
      NetcdfFileWriteable netCDF,
      List<Assay> assays,
      Map<String, List<String>> experimentFactorMap,
      Map<Assay, List<String>> assayFactorValueMap,
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
          netCDF.findDimension("AS").getLength());

      // populate ef, efv matrices
      int efIndex = 0;
      int efvIndex = 0;

      for (String propertyName : experimentFactorMap.keySet()) {
        // add property name to EF
        ef.setString(efIndex, propertyName);

        for (String propertyValue : experimentFactorMap.get(propertyName)) {
          if (efvIndex < netCDF.findDimension("AS").getLength()) {
            // add property value to EFV, indexed by each ef
            efv.setString(efv.getIndex().set(efIndex, efvIndex), propertyValue);
            // increment index count on efv axis
            efvIndex++;
          }
          else {
            // fixme:
            // this will occur if there are multiple property values assigned
            // to the same property for single assay - in theory this shouldn't
            // happen, but it is technically possible.
            // In the old software, this results in lost data
            log.error("Multiple property values assigned to the same " +
                "property for single assay!");
            break;
          }
        }

        // increment ef axis up one, and reset efv axis to zero
        efIndex++;
        efvIndex = 0;
      }

      // populate uefv, uefvnum matrices
      int uefvIndex = 0;
      int uefvNumIndex = 0;

      for (Assay ass : assays) {
        // uefv gets populated with grouped observed values
        for (String propertyValue : assayFactorValueMap.get(ass)) {
          // add property value to uEFV, indexed by cumulative count uefvIndex
          uefv.setString(uefvIndex, propertyValue);
          // increment uefv index up one - running total
          uefvIndex++;
        }

        // uefvNum gets populated with the counts of values pper assay
        uefvNum.setInt(uefvNum.getIndex().set(uefvNumIndex),
                       assayFactorValueMap.get(ass).size());
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
      int scIndex = 0;
      int scvIndex = 0;
      for (String propertyName : sampleCharacteristicMap.keySet()) {
        sc.setString(scIndex, propertyName);

        for (String propertyValue : sampleCharacteristicMap.get(propertyName)) {
          if (scvIndex < netCDF.findDimension("BS").getLength()) {
            // add property value to SCV, indexed by each SC
            scv.setString(scv.getIndex().set(scIndex, scvIndex), propertyValue);
            // increment index count on scv axis
            scvIndex++;
          }
          else {
            // fixme:
            // this will occur if there are multiple property values assigned
            // to the same property for single sample - in theory this shouldn't
            // happen, but it is technically possible.
            // In the old software, this results in lost data
            log.error("Multiple property values assigned to the same " +
                "property for single sample!");
            break;
          }
        }

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
                                List<Assay> assays,
                                Map<Assay, List<String>> assayFactorValueMap)
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

      int deIndex = 0;
      int uefvIndex = 0;
      // loop over design elements
      for (int designElementID : designElements.keySet()) {
        if (analyses.get(designElementID) != null) {
          // not null, so now loop over values of the assayFactorValueMap,
          // so  we can lookup analysis by design element id and property value
          for (Assay a : assays) {
            for (String propertyValue : assayFactorValueMap.get(a)) {
              boolean foundAnalysis = false;
              for (ExpressionAnalysis analysis : analyses
                  .get(designElementID)) {
                if (analysis.getEfvName().equals(propertyValue)) {
                  // found the right analysis, add
                  pval.setDouble(pval.getIndex().set(deIndex, uefvIndex),
                                 analysis.getPValAdjusted());
                  tstat.setDouble(tstat.getIndex().set(deIndex, uefvIndex),
                                  analysis.getTStatistic());

                  // and quit this loop
                  foundAnalysis = true;
                  break;
                }
              }

              // if we couldn't find an appropriate analysis, warn
              if (!foundAnalysis) {
                unmappedProperties.add(designElementID + ":" + propertyValue);
              }

              // increment uefv index, whether or not we found the analysis
              uefvIndex++;
            }
          }
        }
        else {
          unmappedDesignElements.add(designElementID);
        }
        deIndex++;
        uefvIndex = 0;
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
