package uk.ac.ebi.microarray.atlas.netcdf.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class NetCDFWriter {
  private Log log = LogFactory.getLog(this.getClass());

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
          dataSlice.getAssayToSampleMapping());

      // write design element data
      writeDesignElementData(
          netCDF,
          dataSlice.getDesignElementIDs());

      // write gene data
      writeGeneData(
          netCDF,
          dataSlice.getDesignElementIDs(),
          dataSlice.getGenes());

      // write property data
      writePropertyData(
          netCDF,
          dataSlice.getAssays(),
          dataSlice.getSamples());

      // write expression matrix values
      writeExpressionMatrixValues(
          netCDF,
          dataSlice.getAssays());

      // write stats matrix values
      writeStatsValues(
          netCDF,
          dataSlice.getDesignElementIDs(),
          dataSlice.getExpressionAnalyses());
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
      for (Assay assay : assays) {
        asIt.setIntNext(assay.getAssayID());
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
                                      Map<Assay, List<Sample>> assayToSamples)
      throws NetCDFGeneratorException, IOException, InvalidRangeException {
    // build unique maps of IDs to index
    Map<Integer, Integer> assays = new HashMap<Integer, Integer>();
    Map<Integer, Integer> samples = new HashMap<Integer, Integer>();

    // index counters
    int assayIndex = 0;
    int sampleIndex = 0;
    for (Assay assay : assayToSamples.keySet()) {
      if (!assays.containsKey(assay.getAssayID())) {
        // add assayId to our assayMap
        assays.put(assay.getAssayID(), assayIndex);
        // and increment the index
        assayIndex++;
      }

      for (Sample sample : assayToSamples.get(assay)) {
        if (!samples.containsKey(sample.getSampleID())) {
          // add sampleId to our sampleMap
          samples.put(sample.getSampleID(), sampleIndex);
          // and increment the count
          sampleIndex++;
        }
      }
    }

    if (samples.size() > 0 && assays.size() > 0) {
      ArrayInt bs2as = new ArrayInt.D2(samples.size(),
                                       assays.size());
      // initialize the matrix with zeros
      IndexIterator bs2asIt = bs2as.getIndexIterator();
      while (bs2asIt.hasNext()) {
        bs2asIt.setIntNext(0);
      }

      // iterate over keys, and work out which spot in the matrix to set to 1
      for (Assay assay : assayToSamples.keySet()) {
        for (Sample sample : assayToSamples.get(assay)) {
          // insert value
          int sIndex = samples.get(sample.getSampleID());
          int aIndex = assays.get(assay.getAssayID());

          bs2as.setInt(bs2as.getIndex().set(sIndex, aIndex), 1);
        }
      }

      // finally, write
      netCDF.write("BS2AS", bs2as);
    }
    log.debug("Wrote assay2sample data matrix ok.");
  }

  private void writeDesignElementData(NetcdfFileWriteable netCDF,
                                      List<Integer> designElementIDs)
      throws IOException, InvalidRangeException {
    if (netCDF.findDimension("DE") != null) {
      // add design element id data
      ArrayInt de = new ArrayInt.D1(designElementIDs.size());
      IndexIterator deIt = de.getIndexIterator();
      for (int designElementID : designElementIDs) {
        deIt.setIntNext(designElementID);
      }
      netCDF.write("DE", de);
    }
    log.debug("Wrote design element data matrix ok.");
  }

  private void writeGeneData(NetcdfFileWriteable netCDF,
                             List<Integer> designElements,
                             Map<Integer, Gene> genes)
      throws IOException, InvalidRangeException {
    if (netCDF.findDimension("GN") != null) {
      ArrayInt gn;

      // check that we have an appropriate mapping
      if (genes.keySet().size() != designElements.size()) {
        log.warn(
            "Mismatched design element index to gene index.  " +
                "GN will be created using design element counts.");
        // add gene id data for stored design elements
        gn = new ArrayInt.D1(designElements.size());
        IndexIterator gnIt = gn.getIndexIterator();
        for (int designElement : designElements) {
          if (genes.get(designElement) == null) {
            gnIt.setIntNext(0);
          }
          else {
            gnIt.setIntNext(genes.get(designElement).getGeneID());
          }
        }
      }
      else {
        // add gene id data
        gn = new ArrayInt.D1(genes.size());
        IndexIterator gnIt = gn.getIndexIterator();
        for (int designElement : genes.keySet()) {
          gnIt.setIntNext(genes.get(designElement).getGeneID());
        }
      }

      // write out the data
      netCDF.write("GN", gn);
    }
    log.debug("Wrote gene data matrix ok.");
  }


  private void writePropertyData(NetcdfFileWriteable netCDF,
                                 List<Assay> assays,
                                 List<Sample> samples)
      throws IOException, InvalidRangeException {
    // build the maps of the data we need

    // maps property names to all values for assay properties
    Map<String, List<String>> assayPropertyValues =
        new HashMap<String, List<String>>();
    // maps property names to all values for sample properties
    Map<String, List<String>> samplePropertyValues =
        new HashMap<String, List<String>>();

    // List of property names seen across all assays - duplicate names allowed
    Map<String, Integer> uniqueCounts = new HashMap<String, Integer>();

    // check all assays
    for (Assay assay : assays) {
      // get all assay properties
      for (Property prop : assay.getProperties()) {
        // have we seen this property name before?
        if (assayPropertyValues.containsKey(prop.getName())) {
          // if so, add values to the existing list
          assayPropertyValues.get(prop.getName()).add(prop.getValue());

          // and increment the count by one
        }
        else {
          // otherwise, start a new list and add it, keyed by the new name
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          assayPropertyValues.put(prop.getName(), propertyNames);

          // add a new count of 1
        }

        // have we seen this property name for our counts before
        if (uniqueCounts.containsKey(prop.getName())) {
          // if so, increment the count
          Integer update = uniqueCounts.get(prop.getName()) + 1;
          uniqueCounts.put(prop.getName(), update);
        }
        else {
          // otherwise, add a new count of 1
          uniqueCounts.put(prop.getName(), 1);
        }
      }
    }

    // check all samples
    for (Sample sample : samples) {
      // get all sample properties
      for (Property prop : sample.getProperties()) {
        // have we seen this property name before?
        if (samplePropertyValues.containsKey(prop.getName())) {
          // if so, add values to the existing list
          samplePropertyValues.get(prop.getName()).add(prop.getValue());
        }
        else {
          // otherwise, start a new list and add it, keyed by the new name
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          samplePropertyValues.put(prop.getName(), propertyNames);
        }
      }
    }

    // use this data to write all the required property data

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

      int efIndex = 0;
      int efvIndex = 0;
      int uefvIndex = 0;

      for (String propertyName : assayPropertyValues.keySet()) {
        // add property name to EF
        ef.setString(efIndex, propertyName);

        for (String propertyValue : assayPropertyValues.get(propertyName)) {
          if (efvIndex < netCDF.findDimension("AS").getLength()) {
            // add property value to EFV, indexed by each ef
            efv.setString(efv.getIndex().set(efIndex, efvIndex), propertyValue);
            // increment index count on efv axis
            efvIndex++;

            // add property value to uEFV, indexed by cumulative count uefvIndex
            uefv.setString(uefvIndex, propertyValue);
          }
          else {
            // this will occur if there are multiple property values assigned
            // to the same property for single assay

            // apparently, this is wrong somehow
            log.error("Multiple property values assigned to the same " +
                "property for single assay!");
            break;
          }
        }

        // increment ef axis up one, and reset efv axis to zero
        efIndex++;
        // increment uefv axis up one, this follows count on uEFV index
        uefvIndex++;
        efvIndex = 0;
      }

      ArrayInt uefvNum = new ArrayInt.D1(
          netCDF.findDimension("EF").getLength());
      int countIndex = 0;
      for (String propertyName : uniqueCounts.keySet()) {
        uefvNum.setInt(uefvNum.getIndex().set(countIndex),
                       uniqueCounts.get(propertyName));
        countIndex++;
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
      for (String propertyName : samplePropertyValues.keySet()) {
        sc.setString(scIndex, propertyName);

        for (String propertyValue : samplePropertyValues.get(propertyName)) {
          if (scvIndex < netCDF.findDimension("BS").getLength()) {
            // add property value to SCV, indexed by each SC
            scv.setString(scv.getIndex().set(scIndex, scvIndex), propertyValue);
            // increment index count on scv axis
            scvIndex++;
          }
          else {
            // fixme:
            // this will occur if there are multiple property values assigned
            // to the same property for single assay - in old generation this results in lost data
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

  private void writeExpressionMatrixValues(NetcdfFileWriteable netCDF,
                                           List<Assay> assays)
      throws IOException, InvalidRangeException {
    if (netCDF.findDimension("AS") != null &&
        netCDF.findDimension("DE") != null) {
      // count expression values
      int expressionValueCount = 0;
      for (Assay assay : assays) {
        if (expressionValueCount < assay.getExpressionValues().size()) {
          expressionValueCount = assay.getExpressionValues().size();
        }
      }

      // create matrix for BDC
      ArrayDouble bdc = new ArrayDouble.D2(expressionValueCount,
                                           assays.size());

      // initialise everything to -1000000, default value
      IndexIterator bdcIt = bdc.getIndexIterator();
      while (bdcIt.hasNext()) {
        bdcIt.setDoubleNext(-1000000);
      }

      // loop over assays
      int asIndex = 0;
      int deIndex = 0;
      for (Assay assay : assays) {
        for (ExpressionValue ev : assay.getExpressionValues()) {
          // write expression value to matrix
          bdc.setDouble(bdc.getIndex().set(deIndex, asIndex), ev.getValue());

          // increment deIndex
          deIndex++;
        }

        asIndex++;
        deIndex = 0;
      }

      netCDF.write("BDC", bdc);
    }
    log.debug("Wrote expression data matrix ok.");
  }

  private void writeStatsValues(NetcdfFileWriteable netCDF,
                                List<Integer> designElements,
                                Map<Integer, List<ExpressionAnalysis>> analyses)
      throws IOException, InvalidRangeException {
    if (netCDF.findDimension("DE") != null &&
        netCDF.findDimension("uEFV") != null) {
      int deMaxLength = netCDF.findDimension("DE").getLength();
      int uefvMaxLength = netCDF.findDimension("uEFV").getLength();
      ArrayDouble pval = new ArrayDouble.D2(
          deMaxLength, uefvMaxLength);
      ArrayDouble tstat = new ArrayDouble.D2(
          deMaxLength, uefvMaxLength);

      // loop over design elements
      int deIndex = 0;
      int uefvIndex = 0;
      for (int designElementID : designElements) {
        if (analyses.get(designElementID) != null) {
          for (ExpressionAnalysis analysis : analyses.get(designElementID)) {
            pval.setDouble(pval.getIndex().set(deIndex, uefvIndex),
                           analysis.getPValAdjusted());
            tstat.setDouble(tstat.getIndex().set(deIndex, uefvIndex),
                            analysis.getTStatistic());

            // increment uefv index
            uefvIndex++;
          }
        }
        else {
          log.warn("No analyses present for design element " + designElementID +
              ": stats matrix cells will be default to 0");
        }
        deIndex++;
        uefvIndex = 0;
      }

      netCDF.write("PVAL", pval);
      netCDF.write("TSTAT", tstat);
    }
    log.debug("Wrote stats data matrix ok.");
  }
}
