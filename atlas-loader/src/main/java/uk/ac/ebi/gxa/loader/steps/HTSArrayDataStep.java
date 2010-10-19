package uk.ac.ebi.gxa.loader.steps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.*;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.file.FileDescription;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * User: nsklyar
 * Date: Oct 4, 2010
 */
public class HTSArrayDataStep implements Step {

    private final MAGETABInvestigationExt investigation;
    private final AtlasLoadCache cache;
    private AtlasComputeService computeService;

    private final Log log = LogFactory.getLog(this.getClass());


    public HTSArrayDataStep(MAGETABInvestigationExt investigation, AtlasComputeService computeService) {
        this.investigation = investigation;
        this.cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
        this.computeService = computeService;
    }

    public String displayName() {
        return "Processing HTS data";
    }


    public void run() throws AtlasLoaderException {
        log.info("Starting HTS data load");

        // check that data is from RNASeq (comments: "Comment [ENA_RUN]"	"Comment [FASTQ_URI]" must bw present)
        //ToDo: add this check in the Loader
        for (ScanNode scanNode : investigation.SDRF.lookupNodes(ScanNode.class)) {
            if (!(scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
                log.debug("Exit HTSArrayDataStep");
                return;
            }
        }

        // sdrf location
        URL sdrfURL = investigation.SDRF.getLocation();

        File sdrfFilePath = new File(sdrfURL.getFile());
        File inFilePath = new File(sdrfFilePath.getParentFile(), "esetcount.RData");
        File outFilePath = new File(sdrfFilePath.getParentFile(), "out.txt");

        //run files through the pipeline
        runPipeline(inFilePath, outFilePath);

        if (!outFilePath.exists()) {
              throw new AtlasLoaderException("File " + outFilePath + " hasn't been created");
        }

        // try to get the relative filename
        URL dataMatrixURL = null;

        // NB. making sure we replace File separators with '/' to guard against windows issues
        try {
            dataMatrixURL = sdrfURL.getPort() == -1
                    ? new URL(sdrfURL.getProtocol(),
                    sdrfURL.getHost(),
                    outFilePath.toString().replaceAll("\\\\", "/"))
                    : new URL(sdrfURL.getProtocol(),
                    sdrfURL.getHost(),
                    sdrfURL.getPort(),
                    outFilePath.toString().replaceAll("\\\\", "/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("dataMatrixURL = " + dataMatrixURL);

        // now, obtain a buffer for this dataMatrixFile
        log.debug("Opening buffer of data matrix file at " + dataMatrixURL);

        DataMatrixFileBuffer buffer = cache.getDataMatrixFileBuffer(dataMatrixURL, null, false);


        // find the type of nodes we need - lookup from data matrix buffer
        String refNodeName = buffer.getReferenceColumnName();

        // fetch the references from the buffer
        List<String> refNames = buffer.getReferences();

        // for each refName, identify the assay the expression values relate to
        for (int refIndex = 0; refIndex < refNames.size(); ++refIndex) {
            String refName = refNames.get(refIndex);

            log.debug("Attempting to attach expression values to next reference " + refName);
            Assay assay;
            if (refNodeName.equals("scanname")) {
                // this requires mapping the assay upstream of this node to the scan
                // no need to block, since if we are reading data, we've parsed the scans already
                SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                if (refNode == null) {
                    // generate error item and throw exception
                    throw new AtlasLoaderException("Could not find " + refName + " [" + refNodeName + "] in SDRF");
                }

                // collect all the possible 'assay' forming nodes

                Collection<AssayNode> assayNodes = SDRFUtils.findUpstreamNodes(
                        refNode, AssayNode.class);

                // now check we have 1:1 mappings so that we can resolve our scans
                if (assayNodes.size() == 1) {
                    SDRFNode assayNode = assayNodes.iterator().next();
                    log.debug("Scan node " + refNodeName + " resolves to " +
                            assayNode.getNodeName());

                    assay = cache.fetchAssay(assayNode.getNodeName());
                } else {
                    // many to one scan-to-assay, we can't load this
                    // generate error item and throw exception
                    throw new AtlasLoaderException(
                            "Unable to update resolve expression values to assays for " +
                                    investigation.accession + " - data matrix file references scans, " +
                                    "and in this experiment scans do not map one to one with assays.  " +
                                    "This is not supported, as it would result in " +
                                    (assayNodes.size() == 0 ? "zero" : "multiple") + " expression " +
                                    "values per assay."
                    );
                }
            } else if (refNodeName.equals("assayname") || refNodeName.equals("hybridizationname")) {
                // just check it is possible to recover the SDRF node referenced in the data file
                SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                if (refNode == null) {
                    // generate error item and throw exception
                    throw new AtlasLoaderException("Could not find " + refName + " [" + refNodeName + "] in SDRF");
                }

                // refNode is not null, meaning we recovered this assay - it's safe to wait for it
                assay = cache.fetchAssay(refNode.getNodeName());
            } else {
                assay = null;
            }

            if (assay != null) {
                log.trace("Updating assay " + assay.getAccession() + " with expression values, " +
                        "must be stored first...");
                cache.setAssayDataMatrixRef(assay, buffer.getStorage(), refIndex);
                cache.setDesignElements(assay.getArrayDesignAccession(), buffer.getDesignElements());
            } else {
                // generate error item and throw exception
                throw new AtlasLoaderException("Data file references elements that are not present in the SDRF (" + refNodeName + ", " + refName + ")");
            }
        }


    }

    private String runPipeline(File inFilePath, File outFilePath) {

//        final URL sdrfURL = investigation.SDRF.getLocation();
//        final File expDir = new File(sdrfURL.getFile()).getParentFile();

        final String inFile = inFilePath.getAbsolutePath();
        final String outFile = outFilePath.getAbsolutePath();

        DataNormalizer dataNormalizer = new DataNormalizer(inFile, outFile);
        computeService.computeTask(dataNormalizer);

        return outFile;
    }

    private static class DataNormalizer implements ComputeTask<Void> {

        public final String infname;
        public final String outfname;


        private DataNormalizer(String inputFile, String outputFile) {
            this.infname = inputFile;
            this.outfname = outputFile;
        }

        public Void compute(RServices R) throws RemoteException {

            R.sourceFromBuffer("infname = '" + infname + "'");
            R.sourceFromBuffer("outfname = '" + outfname + "'");
            R.sourceFromBuffer(getRCodeFromResource("R/htsProcessPipeline.R"));
            R.sourceFromBuffer("esetToTextFile(infname = infname, outfname = outfname)");

            return null;
        }

        // TODO: copy-pasted from atlas-analitics; should be extracted to an utility function

        private String getRCodeFromResource(String resourcePath) throws ComputeException {
            // open a stream to the resource
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

            // create a reader to read in code
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuilder sb = new StringBuilder();
            String line;

            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new ComputeException("Error while reading in R code from " + resourcePath, e);
            } finally {
                if (null != in) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        
                    }
                }
            }

            return sb.toString();
        }
    }
}

