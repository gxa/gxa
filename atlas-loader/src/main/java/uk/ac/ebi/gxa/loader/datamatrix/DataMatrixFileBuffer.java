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

package uk.ac.ebi.gxa.loader.datamatrix;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * A class that can be used to buffer data read from a MAGE-TAB Derived Array Data Matrix format file.
 *
 * @author Tony Burdett
 */
public class DataMatrixFileBuffer {

    private URL dataMatrixURL;
    private String fileName;
    private String referenceColumnName;
    private Collection<String> possibleQTypes;

    /**
     * References the target name (normally hyb/assay/sacn) to the column in the data matrix file we need to read
     * expression values for (so the correct quantitation type).  This is only used in parsing - don't use it to look up
     * the array index from expression values!
     */
    private Map<String, Integer> refToEVColumn = new HashMap<String, Integer>();

    /**
     * An array of reference names - "references" will normally be assays, hybs or scans depending on the type
     * declaration in this file.  This array has the same ordering as arrays in the data matrix file, but only stores
     * unique values.
     */
    private final List<String> referenceNames = new ArrayList<String>();

    private DataMatrixStorage storage;

    private List<String> designElements = new ArrayList<String>();

    private static Logger log = LoggerFactory.getLogger(DataMatrixFileBuffer.class);

    private boolean hasQtTypes = true;

    public DataMatrixFileBuffer(URL dataMatrixURL, String fileName, Collection<String> possibleQTypes) throws AtlasLoaderException {
        this(dataMatrixURL, fileName, possibleQTypes, true);
    }

    public DataMatrixFileBuffer(URL dataMatrixURL, String fileName, Collection<String> possibleQTypes, boolean hasQtTypes) throws AtlasLoaderException {
        this.dataMatrixURL = dataMatrixURL;
        this.fileName = fileName;
        this.possibleQTypes = possibleQTypes;
        this.hasQtTypes = hasQtTypes;
        init();
    }

    public List<String> getReferences() {
        return Collections.unmodifiableList(referenceNames);
    }

    /**
     * Returns the reference column name for this data matrix file.  This is the left most string value on the first
     * line, and references the column name in the SDRF graph for which all the subsequent values are taken from.  This
     * will usually be "hybridization name", "assay name" or "scan name"
     *
     * @return the reference column name
     */
    public String getReferenceColumnName() {
        return referenceColumnName;
    }

    public DataMatrixStorage getStorage() {
        return storage;
    }

    public List<String> getDesignElements() {
        return designElements;
    }

    private InputStream openStream() throws IOException {
        if (fileName == null)
            return dataMatrixURL.openStream();

        // HACK: fix bad ArrayExpress URLs like
        // ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/TABM/E-TABM-733/TABM/E-TABM-733/E-TABM-733.processed.1.zip
        String strDataMatrixURL = dataMatrixURL.toExternalForm();
        Pattern badArrayExpressURLPattern =
                Pattern.compile("ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/(.*)/(.*)/\\1/\\2/\\2\\.(.*zip)");

        Matcher m = badArrayExpressURLPattern.matcher(strDataMatrixURL);
        if (m.matches()) {
            strDataMatrixURL = "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/" +
                    m.group(1) + "/" + m.group(2) + "/" + m.group(2) + "." + m.group(3);

            dataMatrixURL = new URL(strDataMatrixURL);
        }

        // TODO: review resource handling here, possible leaks
        ZipInputStream zistream = new ZipInputStream(new BufferedInputStream(dataMatrixURL.openStream()));
        ZipEntry zi;
        while ((zi = zistream.getNextEntry()) != null) {
            if (zi.getName().toLowerCase().endsWith(fileName.toLowerCase())) {
                return zistream;
            }
            zistream.closeEntry();
        }
        zistream.close();
        throw new FileNotFoundException("Can't find file " + fileName + " in archive " + dataMatrixURL);
    }

