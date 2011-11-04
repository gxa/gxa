package uk.ac.ebi.gxa.loader.steps;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.compute.RUtil;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HTSArrayDataStep {
    private final static Logger log = LoggerFactory.getLogger(HTSArrayDataStep.class);

    private static final String RDATA = "eset_notstd_rpkm.RData";

    public static String displayName() {
        return "Processing HTS data";
    }

    public void readHTSData(MAGETABInvestigation investigation, AtlasComputeService computeService, AtlasLoadCache cache, LoaderDAO dao) throws AtlasLoaderException {
        log.info("Starting HTS data load");

        // sdrf location
        URL sdrfURL = investigation.SDRF.getLocation();


        //run files through the pipeline
        File outFilePath = runPipeline(sdrfURL, computeService);


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
                Assay assay = cache.fetchAssay(enaRunName);

                if (assay != null) {
                    log.trace("Updating assay {} with expression values, must be stored first...", assay);
                    cache.setAssayDataMatrixRef(assay, buffer.getStorage(), refIndex);
                    if (assay.getArrayDesign() == null) {
                        assay.setArrayDesign(dao.getArrayDesignShallow(findArrayDesignName(refNode)));
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

    private static URL convertPathToULR(URL sdrfURL, File outFilePath) throws AtlasLoaderException {
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

    private static File runPipeline(URL sdrfURL, AtlasComputeService computeService) throws AtlasLoaderException {

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

        File outFilePath = new File(FileUtil.getTempDirectory(), "out.txt");
        log.debug("Output file " + outFilePath);

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
            log.info(e.getMessage(), e);
            //this exception can be ignored
        }
        if (!fileExists) {
            throw new AtlasLoaderException("File " + outFilePath + " hasn't been created");
        }

        return outFilePath;
    }

    //ToDo: this is only temp solution! Array design will not be user for RNA-seq experiments
    private static String findArrayDesignName(SDRFNode node) {
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
            rs.sourceFromBuffer(RUtil.getRCodeFromResource("R/htsProcessPipeline.R"));
            rs.sourceFromBuffer("esetToTextFile(infname = infname, outfname = outfname)");

            return null;
        }
    }
}

