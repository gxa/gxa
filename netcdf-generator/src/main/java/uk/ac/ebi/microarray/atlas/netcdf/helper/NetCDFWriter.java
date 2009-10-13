package uk.ac.ebi.microarray.atlas.netcdf.helper;

import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayInt;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;
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
          dataSlice.getGenes());

      // write property data
      writePropertyData(
          netCDF,
          dataSlice.getAssays(),
          dataSlice.getSamples());
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
    // add assay id data
    ArrayInt as = new ArrayInt.D1(assays.size());
    IndexIterator asIter = as.getIndexIterator();
    for (Assay assay : assays) {
      asIter.setIntNext(assay.getAssayID());
    }
    netCDF.write("AS", as);
  }

  private void writeSampleData(NetcdfFileWriteable netCDF, List<Sample> samples)
      throws IOException, InvalidRangeException {
    // add assay id data
    ArrayInt bs = new ArrayInt.D1(samples.size());
    IndexIterator asIter = bs.getIndexIterator();
    for (Sample sample : samples) {
      asIter.setIntNext(sample.getSampleID());
    }
    netCDF.write("BS", bs);
  }

  private void writeAssayToSampleData(NetcdfFileWriteable netCDF,
                                      Map<Assay, List<Sample>> assayToSamples)
      throws NetCDFGeneratorException, IOException, InvalidRangeException {
    // fixme: the old version of this copde looked quite brittle, i'm not sure if the cardinality is correct (1:1 sample:assay always?)

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

    ArrayInt bs2as = new ArrayInt.D2(samples.size(),
                                     assays.size());
    //Initialize the matrix with zeros
    IndexIterator iterbs = bs2as.getIndexIterator();
    while (iterbs.hasNext()) {
      iterbs.setIntNext(0);
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
    netCDF.write("BS2AS", bs2as);
  }

  private void writeDesignElementData(NetcdfFileWriteable netCDF,
                                      List<Integer> designElementIDs)
      throws IOException, InvalidRangeException {
    // add design element id data
    ArrayInt de = new ArrayInt.D1(designElementIDs.size());
    IndexIterator asIter = de.getIndexIterator();
    for (int designElementID : designElementIDs) {
      asIter.setIntNext(designElementID);
    }
    netCDF.write("DE", de);
  }

  private void writeGeneData(NetcdfFileWriteable netCDF, List<Gene> genes)
      throws IOException, InvalidRangeException {
    // add design element id data
    ArrayInt gn = new ArrayInt.D1(genes.size());
    IndexIterator asIter = gn.getIndexIterator();
    for (Gene gene : genes) {
      asIter.setIntNext(gene.getGeneID());
    }
    netCDF.write("GN", gn);
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

    // write assay property values
    for (String propertyName : assayPropertyValues.keySet()) {
      // add property name to EF
      ef.setString(efIndex, propertyName);

      for (String propertyValue : assayPropertyValues.get(propertyName)) {
        // add property value to EFV, indexed by each ef
        efv.setString(efv.getIndex().set(efIndex, efvIndex), propertyValue);
        // increment index count on efv axis
        efvIndex++;

        // add property value to uEFV, indexed by cumulative count uefvIndex
        uefv.setString(uefvIndex, propertyValue);
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
      uefvNum.setInt(efv.getIndex().set(countIndex),
                     uniqueCounts.get(propertyName));
      countIndex++;
    }

    // finally write sample data
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
        scv.setString(scv.getIndex().set(scIndex, scvIndex), propertyValue);
        scvIndex++;
      }

      scIndex++;
      scvIndex = 0;
    }

    netCDF.write("EF", ef);
    netCDF.write("EFV", efv);
    netCDF.write("uEFV", uefv);
    netCDF.write("uEFVnum", uefvNum);
    netCDF.write("SC", sc);
    netCDF.write("SCV", scv);
  }
}