    private void init() throws AtlasLoaderException {
        CSVReader csvReader = null;
        try {
            // create a buffered reader
            csvReader = new CSVReader(new InputStreamReader(openStream()), '\t', '"');

            // parse the headers
            List<Header> headers = parseHeaders(csvReader);

            int colNum = 1;
            // now, iterate over headers, doing dictionary lookup for qtTypes and setting the known reference names
            for (Header header : headers) {
                // store next refName
                log.trace("Storing reference for " + header.assayRef);
                referenceNames.add(header.assayRef);

                // locate the right QT for this data file
                if (hasQtTypes) {
                    List<String> possibleTypes = new ArrayList<String>();
                    Collection<String> allTypes = header.getQuantitationTypes();
                    for (String qtType : allTypes) {
                        log.trace("Checking type (" + qtType + ") against dictionary " +
                                "for " + header.assayRef);
                        if (possibleQTypes != null && possibleQTypes.contains(qtType)) {
                            possibleTypes.add(qtType);
                        }
                    }

                    // more than one possible type or not possible and more than one total
                    if (possibleTypes.size() > 1 || (possibleTypes.isEmpty() && allTypes.size() > 1)) {
                        StringBuilder sb = new StringBuilder();

                        if (possibleTypes.size() > 1) {
                            sb.append("Possible types: [");
                            sb.append(StringUtils.join(possibleTypes, ","));
                            sb.append("]");
                        }

                        if (allTypes.size() > 1) {
                            sb.append("All types: [");
                            sb.append(StringUtils.join(allTypes, ","));
                            sb.append("]");
                        }

                        throw new AtlasLoaderException(
                                "Unable to load - data matrix file contains " + possibleTypes.size() +
                                        " recognised candidate quantitation types out of " + allTypes.size() +
                                        " total to use for expression values.\n" +
                                        "Ambiguity over which QT type should be used, from: " + sb.toString()
                        );
                    } else if (allTypes.isEmpty()) {
                        log.error("No matching terms: " + StringUtils.join(possibleQTypes, ","));
                        throw new AtlasLoaderException(
                                "Unable to load - data matrix file contains 0 " +
                                        "recognised candidate quantitation types to use for " +
                                        "expression values"
                        );
                    }

                    // Use either possible (only one) or absolutely one qt type
                    String qtType = possibleTypes.isEmpty() ? allTypes.iterator().next() : possibleTypes.iterator().next();
                    log.trace("Using " + qtType + " for expression values");
                    refToEVColumn.put(header.assayRef, header.getIndexOfQuantitationType(qtType));
                } else {
                    refToEVColumn.put(header.assayRef, colNum++);
                }
            }

            // now we've sorted out our headers and the ref columns

            // read all the data into the buffer...
            readFileIntoBuffer(csvReader);
        } catch (IOException e) {
            throw new AtlasLoaderException(
                    "An error occurred whilst attempting to read from the " +
                            "derived array data matrix file at " + dataMatrixURL);
        } finally {
            closeQuietly(csvReader);
        }
    }

