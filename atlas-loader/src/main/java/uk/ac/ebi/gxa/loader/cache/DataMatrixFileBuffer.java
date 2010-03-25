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

package uk.ac.ebi.gxa.loader.cache;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.loader.utils.QuantitationTypeDictionary;
import uk.ac.ebi.gxa.utils.FlattenIterator;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * A class that can be used to buffer data read from a MAGE-TAB Derived Array Data Matrix format file.
 *
 * @author Tony Burdett
 * @date 03-Sep-2009
 */
public class DataMatrixFileBuffer {

    private URL dataMatrixURL;
    private String fileName;
    private String referenceColumnName;

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
    private String[] referenceNames;

    private LinkedList<DataMatrixBlock> dataBlocks = new LinkedList<DataMatrixBlock>();

    private static Logger log = LoggerFactory.getLogger(DataMatrixFileBuffer.class);

    public DataMatrixFileBuffer(URL dataMatrixURL, String fileName) throws ParseException {
        this.dataMatrixURL = dataMatrixURL;
        this.fileName = fileName;

        init();
    }

    public DataMatrixFileBuffer(URL dataMatrixURL) throws ParseException {
        this.dataMatrixURL = dataMatrixURL;
        this.fileName = null;

        init();
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

    /**
     * Returns an array of references observed in this data file.  This should normally match exactly to the set of
     * hybs/assays/scans described in the SDRF file.
     *
     * @return the 'references' in this data file, which should correspond to the e.g. hybridization name in the SDRF
     */
    public String[] getReferences() {
        return referenceNames;
    }

    public Iterable<DataMatrixBlock> getDataBlocks() {
        return dataBlocks;
    }

    public Iterable<String> getDesignElements() {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new FlattenIterator<DataMatrixBlock, String>(dataBlocks.iterator()) {
                    public Iterator<String> inner(DataMatrixBlock dataMatrixBlock) {
                        return Arrays.asList(dataMatrixBlock.designElements).subList(0, dataMatrixBlock.size()).iterator();
                    }
                };
            }
        };
    }

    private InputStream openStream() throws IOException {
        if(fileName == null)
            return dataMatrixURL.openStream();

        // HACK: fix bad ArrayExpress URLs like
        // ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/TABM/E-TABM-733/TABM/E-TABM-733/E-TABM-733.processed.1.zip
        String strDataMatrixURL = dataMatrixURL.toExternalForm();
        Pattern badArrayExpressURLPattern =
                Pattern.compile("ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/(.*)/(.*)/\\1/\\2/\\2\\.(.*zip)");

        Matcher m = badArrayExpressURLPattern.matcher(strDataMatrixURL);
        if(m.matches()) {
            strDataMatrixURL = "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/" +
                    m.group(1) + "/" + m.group(2) + "/" + m.group(2) + "." + m.group(3);

            dataMatrixURL = new URL(strDataMatrixURL);
        }

        ZipInputStream zistream = new ZipInputStream(new BufferedInputStream(dataMatrixURL.openStream()));
        ZipEntry zi;
        while((zi = zistream.getNextEntry()) != null) {
            if(zi.getName().toLowerCase().endsWith(fileName.toLowerCase())) {
                return zistream;
            }
            zistream.closeEntry();
        }
        zistream.close();
        throw new FileNotFoundException("Can't find file " + fileName + " in archive " + dataMatrixURL);
    }

    private void init() throws ParseException {
        BufferedReader reader = null;
        try {
            // create a buffered reader
            reader = new BufferedReader(new InputStreamReader(openStream()));

            // parse the headers
            Header[] headers;
            try {
                headers = parseHeaders(reader);
            }
            catch (ParseException e) {
                // this occurs if the dataMatrixFile is badly formatted, set the file and rethrow
                e.getErrorItem().setParsedFile(dataMatrixURL.toString());
                throw e;
            }

            // now, iterate over headers, doing dictionary lookup for qtTypes and setting the known reference names
            QuantitationTypeDictionary dictionary = QuantitationTypeDictionary.getQTDictionary();
            referenceNames = new String[headers.length];
            int referenceIndex = 0;
            for (Header header : headers) {
                // store next refName
                log.trace("Storing reference for " + header.assayRef);
                referenceNames[referenceIndex] = header.assayRef;
                referenceIndex++;


                // locate the right QT for this data file
                List<String> possibleTypes = new ArrayList<String>();
                Collection<String> allTypes = header.getQuantitationTypes();
                for (String qtType : allTypes) {
                    log.trace("Checking type (" + qtType + ") against dictionary " +
                            "for " + header.assayRef);
                    if (dictionary.lookupTerm(qtType)) {
                        possibleTypes.add(qtType);
                    }
                }

                // more than one possible type or not possible and more than one total
                if (possibleTypes.size() > 1 || (possibleTypes.isEmpty() && allTypes.size() > 1)) {
                    StringBuffer sb = new StringBuffer();

                    if(possibleTypes.size() > 1) {
                        sb.append("Possible types: [");
                    for (String pt : possibleTypes) {
                        sb.append(pt).append(", ");
                    }
                    sb.append("]");
                    }

                    if(allTypes.size() > 1) {
                        sb.append("All types: [");
                         for (String at : allTypes) {
                            sb.append(at).append(", ");
                        }
                        sb.append("]");
                    }

                    String message =
                            "Unable to load - data matrix file contains " + possibleTypes.size() + " " +
                                    "recognised candidate quantitation types out of " + allTypes.size() + " total " +
                                    " to use for expression values.\n" +
                                    "Ambiguity over which QT type should be used, from: " + sb.toString();
                    ErrorItem error =
                            ErrorItemFactory
                                    .getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(
                                            message,
                                            601,
                                            this.getClass());

                    throw new ParseException(error, true);
                }
                else if (allTypes.isEmpty()) {
                    // zero possible types - dump dictionary to logs
                    StringBuffer sb = new StringBuffer();
                    sb.append("QuantitationTypeDictionary: [");
                    for (String term : dictionary.listQTTypes()) {
                        sb.append(term).append(", ");
                    }
                    sb.append("]");
                    log.error("No matching terms: " + sb.toString());

                    String message =
                            "Unable to load - data matrix file contains 0 " +
                                    "recognised candidate quantitation types to use for " +
                                    "expression values";
                    ErrorItem error =
                            ErrorItemFactory
                                    .getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(
                                            message,
                                            601,
                                            this.getClass());

                    throw new ParseException(error, true);
                }

                // Use either possible (only one) or absolutely one qt type
                String qtType = possibleTypes.isEmpty() ? allTypes.iterator().next() : possibleTypes.iterator().next();
                log.trace("Using " + qtType + " for expression values");
                refToEVColumn.put(header.assayRef,
                        header.getIndexOfQuantitationType(
                                qtType));

            }

            // now we've sorted out our headers and the ref columns

            // read all the data into the buffer...
            readFileIntoBuffer(reader);
        }
        catch (ParseException e) {
            throw e;
        }
        catch (Throwable e) {
            // generate error item and throw exception
            String message =
                    "An error occurred whilst attempting to read from the " +
                            "derived array data matrix file at " + dataMatrixURL;
            ErrorItem error =
                    ErrorItemFactory
                            .getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    1023,
                                    this.getClass());

            throw new ParseException(error, true, e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
       }
    }

    /**
     * Reads the entire contents of a data matrix file into memory, utlisiing a two dimension float array for the data.
     * The float[][] "expressionValues" should have been pre-initialised with AT LEAST enough elements to hold all the
     * data.  If the array is too small, an index out of bound exception will occur.  Note that it is not critical that
     * the array is exactly the correct size - empty elements will be trimmed off the end once parsing has completed.
     *
     * @param reader
     * @return
     * @throws ParseException
     */
    private void readFileIntoBuffer(BufferedReader reader) throws ParseException {
        try {
            log.info("Reading data matrix from " + dataMatrixURL + "...");

            // read data - track the design element index in order to store axis info
            String line;
            while ((line = reader.readLine()) != null) {
                // ignore empty lines
                if (!line.trim().equals("")) {
                    if (!line.startsWith("#")) {
                        final String[] tokens = line.split("\t");

                        if (tokens.length > 0) {
                            // ignore header lines
                            String tag = MAGETABUtils.digestHeader(tokens[0]);
                            if (tag.equals("hybridizationref") ||
                                    tag.equals("assayref") ||
                                    tag.equals("hybridizationref") ||
                                    tag.equals("scanref") ||
                                    tag.equals("reporterref") ||
                                    tag.equals("compositeelementref") ||
                                    tag.startsWith("termsourceref:") ||
                                    tag.startsWith("coordinatesref:")) {
                                // this is header, so skip this line
                                log.debug("Skipping line, looks like a header [" + line + "]");
                            }
                            else {
                                DataMatrixBlock block;
                                if(dataBlocks.isEmpty()) {
                                    block = new DataMatrixBlock(10000, referenceNames.length);
                                    dataBlocks.add(block);
                                } else {
                                    block = dataBlocks.getLast();
                                    if(block.size() == block.capacity()) {
                                        block = new DataMatrixBlock(1000, referenceNames.length);
                                        dataBlocks.add(block);
                                    }
                                }

                                int position = block.size++;
                                block.designElements[position] = tokens[0];
                                for(int i = 0; i < referenceNames.length; ++i) {
                                    try {
                                        block.expressionValues[position * referenceNames.length + i]
                                                = Float.parseFloat(tokens[refToEVColumn.get(referenceNames[i])]);
                                    } catch(NumberFormatException e) {
                                        block.expressionValues[position * referenceNames.length + i] =  -1000000f;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            // generate error item and throw exception
            String message =
                    "An error occurred whilst attempting to read from the " +
                            "derived array data matrix file at " + dataMatrixURL;
            ErrorItem error =
                    ErrorItemFactory
                            .getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    1023,
                                    this.getClass());

            log.error("Error", e);
            throw new ParseException(error, true, e);
        }
        finally {
            try {
                log.info("Finished reading from " + dataMatrixURL + (fileName != null ? ":" + fileName : "") + ", closing");
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
                // ignore
            }
        }
    }

    private Header[] parseHeaders(BufferedReader reader)
            throws IOException, ParseException {
        // grab the first two header lines
        String line;
        List<String> lines = new ArrayList<String>();
        while (lines.size() < 2 && (line = reader.readLine()) != null) {
            // ignore empty lines
            if (!line.trim().equals("")) {
                if (!line.startsWith("#")) {
                    // reformat and unescape lines
                    String firstLine = line;
                    while (MAGETABUtils.endsWithEscapedNewline(firstLine)) {
                        String secondLine = reader.readLine();
                        line = MAGETABUtils.compensateForEscapedNewlines(firstLine, secondLine);
                        firstLine = secondLine;
                    }

                    // removing escaping
                    line = MAGETABUtils.unescapeLine(line);
                    // and add to the list of headers
                    lines.add(line);
                }
            }
        }

        log.debug("Headers parsing, read first two non-comment, non-empty lines");

        // tokenise the first linee
        String[] valRefs = lines.get(0).split("\t");
        String[] qtTypes = lines.get(1).split("\t");

        // do some integrity checking before parsing
        // check they have the same number of tokens
        if (valRefs.length != qtTypes.length) {
            // this file looks wrong, so generate error item and throw exception
            String message =
                    "Failed to parse the derived array data matrix file - there were " +
                            "different numbers of hybridization references to quantitation " +
                            "types, this must be a one-to-one binding";
            ErrorItem error =
                    ErrorItemFactory
                            .getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    1023,
                                    this.getClass());

            throw new ParseException(error, true);
        }
        // check first column of valRefs refers to a real SDRF column -
        // should be hybridizationref, assayref, or scanref
        String refName = MAGETABUtils.digestHeader(valRefs[0]);
        if (!refName.startsWith("hybridizationref")
                && !refName.startsWith("assayref")
                && !refName.startsWith("scanref")) {
            // this file looks wrong, so generate error item and throw exception
            String message =
                    "Failed to parse the derived array data matrix file - the " +
                            "first line started with '" + refName + "' when one of " +
                            "'hybridizationref', 'assayref' or 'scanref' was expected";
            ErrorItem error =
                    ErrorItemFactory
                            .getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    1023,
                                    this.getClass());

            throw new ParseException(error, true);
        }
        else {
            referenceColumnName = refName.replace("ref", "name");
            log.debug("Reference column set to '" + referenceColumnName + "'");
        }

        // check our list of headers has size = 2
        if (lines.size() != 2) {
            // generate error item and throw exception
            String message =
                    "Failed to parse the derived array data matrix file - the header " +
                            "lines were badly formatted, could not read the first two " +
                            "lines as expected";
            ErrorItem error =
                    ErrorItemFactory
                            .getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    1023,
                                    this.getClass());

            throw new ParseException(error, true);
        }

        log.debug("Integrity checks passed, two header lines with expected " +
                "start points and equal number of tokens parsed");

        // passed checks, so now parse fully
        List<Header> headers = new ArrayList<Header>();
        Header header = null;
        for (int column = 1; column < valRefs.length; column++) {
            // grab the normalized values
            String hybRef = valRefs[column];
            String qtType = MAGETABUtils.digestHeader(qtTypes[column]);

            // also, only care about the last bit (ignore 'namespacey' type crap
            qtType = qtType.substring(qtType.lastIndexOf(":") + 1, qtType.length());

            // new header needed?
            if (header == null || !header.getAssayRef().equals(hybRef)) {
                // i.e. first token, or the hybRef is the same as the last token so use same header
                log.trace("Found header binding to " + hybRef);
                header = new Header();
                header.setAssayRef(hybRef);
                headers.add(header);
            }

            // add qtTypes
            log.trace("Adding quantitation type '" + qtType + "' to header '" +
                    hybRef + "', index=" + column);
            header.addQTType(qtType, column);
        }

        // now our list of headers is fully populated, so return it
        return headers.toArray(new Header[headers.size()]);
    }

    private class Header {
        private String assayRef;
        private Map<String, Integer> columnIndexByQTType;

        public Header() {
            columnIndexByQTType = new HashMap<String, Integer>();
        }

        public String getAssayRef() {
            return assayRef;
        }

        public void setAssayRef(String assayRef) {
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
            StringBuffer sb = new StringBuffer();
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
