package uk.ac.ebi.gxa.loader.steps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

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
                log.info("Exit HTSArrayDataStep. No comment[ENA_RUN] found.");
                return;
            }
        }

        // sdrf location
        URL sdrfURL = investigation.SDRF.getLocation();


        //run files through the pipeline
        File outFilePath = runPipeline(sdrfURL);


        // try to get the relative filename
        URL dataMatrixURL = convertPathToULR(sdrfURL, outFilePath);

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
//                SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                SDRFNode refNode = investigation.SDRF.lookupScanNodeWithComment("ENA_RUN", refName);
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


        outFilePath.delete();
        outFilePath.getParentFile().delete();
    }

    private URL convertPathToULR(URL sdrfURL, File outFilePath) throws AtlasLoaderException {
        URL dataMatrixURL;// NB. making sure we replace File separators with '/' to guard against windows issues
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
            // generate error item and throw exception
            throw new AtlasLoaderException(
                    "Cannot formulate the URL to retrieve the " +
                            "DerivedArrayDataMatrix," +
                            " this file could not be found relative to " + sdrfURL);
        }


        // now, obtain a buffer for this dataMatrixFile
        log.debug("Opening buffer of data matrix file at " + dataMatrixURL);
        return dataMatrixURL;
    }

    private File runPipeline(URL sdrfURL) throws AtlasLoaderException {

        //ToDo: this code will be removed once a whole pipeline is integrated
        // The  directory structure is like that:
        // EXP_ACC
        // -data
        // -- *.idf
        // -- *.sdrf
        // -esetcount.RData

        File sdrfFilePath = new File(sdrfURL.getFile());
        File inFilePath = new File(sdrfFilePath.getParentFile().getParentFile(), "esetcount.RData");

        if (!inFilePath.exists()) {
            //Try to look for RData in the same directory as sdrf file
            inFilePath = new File(sdrfFilePath.getParentFile(), "esetcount.RData");

            if (!inFilePath.exists()) {
                throw new AtlasLoaderException("File with R object (esetcount.RData) is not found niether in " +
                sdrfFilePath.getParentFile() + " nor in " + sdrfFilePath.getParentFile().getParentFile() + " directories.");
            }
        }

        File outFilePath = new File(createTempDir(), "out.txt");

        final String inFile = inFilePath.getAbsolutePath();
        final String outFile = outFilePath.getAbsolutePath();

        DataNormalizer dataNormalizer = new DataNormalizer(inFile, outFile);
        computeService.computeTask(dataNormalizer);

        if (!outFilePath.exists()) {
            throw new AtlasLoaderException("File " + outFilePath + " hasn't been created");
        }

        return outFilePath;
    }

    private final File createTempDir() throws AtlasLoaderException {
        try {
            //final File dir = File.createTempFile("atlas-loader", ".dat", new File("/nfs/ma/home/geometer-tmp"));
            final File dir = File.createTempFile("atlas-loader", ".dat");
            dir.delete();
            if (!dir.mkdir()) {
                throw new AtlasLoaderException("Couldn't create directory \"" + dir.getAbsolutePath() + "\"");
            }
            return dir;
        } catch (IOException e) {
            throw new AtlasLoaderException(e);
        }
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

