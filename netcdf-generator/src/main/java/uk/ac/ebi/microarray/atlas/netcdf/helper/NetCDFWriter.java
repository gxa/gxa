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
import uk.ac.ebi.microarray.atlas.netcdf.helper.DataSlice;

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
      for (Assay assay : dataSlice.getAssays()) {
        writeSampleData(
            netCDF,
            dataSlice.getSamplesAssociatedWithAssay(
                assay.getAccession()));
      }

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
          dataSlice.getAssayToSampleMapping());
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
                                 Map<Assay, List<Sample>> assayToSamples)
      throws IOException, InvalidRangeException {
    // build the list of all properties, and the mapping of property name to values
    Map<String, List<String>> propertyNameToValues =
        new HashMap<String, List<String>>();
    Map<String, List<String>> samplePropertyNameToValues =
        new HashMap<String, List<String>>();
    for (Assay assay : assayToSamples.keySet()) {
      for (Property prop : assay.getProperties()) {
        if (propertyNameToValues.containsKey(prop.getName())) {
          // add to existing
          propertyNameToValues.get(prop.getName()).add(prop.getValue());
        }
        else {
          // create a new list and add to that
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          propertyNameToValues.put(prop.getName(), propertyNames);
        }
      }

      for (Sample sample : assayToSamples.get(assay)) {
        for (Property prop : sample.getProperties()) {
          if (propertyNameToValues.containsKey(prop.getName())) {
            // add to existing
            propertyNameToValues.get(prop.getName()).add(prop.getValue());
          }
          else {
            // create a new list and add to that
            List<String> propertyNames = new ArrayList<String>();
            propertyNames.add(prop.getValue());
            propertyNameToValues.put(prop.getName(), propertyNames);
          }

          // also add sample properties to the separate map for some reason
          if (samplePropertyNameToValues.containsKey(prop.getName())) {
            samplePropertyNameToValues.get(prop.getName()).add(prop.getValue());
          }
          else {
            List<String> propertyNames = new ArrayList<String>();
            propertyNames.add(prop.getValue());
            samplePropertyNameToValues.put(prop.getName(), propertyNames);
          }
        }
      }
    }

    // we now have a list of all properties and values
    // and a mapping of unique properties to the set of all observed values

    // use this data to write all the required property data
    ArrayChar ef = new ArrayChar.D2(
        netCDF.findDimension("EF").getLength(),
        netCDF.findDimension("EFlen").getLength());
    ArrayChar efv = new ArrayChar.D3(
        netCDF.findDimension("EF").getLength(),
        netCDF.findDimension("AS").getLength(),
        netCDF.findDimension("EFlen").getLength());
    int i = 0;
    for (String propertyName : propertyNameToValues.keySet()) {
      ef.setString(i, propertyName);

      int j = 0;
      for (String propertyValue : propertyNameToValues.get(propertyName)) {
        efv.setString(efv.getIndex().set(i, j), propertyValue);
        j++;
      }
      i++;
    }

    ArrayChar uefv = new ArrayChar.D2(
        netCDF.findDimension("uEFV").getLength(),
        netCDF.findDimension("EFlen").getLength());
    int k = 0;
    for (String propertyName : propertyNameToValues
        .keySet()) { // fixme: again, this is wrong, need intersection of property/property value not just properties again
      uefv.setString(k, propertyName);
      k++;
    }

    ArrayInt uefvNum = new ArrayInt.D1(
        netCDF.findDimension("EF").getLength());
    int l = 0;
    for (String propertyName : propertyNameToValues
        .keySet()) { // fixme: again, this is wrong, need intersection of property/property value not just properties again
      uefvNum.setInt(efv.getIndex().set(l),
                     propertyNameToValues.get(propertyName).size());
      k++;
    }

    // finally write sample data
    ArrayChar sc = new ArrayChar.D2(
        netCDF.findDimension("SC").getLength(),
        netCDF.findDimension("SClen").getLength());
    ArrayChar scv = new ArrayChar.D3(
        netCDF.findDimension("SC").getLength(),
        netCDF.findDimension("BS").getLength(),
        netCDF.findDimension("SClen").getLength());
    int x = 0;
    for (String propertyName : propertyNameToValues.keySet()) {
      ef.setString(x, propertyName);

      int y = 0;
      for (String propertyValue : propertyNameToValues.get(propertyName)) {
        efv.setString(efv.getIndex().set(x, y), propertyValue);
        y++;
      }
      x++;
    }

    netCDF.write("EF", ef);
    netCDF.write("EFV", efv);
    netCDF.write("uEFV", uefv);
    netCDF.write("uEFVnum", uefvNum);
    netCDF.write("SC", sc);
    netCDF.write("SCV", scv);
  }
}
