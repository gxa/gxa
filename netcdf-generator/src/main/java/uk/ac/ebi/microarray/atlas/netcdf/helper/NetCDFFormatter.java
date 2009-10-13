package uk.ac.ebi.microarray.atlas.netcdf.helper;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

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
  private Dimension assayDimension;
  private Dimension sampleDimension;
  private Dimension designElementDimension;
  private Dimension uefvDimension;

  public synchronized void formatNetCDF(
      NetcdfFileWriteable netCDF, DataSlice dataSlice)
      throws NetCDFGeneratorException {
    clear();

    // setup assay part of netCDF
    createAssayVariables(netCDF, dataSlice.getAssays());

    // setup sample part of netCDF
    createSampleVariables(
        netCDF,
        dataSlice.getSamples());

    // set assay to sample part of netCDF (BS2AS matrix)
    createAssaySampleVariable(
        netCDF);

    // setup design element part of netCDF
    createDesignElementVariables(
        netCDF,
        dataSlice.getDesignElementIDs());

    // setup gene part of netCDF
    createGeneVariables(
        netCDF,
        dataSlice.getGenes());

    // setup property parts of the netCDF -
    // this depends on AS and BS dimensions being in place
    createPropertyVariables(
        netCDF,
        dataSlice.getAssays(),
        dataSlice.getSamples());

    // setup expression values matrix -
    // this depends on AS and DE dimensions being in place
    createExpressionMatrixVariables(
        netCDF);

    // setup stats matrices -
    // this depends on DE and uEFV
    createStatsMatricesVariables(
        netCDF);
  }

  private void clear() {
    assayDimension = null;
    sampleDimension = null;
    designElementDimension = null;
    uefvDimension = null;
  }

  /**
   * Creates dimensions and variables in a NetCDF for a list of assays.  This
   * results in the creation of the "AS" dimension and variable.
   *
   * @param netCDF the NetCDF model to modify
   * @param assays the list of assays that will be used to configure this
   *               NetCDF
   */
  private void createAssayVariables(NetcdfFileWriteable netCDF,
                                    List<Assay> assays) {
    // update the netCDF with the assay count
    assayDimension = netCDF.addDimension("AS", assays.size());
    // add assay data variable
    netCDF.addVariable("AS", DataType.INT, new Dimension[]{assayDimension});
  }

  /**
   * Creates dimensions and variables in a NetCDF for a list of samples.  This
   * results in the creation of the "BS" dimension and variable.
   *
   * @param netCDF  the NetCDF model to modify
   * @param samples the list of samples that will be used to configure this
   *                NetCDF
   */
  private void createSampleVariables(NetcdfFileWriteable netCDF,
                                     List<Sample> samples) {
    // update the netCDF with the sample count
    sampleDimension = netCDF.addDimension("BS", samples.size());
    // add sample variable
    netCDF.addVariable("BS", DataType.INT, new Dimension[]{sampleDimension});
  }

  private void createAssaySampleVariable(NetcdfFileWriteable netCDF) {
    // add assay to sample variable
    netCDF.addVariable("BS2AS", DataType.INT,
                       new Dimension[]{sampleDimension, assayDimension});
  }

  /**
   * Creates dimensions and variables in a NetCDF for a list of design element
   * identifiers.  This results in the creation of the "DE" dimension and
   * variable.
   *
   * @param netCDF           the NetCDF model to modify
   * @param designElementIDs the list of unique identifiers for design elements
   *                         that will be used to configure this NetCDF
   */
  private void createDesignElementVariables(
      NetcdfFileWriteable netCDF, List<Integer> designElementIDs) {
    // update the netCDF with the genes count
    designElementDimension = netCDF.addDimension("DE", designElementIDs.size());
    // add gene variable
    netCDF.addVariable("DE", DataType.INT,
                       new Dimension[]{designElementDimension});
  }

  /**
   * Creates dimensions and variables in a NetCDF for a list of genes.  This
   * results in the creation of the "GN" dimension and variable.
   *
   * @param netCDF the NetCDF model to modify
   * @param genes  the list of genes that will be used to configure this NetCDF
   */
  private void createGeneVariables(NetcdfFileWriteable netCDF,
                                   List<Gene> genes) {
    // update the netCDF with the genes count
    Dimension geneDimension =
        netCDF.addDimension("GN", genes.size());
    // add gene variable
    netCDF.addVariable("GN", DataType.INT,
                       new Dimension[]{geneDimension});
  }

  /**
   * Creates the variables for the property-based matrices.  There are several
   * matrices created here, "EF", "EFV", "uEFV" and "uEFVnum".  In turn, these
   * represent the experiment factors (or assay properties) in the data, the
   * experiment factor values, the unique experiment factor/experiment factor
   * value combinations, and the number of times a unique combination of
   * experiment factor/experiment factor value was seen.
   * <p/>
   * Some of these matrices map values to assays, and some to samples, and as
   * such the "AS" and "BS" dimensions should already be present in the supplied
   * NetCDF.
   *
   * @param netCDF  the NetcdfFileWriteable currently being set up
   * @param assays  the assays being used to generate this NetCDF file
   * @param samples the samples being used to generate this NetCDF file
   * @throws uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException
   *          if dependent matrices "AS" or "BS" have not previously been
   *          configured for this NetCDF.
   */
  private void createPropertyVariables(NetcdfFileWriteable netCDF,
                                       List<Assay> assays,
                                       List<Sample> samples)
      throws NetCDFGeneratorException {
    if (assayDimension == null) {
      throw new NetCDFGeneratorException("Can't create property matrices " +
          "without first creating 'AS' dimension and variable");
    }
    if (sampleDimension == null) {
      throw new NetCDFGeneratorException("Can't create property matrices " +
          "without first creating 'BS' dimension and variable");
    }

    // build the maps of the data we need

    // maps property names to all values for assay properties
    Map<String, List<String>> assayPropertyValues =
        new HashMap<String, List<String>>();
    // maps property names to all values for sample properties
    Map<String, List<String>> samplePropertyValues =
        new HashMap<String, List<String>>();

    // List of property names seen across all assays - duplicate names allowed
    List<String> uniqueCombo = new ArrayList<String>();

    // check all assays
    for (Assay assay : assays) {
      // get all assay properties
      for (Property prop : assay.getProperties()) {
        // add to unique combo
        uniqueCombo.add(prop.getName());

        // have we seen this property name before?
        if (assayPropertyValues.containsKey(prop.getName())) {
          // if so, add values to the existing list
          assayPropertyValues.get(prop.getName()).add(prop.getValue());
        }
        else {
          // otherwise, start a new list and add it, keyed by the new name
          List<String> propertyNames = new ArrayList<String>();
          propertyNames.add(prop.getValue());
          assayPropertyValues.put(prop.getName(), propertyNames);
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

    // now stack up property slicing

    // first up, EF - length, number of properties
    if (assayPropertyValues.keySet().size() > 0) {
      Dimension efDimension =
          netCDF.addDimension("EF", assayPropertyValues.keySet().size());
      // do a count of the max number of values
      int maxLength = 0;
      for (List<String> propertyValues : assayPropertyValues.values()) {
        if (propertyValues.size() > maxLength) {
          maxLength = propertyValues.size();
        }
      }
      // next up, EFV length - this is equal to max number of values mapped to one property
      Dimension efvDimension =
          netCDF.addDimension("EFlen", maxLength);
      // next, unique EFVs - this is the total number of property values for all proeprties
      uefvDimension = netCDF.addDimension("uEFV", uniqueCombo.size());

      // now add variables
      netCDF.addVariable("EF", DataType.CHAR,
                         new Dimension[]{efDimension, efvDimension});
      netCDF.addVariable("EFV", DataType.CHAR,
                         new Dimension[]{efDimension, assayDimension,
                                         efvDimension});
      netCDF.addVariable("uEFV", DataType.CHAR,
                         new Dimension[]{uefvDimension, efvDimension});
      netCDF.addVariable("uEFVnum", DataType.INT, new Dimension[]{efDimension});
    }

    // finally, do the same thing for sample properties
    if (samplePropertyValues.size() > 0) {
      Dimension scDimension =
          netCDF.addDimension("SC", samplePropertyValues.keySet().size());
      // do a count of the max number of values
      int maxSCLength = 0;
      for (List<String> propertyValues : samplePropertyValues.values()) {
        if (propertyValues.size() > maxSCLength) {
          maxSCLength = propertyValues.size();
        }
      }
      Dimension sclDimension = netCDF.addDimension("SClen", maxSCLength);

      // and add variables
      netCDF.addVariable("SC", DataType.CHAR,
                         new Dimension[]{scDimension, sclDimension});
      netCDF.addVariable("SCV", DataType.CHAR,
                         new Dimension[]{scDimension, sampleDimension,
                                         sclDimension});
    }
  }

  /**
   * Creates the variables for the expression value matrix.  This matrix is
   * keyed on the name "BDC".  It is a 2D matrix of expression values for design
   * elements against assays, so this method requires that both "DE" and "AS"
   * dimensions have already been created in the NetcdfFileWriteable supplied.
   *
   * @param netCDF the NetcdfFileWriteable currently being set up
   * @throws uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException
   *          if the dependent matrix "AS" hasn't first been created
   */
  private void createExpressionMatrixVariables(NetcdfFileWriteable netCDF)
      throws NetCDFGeneratorException {
    if (assayDimension == null) {
      throw new NetCDFGeneratorException("Can't create expression value " +
          "matrix without first creating 'AS' dimension and variable");
    }

    netCDF.addVariable("BDC", DataType.DOUBLE,
                       new Dimension[]{designElementDimension, assayDimension});
  }

  /**
   * This creates the variables for the statistics matrices.  This actually
   * builds two matrices, one of P value statistics and one of T statistics.
   * These matrices are both 2D matrices of T or P values for design elements
   * against unique property/property value combinations.  This method therefore
   * requires that both these "DE" and "uEFV" dimensions have already been
   * created in the NetcdfFileWriteable supplied.
   *
   * @param netCDF the NetcdfFileWriteable currently being set up
   * @throws uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException
   *          if the dependent matrices "DE" and "uEFV" haven't first been
   *          created
   */
  private void createStatsMatricesVariables(NetcdfFileWriteable netCDF)
      throws NetCDFGeneratorException {
    if (designElementDimension == null) {
      throw new NetCDFGeneratorException("Can't create stats matrices " +
          "without first creating 'DE' dimension and variable");
    }
    if (uefvDimension != null) {
      // note that is uefvDimension is null, this isn't an exception,
      // as there is an edge case where there may be no properties
      netCDF.addVariable("PVAL", DataType.DOUBLE,
                         new Dimension[]{designElementDimension,
                                         uefvDimension});
      netCDF.addVariable("TSTAT", DataType.DOUBLE,
                         new Dimension[]{designElementDimension,
                                         uefvDimension});
    }
  }
}
