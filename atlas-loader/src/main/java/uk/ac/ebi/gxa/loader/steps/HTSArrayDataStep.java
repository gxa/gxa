package uk.ac.ebi.gxa.loader.steps;

import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: nsklyar
 * Date: Oct 4, 2010
 */
public class HTSArrayDataStep implements Step {

    private static final String RDATA = "eset_notstd_rpkm.RData";
    //    private static final String RDATA = "esetcount.RData";
    private final MAGETABInvestigationExt investigation;
    private final AtlasLoadCache cache;
    private final AtlasComputeService computeService;

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

        // check that data is from RNASeq (comments: "Comment [ENA_RUN]"	"Comment [FASTQ_URI]" must be present)
        //ToDo: add this check in the Loader
        Collection<ScanNode> scanNodes = investigation.SDRF.lookupNodes(ScanNode.class);
        if (scanNodes.size() == 0) {
            log.info("Exit HTSArrayDataStep. No comment scan nodes found.");
            return;
        }
        for (ScanNode scanNode : scanNodes) {
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
            Assay assay = null;
            if (refNodeName.equals("scanname")) {
                // this requires mapping the assay upstream of this node to the scan
                // no need to block, since if we are reading data, we've parsed the scans already
//                SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                ScanNode refNode = lookupScanNodeWithComment(investigation.SDRF, "ENA_RUN", refName);
                if (refNode == null) {
                    // generate error item and throw exception
                    throw new AtlasLoaderException("Could not find " + refName + " [" + refNodeName + "] in SDRF");
                }

                String enaRunName = refNode.comments.get("ENA_RUN");
                assay = cache.fetchAssay(enaRunName);


                if (assay != null) {
                    log.trace("Updating assay " + assay.getAccession() + " with expression values, " +
                            "must be stored first...");
                    cache.setAssayDataMatrixRef(assay, buffer.getStorage(), refIndex);
                    cache.setDesignElements(assay.getArrayDesignAccession(), buffer.getDesignElements());
                    if (StringUtils.isEmpty(assay.getArrayDesignAccession())) {
                        assay.setArrayDesignAccession(findArrayDesignName(refNode));
                    }
                } else {
                    // generate error item and throw exception
                    throw new AtlasLoaderException("Data file references elements that are not present in the SDRF (" + refNodeName + ", " + refName + ")");
                }
            }
        }

        if (!outFilePath.delete()) {
            log.error("Temp file " + outFilePath + " wasn't deleted!");
        }
    }

    private static ScanNode lookupScanNodeWithComment(SDRF sdrf, String commentType, String commentName) {
        Collection<? extends SDRFNode> nodes = sdrf.lookupNodes(MAGETABUtils.digestHeader("scanname"));
        for (SDRFNode node : nodes) {
            ScanNode scanNode = (ScanNode) node;
            Map<String, String> comments = scanNode.comments;
            String commentValue = comments.get(commentType);
            if (commentValue != null && commentValue.equals(commentName)) {
                return scanNode;
            }
        }
        // if we get to here, either we have no node of this type or none with the same name
        return null;
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
        File inFilePath = new File(sdrfFilePath.getParentFile().getParentFile(), RDATA);

        if (!inFilePath.exists()) {
            //Try to look for RData in the same directory as sdrf file
            inFilePath = new File(sdrfFilePath.getParentFile(), RDATA);

            if (!inFilePath.exists()) {
                throw new AtlasLoaderException("File with R object (" + RDATA + ") is not found neither in " +
                        sdrfFilePath.getParentFile() + " nor in " + sdrfFilePath.getParentFile().getParentFile() + " directories.");
            }
        }

//        File outFilePath = new File(createTempDir(), "out.txt");
        File outFilePath = new File(FileUtil.getTempDirectory(), "out.txt");

        log.debug("Output file " + outFilePath);

        if (!outFilePath.setWritable(true, false)) {
            log.error("File " + outFilePath + " cannot be set to writable!");
            throw new AtlasLoaderException("Cannot write into file " + outFilePath + " which is need to keep temp data from R pipeline.");
        }

        final String inFile = inFilePath.getAbsolutePath();
        final String outFile = outFilePath.getAbsolutePath();

        RRunner rRunner = new RRunner(inFile, outFile);
        computeService.computeTask(rRunner);

        //Sometimes R finishes writing file with a delay
        boolean fileExists = false;
        try {
            for (int i = 0; i < 100; i++) {
                Thread.sleep(2000);
                if (outFilePath.exists()) {
                    fileExists = true;
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info(e);
            //this exception can be ignored
        }
        if (!fileExists) {
            throw new AtlasLoaderException("File " + outFilePath + " hasn't been created");
        }


        return outFilePath;
    }

    //ToDo: this is only temp solution! Array design will not be user for RNA-seq experiments
    private String findArrayDesignName(SDRFNode node) {
        Collection<SourceNode> nodeCollection = SDRFUtils.findUpstreamNodes(node, SourceNode.class);
        for (SourceNode sourceNode : nodeCollection) {
            for (CharacteristicsAttribute characteristic : sourceNode.characteristics) {
                if ("Organism".equals(characteristic.type)) {
                    if ("Homo sapiens".equalsIgnoreCase(characteristic.getNodeName())) {
                        return "A-ENST-3";
                    } else if ("Mus musculus".equalsIgnoreCase(characteristic.getNodeName())) {
                        return "A-ENST-4";
                    } else if ("drosophila melanogaster".equalsIgnoreCase(characteristic.getNodeName())) {
                        return "A-ENST-5";
                    }
                }

            }
        }
        return StringUtils.EMPTY;
    }

    private static class RRunner implements ComputeTask<Void> {
        public final String infname;
        public final String outfname;


        private RRunner(String inputFile, String outputFile) {
            this.infname = inputFile;
            this.outfname = outputFile;
        }

        public Void compute(RServices rs) throws RemoteException {

            rs.sourceFromBuffer("infname = '" + infname + "'");
            rs.sourceFromBuffer("outfname = '" + outfname + "'");
            rs.sourceFromBuffer(getRCodeFromResource("R/htsProcessPipeline.R"));
            rs.sourceFromBuffer("esetToTextFile(infname = infname, outfname = outfname)");

            return null;
        }

        // TODO: copy-pasted from atlas-analitics; should be extracted to an utility function
        private String getRCodeFromResource(String resourcePath) throws ComputeException {
            try {
                return Resources.toString(getClass().getClassLoader().getResource(resourcePath), Charset.defaultCharset());
            } catch (IOException e) {
                throw new ComputeException("Error while reading in R code from " + resourcePath, e);
            }
        }
    }
}

