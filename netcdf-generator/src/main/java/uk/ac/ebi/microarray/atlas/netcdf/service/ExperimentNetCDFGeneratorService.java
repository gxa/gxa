package uk.ac.ebi.microarray.atlas.netcdf.service;

import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;
import uk.ac.ebi.microarray.atlas.netcdf.model.DataSlice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentNetCDFGeneratorService
    extends NetCDFGeneratorService<File> {
  private static final int NUM_THREADS = 64;

  public ExperimentNetCDFGeneratorService(AtlasDAO atlasDAO,
                                          File repositoryLocation) {
    super(atlasDAO, repositoryLocation);
  }

  protected void createNetCDFDocs() throws NetCDFGeneratorException {
    // do initial setup - build executor service
    ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

    // fetch experiments - check if we want all or only the pending ones
    List<Experiment> experiments = getPendingOnly()
        ? getAtlasDAO().getAllExperimentsPendingNetCDFs()
        : getAtlasDAO().getAllExperiments();
    // fixme - implement update checks required?


    // the list of futures - we need these so we can block until completion
    List<Future<Boolean>> tasks =
        new ArrayList<Future<Boolean>>();

    try {
      // generate the data slices we need
      List<DataSlice> dataSlices = new ArrayList<DataSlice>();

      // loop over experiments - one NetCDF per experiment/array design pair
      for (final Experiment experiment : experiments) {
        // list of all unique array designs in this experiment
        List<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        // map from array design accession to assays
        Map<String, List<Assay>> arrayToAssays =
            new HashMap<String, List<Assay>>();

        // get the assays for this experiment
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(
            experiment.getAccession());

        // loop over assays to get array designs
        for (Assay assay : assays) {
          // get the accession
          String arrayDesignAccession = assay.getArrayDesignAccession();

          // have we seen this array before?
          if (!arrayToAssays.containsKey(arrayDesignAccession)) {
            // if not, fetch it
            ArrayDesign arrayDesign = getAtlasDAO()
                .getArrayDesignByAccession(arrayDesignAccession);

            // add to our set of array designs
            arrayDesigns.add(arrayDesign);

            // and init a new list of assays
            List<Assay> assaySet = new ArrayList<Assay>();
            assaySet.add(assay);
            arrayToAssays.put(arrayDesignAccession, assaySet);
          }
          else {
            // or, just add this assay to the list
            arrayToAssays.get(arrayDesignAccession).add(assay);
          }
        }

        // now our data is appropriately sliced so start building up NetCDFs properly
        for (ArrayDesign arrayDesign : arrayDesigns) {
          String arrayDesignAccession = arrayDesign.getAccession();

          DataSlice dataSlice = new DataSlice(
              experiment, arrayDesign,
              arrayToAssays.get(arrayDesignAccession));
          dataSlices.add(dataSlice);
        }
      }

      // process each dataslice to build the netcdf
      for (final DataSlice dataSlice : dataSlices) {
        // run slices in parallel
        tasks.add(tpool.submit(new Callable<Boolean>() {

          public Boolean call() throws Exception {
            // create a new NetCDF document
            NetcdfFileWriteable netCDF = createNetCDF(
                dataSlice.getExperiment(),
                dataSlice.getArrayDesign());

            // setup assay part of netCDF
            writeAssayVariables(netCDF, dataSlice.getAssays());

            // setup sample part of netCDF
            // and also map assays to samples
            Map<Assay, List<Sample>> assayToSamples =
                new HashMap<Assay, List<Sample>>();
            for (Assay assay : dataSlice.getAssays()) {
              List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(
                  assay.getAccession());
              writeSampleVariables(netCDF, samples);

              // do assayToSample mapping
              assayToSamples.put(assay, samples);
            }

            // setup design element part of netCDF
            // fetch design elements
            List<Integer> designElementIDs = getAtlasDAO()
                .getDesignElementsByArrayAccession(
                    dataSlice.getArrayDesign().getAccession());
            writeDesignElementVariables(netCDF, designElementIDs);

            // setup gene part of netCDF
            // fetch genes
            List<Gene> genes = getAtlasDAO().getGenesByExperimentAccession(
                dataSlice.getExperiment().getAccession());
            writeGeneVariables(netCDF, genes);

            // actually create the netCDF
            netCDF.create();

            // write assay data
            writeAssayData(netCDF, dataSlice.getAssays());

            // write sample data
            for (Assay assay : dataSlice.getAssays()) {
              List<Sample> samples = assayToSamples.get(assay);
              writeSampleData(netCDF, samples);
            }

            // write assay/sample mapping data
            writeAssayToSampleData(netCDF, assayToSamples);

            // write design element data
            writeDesignElementData(netCDF, designElementIDs);

            // write gene data
            writeGeneData(netCDF, genes);

            // todo - still need to write matrices for...
            // BDC - DE/AS
            // PVAL - DE/uEFV
            // TSTAT - DE/uEFV
            // EF - EF/EFV
            // EFV - uEFV/EFV
            // uEFV - uEFV/EFV
            // uEFVnum - EF

            return true;
          }
        }));
      }

      // block until completion, and throw any errors
      for (Future<Boolean> task : tasks) {
        try {
          task.get();
        }
        catch (ExecutionException e) {
          if (e.getCause() instanceof NetCDFGeneratorException) {
            throw (NetCDFGeneratorException) e.getCause();
          }
          else {
            throw new NetCDFGeneratorException(
                "An error occurred updating Experiments SOLR index", e);
          }
        }
        catch (InterruptedException e) {
          throw new NetCDFGeneratorException(
              "An error occurred updating Experiments SOLR index", e);
        }
      }
    }
    finally {
      // shutdown the service
      tpool.shutdown();
    }
  }

  private NetcdfFileWriteable createNetCDF(Experiment experiment,
                                           ArrayDesign arrayDesign) {
    // make a new file
    getLog().info("Generating NetCDF for " +
        "Experiment: " + experiment.getAccession() + ", " +
        "Array Design: " + arrayDesign.getAccession());

    String netcdfName =
        experiment.getExperimentID() + "_" +
            arrayDesign.getArrayDesignID() + ".nc";
    String netcdfPath =
        new File(getRepositoryLocation(), netcdfName).getAbsolutePath();
    NetcdfFileWriteable netcdfFile =
        NetcdfFileWriteable.createNew(netcdfPath, false);

    // add global attributes
    netcdfFile.addGlobalAttribute(
        "CreateNetCDF_VERSION",
        versionDescriptor);
    netcdfFile.addGlobalAttribute(
        "experiment_accession",
        experiment.getAccession());
//    netcdfFile.addGlobalAttribute(
//        "quantitationType",
//        qtType); // fixme: quantitation type lookup required
    netcdfFile.addGlobalAttribute(
        "ADaccession",
        arrayDesign.getAccession());
    netcdfFile.addGlobalAttribute(
        "ADname",
        arrayDesign.getName());

    return netcdfFile;

  }

  private Dimension writeAssayVariables(NetcdfFileWriteable netCDF,
                                        List<Assay> assays) {
    // update the netCDF with the assay count
    Dimension assayDimension = netCDF.addDimension("AS", assays.size());
    // add assay data variable
    netCDF.addVariable("AS", DataType.INT, new Dimension[]{assayDimension});

    return assayDimension;
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

  private Dimension writeSampleVariables(NetcdfFileWriteable netCDF,
                                         List<Sample> samples) {
    // update the netCDF with the sample count
    Dimension sampleDimension = netCDF.addDimension("BS", samples.size());
    // add sample variable
    netCDF.addVariable("BS", DataType.INT, new Dimension[]{sampleDimension});

    return sampleDimension;
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
}
