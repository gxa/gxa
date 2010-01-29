package uk.ac.ebi.gxa.loader.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * A class that can be used to buffer data read from a MAGE-TAB Derived Array Data Matrix format file.  This is
 * accomplished by use of a factory method to generate a single DataMatrixFileBuffer object per-file.  On construction,
 * the buffer is initialized - this starts a process that runs in a new thread, parsing the file for headers and doing a
 * dictionary lookup for quantitation types in the file. Once initialization has completed, data can be quickly and
 * easily read out of the file by calling the {@link #readExpressionValues(String...)} method, passing in the id of the
 * assay you wish to read (which should be obtained from the SDRF file, and binds to particular columns in the data
 * matrix file).  You can call {@link #readExpressionValues(String...)} immediately once your bufer object is returned,
 * but this method blocks until initialization has completed.
 *
 * @author Tony Burdett
 * @date 03-Sep-2009
 */
public class DataMatrixFileBuffer {
    private static Map<URL, DataMatrixFileBuffer> buffers =
            new HashMap<URL, DataMatrixFileBuffer>();

    /**
     * Generate a DataMatrixFileBuffer object for the given data matrix file URL.
     *
     * @param dataMatrixFile the URL of the file you wish to buffer
     * @return an object that can buffer data from the given file
     */
    public static DataMatrixFileBuffer getDataMatrixFileBuffer(
            URL dataMatrixFile) {
        if (buffers.containsKey(dataMatrixFile)) {
            // reuse
            return buffers.get(dataMatrixFile);
        }
        else {
            // create a new buffer
            DataMatrixFileBuffer buffer = new DataMatrixFileBuffer(dataMatrixFile);

            // initialize
            buffer.init();

            // insert into map
            buffers.put(dataMatrixFile, buffer);

            // and return
            return buffer;
        }
    }

    private URL dataMatrixURL;
    private String referenceColumnName;
    private Map<String, Integer> refToEVColumn;
    private Map<String, Map<String, Float>> refToEVs;

    private boolean ready = false;
    private ParseException initFailed = null;

    private Log log = LogFactory.getLog(this.getClass());

    private DataMatrixFileBuffer(URL dataMatrixURL) {
        this.dataMatrixURL = dataMatrixURL;
        this.refToEVColumn = new HashMap<String, Integer>();
        this.refToEVs = new HashMap<String, Map<String, Float>>();
    }

    /**
     * Returns the reference column name for this data matrix file.  This is the left most string value on the first
     * line, and references the column name in the SDRF graph for which all the subsequent values are taken from.  This
     * will usually be "hybridization name", "assay name" or "scan name"
     *
     * @return the reference column name
     */
    public String readReferenceColumnName() {
        // block until ready
        synchronized (this) {
            while (!ready && initFailed == null) {
                try {
                    log.debug("Blocking whilst buffer initializes...");
                    wait();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        return referenceColumnName;
    }

    /**
     * Returns the list of references observed in this data file.  This should normally match exactly to the set of
     * hybs/assays/scans described in the SDRF file.
     *
     * @return the 'references' in this data file, which should correspond to the e.g. hybridization name in the SDRF
     */
    public Set<String> readReferences() {
        // block until ready
        synchronized (this) {
            while (!ready && initFailed == null) {
                try {
                    log.debug("Blocking whilst buffer initializes...");
                    wait();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        return refToEVColumn.keySet();
    }

    /**
     * Read off expression values for the column reference.  The reference will normally be a hyb, assay or scan column
     * from the SDRF file.  This buffer object automatically knows which columns must be read, as a dictionary lookup on
     * column names was performed on initialization.  You can configure the dictionary of terms to use manually - for
     * more on this see {@link QuantitationTypeDictionary}.  This method blocks until initialization has completed, and
     * this buffer knows which columns to read to obtain expression values.
     *
     * @param references the references of the assays you wish to find expression values for
     * @return a map of expression values read, indexed by assay ref
     * @throws ParseException if the file could not be parsed, either at initialization or when reading expression
     *                        values
     */
    public Map<String, Map<String, Float>> readExpressionValues(
            String... references)
            throws ParseException {
        // block until ready
        synchronized (this) {
            while (!ready && initFailed == null) {
                try {
                    log.debug("Blocking whilst buffer initializes...");
                    wait();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        // argh, complex mapping - this maps the assay accession to the map of design element/expression value mappings
        Map<String, Map<String, Float>> result = new HashMap<String, Map<String, Float>>();

        // if initFailed is not null, failed to init so throw
        if (initFailed != null) {
            throw initFailed;
        }

        // if we've read these expression values before
        Set<String> bufferedAssays = new HashSet<String>();
        for (String assayRef : references) {
            if (refToEVs.containsKey(assayRef)) {
                result.put(assayRef, refToEVs.get(assayRef));
                bufferedAssays.add(assayRef);
            }
            else {
                // cached map contains no result for this assay, create new list
                refToEVs.put(assayRef, new HashMap<String, Float>());
                // and create list for results
                result.put(assayRef, new HashMap<String, Float>());
            }
        }

        // do we need to actually read the file now?
        if (bufferedAssays.size() == references.length) {
            // we've got all the results we need
            return result;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));

            // now, we have a map of assay names to expression value columns...
            // so read every line of the file, parsing the columns we need
            log.info("Reading data matrix from " + dataMatrixURL + "...");
            String line;

            // read line by line - but if assay refs are missing, we don't want to warn every time
            Set<String> missingAssayRefColumns = new HashSet<String>();

            // fields we need to set to update the data matrix representation
            String designElement;
            Float evFloatValue;

            // NB this uses same reader we used to parse headers, so just continue reading
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                // ignore empty lines
                if (!line.trim().equals("")) {
                    if (!line.startsWith("#")) {
                        String[] tokens = line.split("\t");
                        designElement = tokens[0];

                        // ignore header lines
                        String maybeHeader = MAGETABUtils.digestHeader(designElement);
                        if (maybeHeader.equals("hybridizationref") ||
                                maybeHeader.equals("assayref") ||
                                maybeHeader.equals("hybridizationref") ||
                                maybeHeader.equals("scanref") ||
                                maybeHeader.equals("reporterref") ||
                                maybeHeader.equals("compositeelementref") ||
                                maybeHeader.startsWith("termsourceref:") ||
                                maybeHeader.startsWith("coordinatesref:")) {
                            // this is header, so skip this line
                            log.debug("Skipping line, looks like a header [" + line + "]");
                        }
                        else {
                            // not a header line, so read out expression values
                            for (String assayRef : references) {
                                // only look for this value if we've not got it cached
                                if (!bufferedAssays.contains(assayRef)) {
                                    // read all expression values for this line
                                    if (refToEVColumn.get(assayRef) == null) {
                                        // we have a missing expression value - is the whole column missing?
                                        if (refToEVColumn.get(assayRef) == null) {
                                            // just warn the first time
                                            if (!missingAssayRefColumns.contains(assayRef)) {
                                                missingAssayRefColumns.add(assayRef);
                                                log.warn(new StringBuffer()
                                                        .append("Missing column in data file: no reference to assay ")
                                                        .append(assayRef)
                                                        .append(" could be found").toString());
                                            }
                                        }
                                        else {
                                            // warn each time, as the column is present but just this value is missing
                                            log.warn(new StringBuffer()
                                                    .append("No expression values present for ")
                                                    .append(assayRef)
                                                    .append(" in data matrix file at line: ")
                                                    .append(lineCount)
                                                    .append(", column: ")
                                                    .append(refToEVColumn.get(assayRef)).toString());
                                        }
                                    }
                                    else {
                                        evFloatValue = Float.parseFloat(tokens[refToEVColumn.get(assayRef)]);
                                        // finished reading, store in buffer...
                                        refToEVs.get(assayRef).put(designElement, evFloatValue);
                                        // and now add to result map
                                        result.get(assayRef).put(designElement, evFloatValue);
                                        // reset to null to be sure we don't reuse it accidentally
                                        evFloatValue = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return result;
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

            throw new ParseException(error, true);
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

    private void init() {
        if (!ready && initFailed == null) {
            // start thread to initialise reading things as this hasn't been initialised before
            new Thread(new Runnable() {
                public void run() {
                    BufferedReader reader = null;
                    try {
                        // create a buffered reader
                        reader = new BufferedReader(new InputStreamReader(dataMatrixURL.openStream()));

                        // parse the headers
                        Header[] headers;
                        try {
                            headers = parseHeaders(reader);
                        }
                        catch (ParseException e) {
                            // this occurs if the dataMatrixFile is badly formatted, set the file and rethrow
                            e.getErrorItem().setParsedFile(dataMatrixURL.toString());

                            initFailed = e;
                            return;
                        }

                        // now, iterate over headers, doing dictionary lookup for qtTypes
                        QuantitationTypeDictionary dictionary = QuantitationTypeDictionary.getQTDictionary();
                        for (Header header : headers) {
                            List<String> possibleTypes = new ArrayList<String>();
                            for (String qtType : header.getQuantitationTypes()) {
                                log.trace("Checking type (" + qtType + ") against dictionary " +
                                        "for " + header.assayRef);
                                if (dictionary.lookupTerm(qtType)) {
                                    possibleTypes.add(qtType);
                                    if (!refToEVColumn.containsKey(header.assayRef)) {
                                        log.trace("Term " + qtType +
                                                " is in dictionary, inserting column " +
                                                header.getIndexOfQuantitationType(qtType) +
                                                " into map for " + header.assayRef);
                                        refToEVColumn.put(header.assayRef,
                                                          header.getIndexOfQuantitationType(
                                                                  qtType));
                                    }
                                }
                            }

                            // more than one possible type
                            if (possibleTypes.size() > 1) {
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

                                initFailed = new ParseException(error, true);
                                initFailed.printStackTrace();
                                return;
                            }
                            if (possibleTypes.size() == 0) {
                                // dump dictionary to logs
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

                                initFailed = new ParseException(error, true);
                                initFailed.printStackTrace();
                                return;

                            }
                        }

                        // now we've mapped assay names to expression value columns,
                        // we're ready to read as required
                        ready = true;
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

                        initFailed = new ParseException(error, true);
                        e.printStackTrace();
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

                        synchronized (DataMatrixFileBuffer.this) {
                            DataMatrixFileBuffer.this.notifyAll();
                        }
                    }
                }
            }).start();
        }
    }


    private Header[] parseHeaders(BufferedReader reader)
            throws IOException, ParseException {
        // grab the first two header lines
        String line;
        List<String> lines = new ArrayList<String>();
        while ((line = reader.readLine()) != null && lines.size() < 2) {
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

        // tokenise the first line
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
