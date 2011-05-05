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

import com.google.common.io.Resources;
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
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.rcloud.server.RServices;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.deleteDirectory;

/**
 * Experiment loading step that prepares data matrix to be stored into a NetCDF file.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */


public class ArrayDataStep implements Step {
    static final Object SUCCESS_KEY = new Object();

    private final AtlasMAGETABLoader loader;
    private final MAGETABInvestigationExt investigation;
    private final AtlasLoaderServiceListener listener;
    private final AtlasLoadCache cache;
    private final Log log = LogFactory.getLog(this.getClass());

    public ArrayDataStep(AtlasMAGETABLoader loader, MAGETABInvestigationExt investigation, AtlasLoaderServiceListener listener) {
        this.loader = loader;
        this.investigation = investigation;
        this.listener = listener;
        this.cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
    }

    public String displayName() {
        return "Processing data matrix";
    }

    private static class RawData {
        final File dataDir;
        final HashMap<String,String> celFiles = new HashMap<String,String>();
        final HashMap<String,Assay> assays = new HashMap<String,Assay>();

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

    public void run() throws AtlasLoaderException {
        final URL sdrfURL = investigation.SDRF.getLocation();
        final File sdrfDir = new File(sdrfURL.getFile()).getParentFile();
        final HashMap<String,RawData> dataByArrayDesign =  new HashMap<String,RawData>();
        final HashMap<String,File> zipFiles =  new HashMap<String,File>();

        try {
            boolean useLocalCopy = false;
            listener.setProgress("Loading CEL files");
            for (ArrayDataNode node : investigation.SDRF.lookupNodes(ArrayDataNode.class)) {
                log.info("Found array data matrix node '" + node.getNodeName() + "'");

                final Collection<HybridizationNode> hybridizationNodes = SDRFUtils.findUpstreamNodes(node, HybridizationNode.class);
                final Collection<AssayNode> assayNodes = SDRFUtils.findUpstreamNodes(node, AssayNode.class);
                if (hybridizationNodes.size() + assayNodes.size() != 1) {
                    throw new AtlasLoaderException("ArrayDataNode " + node.getNodeName() + " corresponds to " + (hybridizationNodes.size() + assayNodes.size()) + " assays");
                }
                final HybridizationNode assayNode =
                    hybridizationNodes.size() == 0 ? assayNodes.iterator().next() : hybridizationNodes.iterator().next();
                Assay assay = cache.fetchAssay(assayNode.getNodeName());
                if (assay == null) {
                    throw new AtlasLoaderException("Cannot fetch an assay for node " + assayNode.getNodeName());
                }

                final Collection<ScanNode> scanNodes = SDRFUtils.findUpstreamNodes(node, ScanNode.class);
                if (scanNodes.size() > 1) {
                    throw new AtlasLoaderException("ArrayDataNode " + node.getNodeName() + " corresponds to " + scanNodes.size() + " scans");
                }
                final ScanNode scanNode = scanNodes.size() == 1 ? scanNodes.iterator().next() : null;
                final List<ArrayDesignNode> arrayDesigns = assayNode.arrayDesigns;
                if (arrayDesigns.size() != 1) {
                    throw new AtlasLoaderException("Assay node " + assayNode.getNodeName() + " has " + arrayDesigns.size() + " array designs");
                }

                final String arrayDesignName = arrayDesigns.get(0).getNodeName();
                final String dataFileName = node.getNodeName();
                final String scanName = scanNode != null ? scanNode.getNodeName() : assayNode.getNodeName();

                // We check if this sample is made on Affymetrics chip
                // TODO: use better way to check this if such way exists
                if (arrayDesignName.toLowerCase().indexOf("affy") == -1) {
                    throw new AtlasLoaderException("Array design " + arrayDesignName + " is not an Affymetrics");
                }

                if (dataFileName == null || dataFileName.length() == 0) {
                    continue;
                }

                RawData adData = dataByArrayDesign.get(arrayDesignName);
                if (adData == null) {
                    adData = new RawData();
                    dataByArrayDesign.put(arrayDesignName, adData);
                }
                if (adData.celFiles.get(dataFileName) != null) {
                    throw new AtlasLoaderException("File '" + dataFileName + "' is used twice");
                }
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
                                throw new AtlasLoaderException(e);
                            }
                        }
                        try {
                            extractZip(localZipFile, adData.dataDir);
                            //ZipFile zip = new ZipFile(localZipFile);
                            //copyFile(zip.getInputStream(zip.getEntry(dataFileName)), tempFile);
                        } catch (IOException e) {
                            throw new AtlasLoaderException(e);
                        }
                    }
                }
                if (!tempFile.exists()) {
                    throw new AtlasLoaderException("File '" + dataFileName + "' is not found");
                }
            }

            listener.setProgress("Acquiring R service");
            final AtlasComputeService computeService = loader.getComputeService();
            if (computeService == null) {
                throw new AtlasLoaderException("Cannot create a compute service");
            }

            listener.setProgress("Processing data in R");
            for (Map.Entry<String,RawData> entry : dataByArrayDesign.entrySet()) {
                log.info("ArrayDesign " + entry.getKey() + ":");
                log.info("directory " + entry.getValue().dataDir);

                DataNormalizer normalizer = new DataNormalizer(entry.getValue());
                computeService.computeTask(normalizer);
                try {
                    final File mergedFile = new File(normalizer.mergedFilePath);
                    DataMatrixFileBuffer buffer = cache.getDataMatrixFileBuffer(mergedFile.toURL(), null);
                    final HashMap<String,Assay> assayMap = entry.getValue().assays;
                    final ArrayList<String> fileNames = normalizer.fileNames;
                    for (int i = 0; i < fileNames.size(); ++i) {
                        Assay assay = assayMap.get(fileNames.get(i));
                        cache.setAssayDataMatrixRef(assay, buffer.getStorage(), i);
                        cache.setDesignElements(assay.getArrayDesign().getAccession(), buffer.getDesignElements());
                    }
                    if (!mergedFile.delete())
                        log.warn("Cannot delete" + mergedFile.getAbsolutePath());
                } catch (MalformedURLException e) {
                    throw new AtlasLoaderException(e.getMessage());
                }

                for (String name : entry.getValue().celFiles.keySet()) {
                    log.info("  file " + name);
                }
            }

            investigation.userData.put(SUCCESS_KEY, SUCCESS_KEY);
        } finally {
            for (RawData data : dataByArrayDesign.values()) {
                deleteDirectory(data.dataDir);
            }
            for (File z : zipFiles.values()) {
                if (!z.delete())
                    log.warn("Cannot delete " + z.getAbsolutePath());
            }
        }
    }

    private static class DataNormalizer implements ComputeTask<Void> {
        private final RawData data;
        public final ArrayList<String> fileNames = new ArrayList<String>();
        public final String pathPrefix;
        public final String mergedFilePath;

        public DataNormalizer(RawData data) {
            this.data = data;
            pathPrefix = data.dataDir.getAbsolutePath() + "/";
            mergedFilePath = pathPrefix + "merged.txt";
        }

        public Void compute(RServices R) throws RemoteException {
            StringBuilder files = new StringBuilder();
            StringBuilder scans = new StringBuilder();
            files.append("files = c(");
            scans.append("scans = c(");
            boolean isFirst = true;
            for (Map.Entry<String,String> entry : data.celFiles.entrySet()) {
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
            R.sourceFromBuffer(files.toString());
            R.sourceFromBuffer(scans.toString());
            R.sourceFromBuffer("outFile = '" + mergedFilePath + "'");
            R.sourceFromBuffer(getRCodeFromResource("R/normalizeOneExperiment.R"));
            R.sourceFromBuffer("normalizeOneExperiment(files = files, outFile = outFile, scans = scans, parallel = FALSE)");
            R.sourceFromBuffer("rm(outFile)");
            R.sourceFromBuffer("rm(scans)");
            R.sourceFromBuffer("rm(files)");
            R.sourceFromBuffer(getRCodeFromResource("R/cleanupNamespace.R"));
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
