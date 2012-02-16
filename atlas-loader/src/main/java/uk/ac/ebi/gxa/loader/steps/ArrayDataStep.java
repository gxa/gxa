/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.loader.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.utils.GraphUtils;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.*;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ArrayDesignAttribute;
import uk.ac.ebi.gxa.R.compute.AtlasComputeService;
import uk.ac.ebi.gxa.R.compute.ComputeTask;
import uk.ac.ebi.gxa.R.compute.RUtil;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RChar;
import uk.ac.ebi.rcloud.server.RType.RObject;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.deleteDirectory;

/**
 * Experiment loading step that prepares data matrix to be stored in data files.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */


public class ArrayDataStep {
    private final static Logger log = LoggerFactory.getLogger(ArrayDataStep.class);

    private static final String USE_PROCCESSED_FILES = "; Please try using processed experimental data instead";

    public static String displayName() {
        return "Processing data matrix";
    }

    private static class RawData {
        final File dataDir;
        final HashMap<String, String> celFiles = new HashMap<String, String>();
        final HashMap<String, Assay> assays = new HashMap<String, Assay>();

        RawData() {
            dataDir = FileUtil.createTempDirectory("atlas-loader");
        }
    }

    private static final Object COPY_FILE_LOCK = new Object();

    private static void copyFile(URL from, File to) throws IOException {
        synchronized (COPY_FILE_LOCK) {
            URLConnection connection = null;
            InputStream is = null;
            OutputStream os = null;
            try {
                connection = from.openConnection();
                is = connection.getInputStream();
                os = new FileOutputStream(to);
                copy(is, os);
            } finally {
                closeQuietly(os);
                closeQuietly(is);
                if (connection != null && connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }
    }

    private static final Object EXTRACT_ZIP_LOCK = new Object();

    private static void extractZip(File zipFile, File dir) throws IOException {
        synchronized (EXTRACT_ZIP_LOCK) {
            ZipInputStream zipInputStream = null;
            try {
                zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    final String entryName = zipEntry.getName();
                    FileOutputStream fileOutputStream = null;

                    try {
                        fileOutputStream = new FileOutputStream(new File(dir, entryName));
                        copy(zipInputStream, fileOutputStream);
                    } finally {
                        closeQuietly(fileOutputStream);
                        zipInputStream.closeEntry();
                    }
                    zipEntry = zipInputStream.getNextEntry();
                }
            } finally {
                closeQuietly(zipInputStream);
            }
        }
    }

    public boolean readArrayData(@Nonnull AtlasComputeService computeService, MAGETABInvestigation investigation, AtlasLoaderServiceListener listener, AtlasLoadCache cache, LoaderDAO dao) throws AtlasLoaderException {
        final URL sdrfURL = investigation.SDRF.getLocation();
        final File sdrfDir = new File(sdrfURL.getFile()).getParentFile();
        final HashMap<String, RawData> dataByArrayDesign = new HashMap<String, RawData>();
        final HashMap<String, File> zipFiles = new HashMap<String, File>();

        try {
            // set this variable to false to avoid attempts of load
            // CEL files from the same location as IDF/SDRF files;
            // ftp link will be used
            // set this variable to true to try local files firstly
            boolean useLocalCopy = true;
            final Collection<ArrayDataNode> dataNodes =
                investigation.SDRF.getNodes(ArrayDataNode.class);
            if (dataNodes.isEmpty()) {
                log.warn("No data nodes for raw data are defined in " + sdrfURL);
                // the experiment loading logic will use the processed files instead
                return false;
            }

            listener.setProgress("Loading CEL files");
            for (ArrayDataNode node : dataNodes) {
                log.info("Found array data matrix node '" + node.getNodeName() + "'");

                final Collection<HybridizationNode> hybridizationNodes = GraphUtils.findUpstreamNodes(node, HybridizationNode.class);
                final Collection<AssayNode> assayNodes = GraphUtils.findUpstreamNodes(node, AssayNode.class);
                if (hybridizationNodes.size() + assayNodes.size() != 1) {
                    throw new AtlasLoaderException("ArrayDataNode " + node.getNodeName() + " corresponds to " + (hybridizationNodes.size() + assayNodes.size()) + " assays");
                }
                final HybridizationNode assayNode =
                        hybridizationNodes.size() == 0 ? assayNodes.iterator().next() : hybridizationNodes.iterator().next();
                Assay assay = cache.fetchAssay(assayNode.getNodeName());
                if (assay == null) {
                    throw new AtlasLoaderException("Cannot fetch an assay for node " + assayNode.getNodeName());
                }

                final Collection<ScanNode> scanNodes = GraphUtils.findUpstreamNodes(node, ScanNode.class);
                if (scanNodes.size() > 1) {
                    throw new AtlasLoaderException("ArrayDataNode " + node.getNodeName() + " corresponds to " + scanNodes.size() + " scans");
                }
                final ScanNode scanNode = scanNodes.size() == 1 ? scanNodes.iterator().next() : null;
                final List<ArrayDesignAttribute> arrayDesigns = assayNode.arrayDesigns;
                if (arrayDesigns.size() != 1) {
                    throw new AtlasLoaderException("Assay node " + assayNode.getNodeName() + " has " + arrayDesigns.size() + " array designs");
                }

                final String arrayDesignName = arrayDesigns.get(0).getNodeName();
                final String dataFileName = node.getNodeName();
                final String scanName = scanNode != null ? scanNode.getNodeName() : assayNode.getNodeName();

                // TODO: use better way to check this if such way exists
                if (!arrayDesignName.toLowerCase().contains("affy") && !dao.isArrayDesignSynonym(arrayDesignName)) {
                    log.warn("Array design " + arrayDesignName + " is not an Affymetrix or a synonym of an existing array design");
                    // For non-Affymetrics chip we don't throw and exception but allow the experiment loading logic
                    // to silently move to using the processed files instead.
                    return false;
                }

                if (dataFileName == null || dataFileName.length() == 0) {
                    continue;
                }

                RawData adData = dataByArrayDesign.get(arrayDesignName);
                if (adData == null) {
                    adData = new RawData();
                    dataByArrayDesign.put(arrayDesignName, adData);
                }
                if (adData.celFiles.get(dataFileName) != null)
                    throw new AtlasLoaderException("Error processing file: '" + dataFileName + "' - this file is used twice" + USE_PROCCESSED_FILES);
                adData.celFiles.put(dataFileName, scanName);
                adData.assays.put(dataFileName, assay);

                final File tempFile = new File(adData.dataDir, dataFileName);

                if (useLocalCopy) {
                    final File localFile = new File(sdrfDir, dataFileName);
                    URL localFileURL;
                    try {
                        localFileURL = sdrfURL.getPort() == -1
                                ? new URL(sdrfURL.getProtocol(),
                                sdrfURL.getHost(),
                                localFile.toString().replaceAll("\\\\", "/"))
                                : new URL(sdrfURL.getProtocol(),
                                sdrfURL.getHost(),
                                sdrfURL.getPort(),
                                localFile.toString().replaceAll("\\\\", "/"));
                        copyFile(localFileURL, tempFile);
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (!tempFile.exists() && node.comments != null) {
                    useLocalCopy = false;
                    final String zipName = DataUtils.fixZipURL(node.comments.get("ArrayExpress FTP file"));
                    if (zipName != null) {
                        File localZipFile = zipFiles.get(zipName);
                        if (localZipFile == null) {
                            try {
                                localZipFile = File.createTempFile("atlas-loader", ".zip");
                                zipFiles.put(zipName, localZipFile);
                                copyFile(new URL(zipName), localZipFile);
                            } catch (IOException e) {
                                if (localZipFile != null && !localZipFile.delete()) {
                                    log.error("Cannot delete " + localZipFile.getAbsolutePath());
                                }
                                throw new AtlasLoaderException("Error occurred while retrieving raw data files from ArrayExpress ftp site" + USE_PROCCESSED_FILES, e);
                            }
                        }
                        try {
                            extractZip(localZipFile, adData.dataDir);
                        } catch (IOException e) {
                            throw new AtlasLoaderException("Error occurred while retrieving raw data files from ArrayExpress ftp site" + USE_PROCCESSED_FILES, e);
                        }
                    }
                }
                if (!tempFile.exists()) {
                    throw new AtlasLoaderException("Error occurred while processing raw data files: File '" + dataFileName + "' is not found" + USE_PROCCESSED_FILES);
                }
            }

            listener.setProgress("Processing data in R");
            for (Map.Entry<String, RawData> entry : dataByArrayDesign.entrySet()) {
                final DataNormalizer normalizer = new DataNormalizer(entry.getValue());
                // this method returns null if computation was finished successfully
                // or an instance of "try-error" R class in case of failure
                // currently we receive instances of "try-error" as RChar objects
                final RObject result = computeService.computeTask(normalizer);
                if (result != null) {
                    throw new AtlasLoaderException(
                            "Something unexpected happened during R processing; returned " +
                            (result instanceof RChar
                                    ? ((RChar) result).getValue()[0]
                                    : result));
                }
                try {
                    final File mergedFile = new File(normalizer.mergedFilePath);
                    final DataMatrixFileBuffer buffer = cache.getDataMatrixFileBuffer(mergedFile.toURL(), null);
                    final HashMap<String, Assay> assayMap = entry.getValue().assays;
                    final ArrayList<String> fileNames = normalizer.fileNames;
                    for (int i = 0; i < fileNames.size(); ++i) {
                        final Assay assay = assayMap.get(fileNames.get(i));
                        cache.setAssayDataMatrixRef(assay, buffer.getStorage(), i);
                    }
                    if (!mergedFile.delete()) {
                        log.warn("Cannot delete" + mergedFile.getAbsolutePath());
                    }
                } catch (MalformedURLException e) {
                    throw LogUtil.createUnexpected("MalformedURLException is thrown: " + e.getMessage());
                }
            }

            return true;
        } finally {
            for (RawData data : dataByArrayDesign.values()) {
                deleteDirectory(data.dataDir);
            }
            for (File z : zipFiles.values()) {
                if (!z.delete()) {
                    log.warn("Cannot delete " + z.getAbsolutePath());
                }
            }
        }
    }

    private static class DataNormalizer implements ComputeTask<RObject> {
        private final RawData data;
        public final ArrayList<String> fileNames = new ArrayList<String>();
        public final String pathPrefix;
        public final String mergedFilePath;

        public DataNormalizer(RawData data) {
            this.data = data;
            pathPrefix = data.dataDir.getAbsolutePath() + "/";
            mergedFilePath = pathPrefix + "merged.txt";
        }

        public RObject compute(RServices R) throws RemoteException {
            StringBuilder files = new StringBuilder();
            StringBuilder scans = new StringBuilder();
            files.append("files = c(");
            scans.append("scans = c(");
            boolean isFirst = true;
            for (Map.Entry<String, String> entry : data.celFiles.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    files.append(", ");
                    scans.append(", ");
                }
                fileNames.add(entry.getKey());
                files.append("'");
                files.append(pathPrefix);
                files.append(entry.getKey());
                files.append("'");
                scans.append("'");
                scans.append(entry.getValue());
                scans.append("'");
            }
            files.append(")");
            scans.append(")");
            log.info(files.toString());
            log.info(scans.toString());
            log.info("outFile = '" + mergedFilePath + "'");
            R.sourceFromBuffer(files.toString());
            R.sourceFromBuffer(scans.toString());
            R.sourceFromBuffer("outFile = '" + mergedFilePath + "'");
            R.sourceFromBuffer(RUtil.getRCodeFromResource("R/normalizeOneExperiment.R"));
            final RObject result = R.getObject("normalizeOneExperiment(files = files, outFile = outFile, scans = scans, parallel = FALSE)");
            R.sourceFromBuffer("rm(outFile)");
            R.sourceFromBuffer("rm(scans)");
            R.sourceFromBuffer("rm(files)");
            R.sourceFromBuffer(RUtil.getRCodeFromResource("R/cleanupNamespace.R"));
            return result;
        }
    }
}
