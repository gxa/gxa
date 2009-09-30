package uk.ac.ebi.microarray.atlas.netcdf.helper;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

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
public class NetCDFFormatter {
  public void formatNetCDF(NetcdfFileWriteable netCDF, DataSlice dataSlice) {
    // setup assay part of netCDF
    writeAssayVariables(netCDF, dataSlice.getAssays());

    // setup sample part of netCDF
    writeSampleVariables(
        netCDF,
        dataSlice.getSamples());

    // setup design element part of netCDF
    writeDesignElementVariables(
        netCDF,
        dataSlice.getDesignElementIDs());

    // setup gene part of netCDF
    writeGeneVariables(
        netCDF,
        dataSlice.getGenes());

    // setup property parts of the netCDF
    writePropertyVariables(
        netCDF,
        dataSlice.getAssayToSampleMapping());
  }

  private Dimension writeAssayVariables(NetcdfFileWriteable netCDF,
                                        List<Assay> assays) {
    // update the netCDF with the assay count
    Dimension assayDimension = netCDF.addDimension("AS", assays.size());
    // add assay data variable
    netCDF.addVariable("AS", DataType.INT, new Dimension[]{assayDimension});

    return assayDimension;
  }

  private Dimension writeSampleVariables(NetcdfFileWriteable netCDF,
                                         List<Sample> samples) {
    // update the netCDF with the sample count
    Dimension sampleDimension = netCDF.addDimension("BS", samples.size());
    // add sample variable
    netCDF.addVariable("BS", DataType.INT, new Dimension[]{sampleDimension});

    return sampleDimension;
  }

  private Dimension writeDesignElementVariables(
      NetcdfFileWriteable netCDF, List<Integer> designElementIDs) {
    // update the netCDF with the genes count
    Dimension designElementDimension =
        netCDF.addDimension("DE", designElementIDs.size());
    // add gene variable
    netCDF.addVariable("DE", DataType.INT,
                       new Dimension[]{designElementDimension});

    return designElementDimension;
  }

  private Dimension writeGeneVariables(NetcdfFileWriteable netCDF,
                                       List<Gene> genes) {
    // update the netCDF with the genes count
    Dimension geneDimension =
        netCDF.addDimension("GN", genes.size());
    // add gene variable
    netCDF.addVariable("GN", DataType.INT,
                       new Dimension[]{geneDimension});

    return geneDimension;
  }

  private void writePropertyVariables(NetcdfFileWriteable netCDF,
                                      Map<Assay, List<Sample>> assayToSamples) {
    // build the mapping of property name to values
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

    // now stack up property slicing

    // first up, EF - length, number of properties
    Dimension efDimension =
        netCDF.addDimension("EF", propertyNameToValues.keySet().size());
    // next up, EFV length - this is equal to max number of values mapped to one property
    int maxLength = 0;
    for (List<String> propertyValues : propertyNameToValues.values()) {
      if (propertyValues.size() > maxLength) {
        maxLength = propertyValues.size();
      }
    }
    Dimension efvDimension =
        netCDF.addDimension("EFlen", maxLength);
    // next, unique EFVs - actually this is only the same as our keyset
    Dimension uefvDimension = netCDF.addDimension("uEFV", propertyNameToValues
        .keySet().size()); // fixme - this is wrong, should be intersection of property and value not just the names again

    // lookup assay dimension
    Dimension assayDimension = netCDF.findDimension("AS");
    // now add variables
    netCDF.addVariable("EF", DataType.CHAR,
                       new Dimension[]{efDimension, efvDimension});
    netCDF.addVariable("EFV", DataType.CHAR,
                       new Dimension[]{efDimension, assayDimension,
                                       efvDimension});
    netCDF.addVariable("uEFV", DataType.CHAR,
                       new Dimension[]{uefvDimension, efvDimension});
    netCDF.addVariable("uEFVnum", DataType.INT, new Dimension[]{efDimension});

    // finally, do the same thing for sample properties
    Dimension scDimension =
        netCDF.addDimension("SC", samplePropertyNameToValues.keySet().size());
    int maxSCLength = 0;
    for (List<String> propertyValues : propertyNameToValues.values()) {
      if (propertyValues.size() > maxSCLength) {
        maxSCLength = propertyValues.size();
      }
    }
    Dimension sclDimension =
        netCDF.addDimension("SClen", maxSCLength);

    // lookup sample dimension
    Dimension sampleDimension = netCDF.findDimension("BS");
    // and add variables
    netCDF.addVariable("SC", DataType.CHAR,
                       new Dimension[]{scDimension, sclDimension});
    netCDF.addVariable("SCV", DataType.CHAR,
                       new Dimension[]{scDimension, sampleDimension,
                                       sclDimension});
  }
}