    /**
     * Reads the entire contents of a data matrix file into memory, utlisiing a two dimension float array for the data.
     * The float[][] "expressionValues" should have been pre-initialised with AT LEAST enough elements to hold all the
     * data.  If the array is too small, an index out of bound exception will occur.  Note that it is not critical that
     * the array is exactly the correct size - empty elements will be trimmed off the end once parsing has completed.
     *
     * @param csvReader
     * @return
     * @throws AtlasLoaderException
     */
    private void readFileIntoBuffer(CSVReader csvReader) throws AtlasLoaderException {
        try {
            log.info("Reading data matrix from " + dataMatrixURL + "...");

            storage = new DataMatrixStorage(referenceNames.size(), 10000, 1000);

            // read data - track the design element index in order to store axis info
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                // ignore empty lines & lines with comments
                if (line.length == 0 || line[0].startsWith("#")) {
                    continue;
                }
                // ignore header lines
                String tag = MAGETABUtils.digestHeader(line[0]);
                if (tag.equals("hybridizationref") ||
                        tag.equals("assayref") ||
                        tag.equals("scanref") ||
                        tag.equals("reporterref") ||
                        tag.equals("compositeelementref") ||
                        tag.startsWith("termsourceref:") ||
                        tag.startsWith("coordinatesref:")) {
                    // this is header, so skip this line
                    StringBuilder builder = new StringBuilder();
                    for (String token : line) {
                        builder.append(token);
                        builder.append('\t');
                    }
                    log.debug("Skipping line, looks like a header [" + builder + "]");
                    continue;
                }

                String de = new String(line[0].toCharArray());
                storage.add(de, refToEVColumn, referenceNames, line);
                designElements.add(de);
            }
        } catch (IOException e) {
            // generate error item and throw exception
            throw new AtlasLoaderException(
                    "An error occurred whilst attempting to read from the " +
                            "derived array data matrix file at " + dataMatrixURL
            );
        } finally {
            log.info("Finished reading from " + dataMatrixURL + (fileName != null ? ":" + fileName : "") + ", closing");
            closeQuietly(csvReader);
        }
    }

    private String[] getHeaderLine(CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line;
        do {
            line = csvReader.readNext();
            if (line == null) {
                throw new AtlasLoaderException(
                        "Failed to parse the derived array data matrix file - the header " +
                                "lines were badly formatted, could not read the first two " +
                                "lines as expected"
                );
            }
        } while (line.length == 0 || line[0].startsWith("#"));
        return line;
    }

    private List<Header> parseHeaders(CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] valRefs = getHeaderLine(csvReader);
        String[] qtTypes = null;

        //In a case of HTS data quantitation types are not present
        if (hasQtTypes) {
            qtTypes = getHeaderLine(csvReader);
        }
        log.debug("Headers parsing, read first two non-comment, non-empty lines");

        // do some integrity checking before parsing
        // check they have the same number of tokens
        if (qtTypes != null && valRefs.length != qtTypes.length) {
            // this file looks wrong, so generate error item and throw exception
            throw new AtlasLoaderException(
                    "Failed to parse the derived array data matrix file - there were " +
                            "different numbers of hybridization references to quantitation " +
                            "types, this must be a one-to-one binding"
            );
        }
        // check first column of valRefs refers to a real SDRF column -
        // should be hybridizationref, assayref, or scanref
        String refName = MAGETABUtils.digestHeader(valRefs[0]);
        if (!refName.startsWith("hybridizationref")
                && !refName.startsWith("assayref")
                && !refName.startsWith("scanref")) {
            // this file looks wrong, so generate error item and throw exception
            throw new AtlasLoaderException(
                    "Failed to parse the derived array data matrix file - the " +
                            "first line started with '" + refName + "' when one of " +
                            "'hybridizationref', 'assayref' or 'scanref' was expected"
            );
        } else {
            referenceColumnName = refName.replace("ref", "name");
            log.debug("Reference column set to '" + referenceColumnName + "'");
        }

        log.debug("Integrity checks passed, two header lines with expected " +
                "start points and equal number of tokens parsed");

        // passed checks, so now parse fully
        List<Header> headers = new ArrayList<Header>();
        Header header = null;
        for (int column = 1; column < valRefs.length; column++) {
            // grab the normalized values
            String hybRef = valRefs[column];

            // new header needed?
            if (header == null || !header.assayRef.equals(hybRef)) {
                // i.e. first token, or the hybRef is the same as the last token so use same header
                log.trace("Found header binding to " + hybRef);
                header = new Header(hybRef);
                headers.add(header);
            }

            if (hasQtTypes && qtTypes != null) {
                String qtType = MAGETABUtils.digestHeader(qtTypes[column]);

                // also, only care about the last bit (ignore 'namespacey' type crap
                qtType = qtType.substring(qtType.lastIndexOf(":") + 1, qtType.length());

                // add qtTypes
                log.trace("Adding quantitation type '" + qtType + "' to header '" +
                        hybRef + "', index=" + column);
                header.addQTType(qtType, column);
            }
        }

        // now our list of headers is fully populated, so return it
        return headers;
    }

    private static class Header {
        private Map<String, Integer> columnIndexByQTType;
        private final String assayRef;

        public Header(String assayRef) {
            columnIndexByQTType = new HashMap<String, Integer>();
            this.assayRef = assayRef;
        }

        public void addQTType(String qtTypeName, int columnIndex) {
            columnIndexByQTType.put(qtTypeName, columnIndex);
        }

        public Collection<String> getQuantitationTypes() {
            return columnIndexByQTType.keySet();
        }

        public int getIndexOfQuantitationType(String quantitationType) {
            return columnIndexByQTType.get(quantitationType);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Header{" + "assayRef='").append(assayRef)
                    .append('\'' + ", quantitationTypes=[");
            for (String qtType : columnIndexByQTType.keySet()) {
                sb.append("type='").append(qtType).append("'(");
                sb.append(columnIndexByQTType.get(qtType)).append("), ");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
