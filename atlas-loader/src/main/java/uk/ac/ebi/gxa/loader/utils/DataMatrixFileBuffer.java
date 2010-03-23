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

package uk.ac.ebi.gxa.loader.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * A class that can be used to buffer data read from a MAGE-TAB Derived Array Data Matrix format file.  This is
 * accomplished by use of a factory method to generate a single DataMatrixFileBuffer object per-file.  On construction,
 * the buffer is initialized - this starts a process that runs in a new thread, parsing the file for headers and doing a
 * dictionary lookup for quantitation types in the file. Once initialization has completed, data can be quickly and
 * easily read out of the file by calling the {@link #readExpressionValues(String[])}  method, passing in the id of the
 * referenced value (usually an assay, but sometimes the associated scan) you wish to read (which should be obtained
 * from the SDRF file, and binds to particular columns in the data matrix file).  You can call {@link
 * #readExpressionValues(String[])} immediately once your bufer object is returned, but this method blocks until
 * initialization has completed.
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
    private Map<String, Integer> refToEVColumn;

    /**
     * An array of reference names - "references" will normally be assays, hybs or scans depending on the type
     * declaration in this file.  This array has the same ordering as arrays in the data matrix file, but only stores
     * unique values.
     */
    private String[] referenceNames;
    /**
     * An array of design element names.  This array has the same ordering as design elements in the data matrix file
     * this object buffers.
     */
    private String[] designElementNames;
    /**
     * A 2D array of floats.  The outer array orders elements corresponding to the reference column names, whereas the
     * inner array orders values corresponding to design elements.  Only relevant values are stored, so the outer
     * 'reference-indexed' array does not have the same number of columns as the data matrix file
     */
    private float[][] expressionValues;

    private Log log = LogFactory.getLog(this.getClass());

    public DataMatrixFileBuffer(URL dataMatrixURL, String fileName) throws ParseException {
        this.dataMatrixURL = dataMatrixURL;
        this.refToEVColumn = new HashMap<String, Integer>();
        this.fileName = fileName;

        init();
    }

    public DataMatrixFileBuffer(URL dataMatrixURL) throws ParseException {
        this.dataMatrixURL = dataMatrixURL;
        this.refToEVColumn = new HashMap<String, Integer>();
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
    public String readReferenceColumnName() {
        return referenceColumnName;
    }

    /**
     * Returns an array of references observed in this data file.  This should normally match exactly to the set of
     * hybs/assays/scans described in the SDRF file.
     *
     * @return the 'references' in this data file, which should correspond to the e.g. hybridization name in the SDRF
     */
    public String[] readReferences() {
        return referenceNames;
    }

    /**
     * Returns an array of design element names observed in the data file.  This ordering exactly matches the ordering
     * of returned expression values, so that for each expression value.
     * <p/>
     * Design element names are lazily instantiated during reading, so if the data matrix file hasn't been read once
     * already the result may be null
     *
     * @return the design element names listed in this file
     */
    public String[] readDesignElements() {
        return designElementNames;
    }

    /**
     * Read off expression values for the column reference.  The reference will normally be a hyb, assay or scan column
     * from the SDRF file.  This buffer object automatically knows which columns must be read, as a dictionary lookup on
     * column names was performed on initialization.  You can configure the dictionary of terms to use manually - for
     * more on this see {@link QuantitationTypeDictionary}.  This method blocks until initialization has completed, and
     * this buffer knows which columns to read to obtain expression values.
     * <p/>
     * The result of this operation is a two-dimensional array of floats.  This 2D array has defined ordering - the
     * outer array is ordered by the design elements in this file.  You can map to these by calling {@link
     * #readDesignElements()} - this returns an array of string representing the design element names, with the same
     * ordering as the outer array returned.  The inner array of floats has the same ordering as the references passed
     * as a parameter to this method.
     *
     * @param references the references of the assays you wish to find expression values for
     * @return an array of float expression values, with specific ordering
     */
    public float[][] readExpressionValues(String... references) {
        // buffer was initialized, so grab results from expression values buffer

        // result is an array of float arrays, sized by the number of references passed
        float[][] results = new float[references.length][designElementNames.length];

        // loop over indices to match ref names
        for (int resultIndex = 0; resultIndex < references.length; resultIndex++) {
            String reference = references[resultIndex];
            // get index of this reference
            for (int bufferRefIndex = 0; bufferRefIndex < referenceNames.length; bufferRefIndex++) {
                if (referenceNames[bufferRefIndex].equals(reference) && expressionValues[bufferRefIndex] != null) {
                    // update our result with the values that are already buffered
                    results[resultIndex] = expressionValues[bufferRefIndex];
                    break;
                }
            }
        }

        return results;
    }

    public void clear() {
    }

    private InputStream openStream() throws IOException {
        if(fileName == null)
            return dataMatrixURL.openStream();

        ZipInputStream zistream = new ZipInputStream(new BufferedInputStream(dataMatrixURL.openStream()));
        ZipEntry zi;
        while((zi = zistream.getNextEntry()) != null) {
            if(zi.getName().toLowerCase().endsWith(fileName.toLowerCase())) {
                return zistream;
            }
        }
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
                    sb.append("[");
                    for (String pt : possibleTypes) {
                        sb.append(pt).append(", ");
                    }
                    sb.append("]");

                    String message =
                            "Unable to load - data matrix file contains " + possibleTypes.size() + " " +
                                    "recognised candidate quantitation types to use for " +
                                    "expression values.\n" +
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
                refToEVColumn.put(header.assayRef,
                        header.getIndexOfQuantitationType(
                                qtType));

            }

            // now we've sorted out our headers and the ref columns

            // do a quick read to count the number of lines in the file, so we can initialize other arrays
            int lineCount = countNumberOfLinesInFile();

            // initialize arrays using this count - it may be a few too big, but we can trim later
            designElementNames = new String[lineCount];
            expressionValues = new float[referenceNames.length][designElementNames.length];

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

    private int countNumberOfLinesInFile() throws IOException {
        LineNumberReader reader = null;
        try {
            // create reader to count lines
            reader = new LineNumberReader(new InputStreamReader(openStream()));
            while (reader.readLine() != null) {
                // simply loops to last line of the file
            }
            // and returns line number
            return reader.getLineNumber();
        }
        finally {
            if (reader != null) {
                reader.close();
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
            int deIndex = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                // ignore empty lines
                if (!line.trim().equals("")) {
                    if (!line.startsWith("#")) {
                        String[] tokens = line.split("\t");

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
                                // this isn't a header - read values out
                                designElementNames[deIndex] = tokens[0];

                                // now, read out the expression values
                                for (int refIndex = 0; refIndex < referenceNames.length; refIndex++) {
                                    String reference = referenceNames[refIndex];

                                    // get the float value from the next ref column
                                    try {
                                        float ev = Float.parseFloat(tokens[refToEVColumn.get(reference)]);
                                        // and set the float at the appropriate coordinate
                                        expressionValues[refIndex][deIndex] = ev;
                                    } catch(NumberFormatException e) {
                                        expressionValues[refIndex][deIndex] = -1000000;
                                    }
                                }


                                // and increment our design element count
                                deIndex++;
                            }
                        }
                    }
                }
            }

            // we've now read all the data
            // truncate the design element names and expression value arrays to the correct size
            designElementNames = resizeArray(designElementNames, deIndex);
            for (int i = 0; i < expressionValues.length; i++) {
                expressionValues[i] = resizeArray(expressionValues[i], deIndex);
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
                log.info("Finished reading from " + dataMatrixURL + ", closing");
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
                // ignore
            }
        }
    }

    private String[] resizeArray(String[] array, int size) {
        String[] newarray = new String[size];
        System.arraycopy(array, 0, newarray, 0, Math.min(size, array.length));
        return newarray;
    }

    private float[] resizeArray(float[] array, int size) {
        float[] newarray = new float[size];
        System.arraycopy(array, 0, newarray, 0, Math.min(size, array.length));
        return newarray;
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
