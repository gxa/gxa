package uk.ac.ebi.arrayexpress2.magetab.parser;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.FactorValueNodeHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ProtocolHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Imports SDRF documents into an {@link SDRF}.  The SDRFImporter implements a
 * parser for SDRF documents, creating an in-memory object model to cache this
 * date. The data contained in the cache is then converted to AE2 object model
 * instances, and stored in a MAGETABObjectBag that can be validated,
 * manipulated or persisted as appropriate.
 * <p/>
 * As for the {@link IDFParser}, this importer processes the document in
 * parallel and uses {@link uk.ac.ebi.arrayexpress2.magetab.handler.Handler}s to
 * distribute the work.  As handlers may, at some point, require data that is
 * being processed by another handler that is running in a parallel process,
 * there is a degree of waiting and communication between threads.
 * <p/>
 * In addition, SDRF parsing requires resolution of objects that are only
 * present in the IDF document.  This means that SDRF files cannot easily be
 * loaded in isolation.  As such, several SDRF handler implementations require
 * data in the IDF portion of the cache to be present before they procede.  If
 * these objects are missing, it may not be possible to complete SDRF import,
 * resulting in timeout issues.
 * <p/>
 * Generally, it is advisable to use the MAGETABImporter rather than the
 * SDRFImporter in isolation.
 *
 * @author Tony Burdett
 * @date 09-Mar-2009
 * @see MAGETABParser
 * @see IDFParser
 * @see uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF
 */
public class SDRFParser extends AbstractParser<SDRF> {
  private static final int MAX_THREADS = 64;

  private MAGETABInvestigation investigation;

  private ParserMode mode;

  private HandlerPool handlers;

  private final Map<Integer, Class<? extends SDRFHandler>>
      handlerIndex;

  private double percentage = 0;

  /**
   * Default constructor for SDRFImporter.  Normally, it is advisable to use
   * this, as doing so creates a new ExecutorService for this import and using a
   * new service minimises the chance of deadlock.
   */
  public SDRFParser() {
    this.mode = ParserMode.READ_ONLY;

    // generate a map to index handler classes by column number
    handlerIndex = new HashMap<Integer, Class<? extends SDRFHandler>>();

    // get a handler pool
    handlers = HandlerPool.getInstance();
  }

  /**
   * Gets the MAGETABInvestigation that this importer will use to cache data
   * parsed from SDRF.  Essentially, this acts as an in-memory cache for the
   * data as it gets parsed, but before database objects are manufactured.
   *
   * @return the investigation to use as an in-memory cache
   */
  public synchronized MAGETABInvestigation getInvestigation() {
    if (investigation == null) {
      // not been set, so no re-use - construct a new investigation
      investigation = new MAGETABInvestigation();
    }
    return investigation;
  }

  /**
   * Sets the MAGETABInvestigation that this importer will use to cache data
   * parsed from SDRF.  Essentially, this acts as an in-memory cache for the
   * data as it gets parsed, but before database objects are manufactured.
   *
   * @param investigation the investigation to use as an in-memory cache
   */
  public synchronized void setInvestigation(
      final MAGETABInvestigation investigation) {
    this.investigation = investigation;
  }

  public ParserMode getParsingMode() {
    return mode;
  }

  public void setParsingMode(ParserMode mode) {
    this.mode = mode;
  }

  /**
   * Performs parsing of the specified source, populating an {@link
   * uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF} object. If this parser has
   * been set up with an existing investigation, the investigation is reused and
   * the objects created added to the existing SDRF. Also, any data already read
   * into the investigation can be reused. This makes it possible to run IDF and
   * SDRF importers in parallel, updating the same investigation.
   * <p/>
   * A {@link ParseException} will be thrown if and only if there was a problem
   * with the SDRF file that meant parsing could not continue with any accuracy.
   * Note that you not use the absence of a parse exception to indicate that
   * parsing was successful, rather you should register an ErrorItemListener and
   * check any generated error items to determine whether the IDF is suitable
   * for your own purposes.
   * <p/>
   * This method dynamically creates and ExecutorService to submit parsing tasks
   * to, and terminates it once the parsing has completed.
   *
   * @param sdrfSource the URL of the SDRF file to parse
   * @return the populated SDRF objects
   * @throws ParseException if the file could not be parsed
   */
  public SDRF parse(URL sdrfSource) throws ParseException {
    // create executor service
    ExecutorService service = Executors.newFixedThreadPool(MAX_THREADS);

    try {
      // run parse - this will
      return parse(service, sdrfSource);
    }
    finally {
      service.shutdown();
    }
  }

  /**
   * Performs import of the specified source, populating an {@link SDRF}.  If
   * this importer has been set up with an existing investigation and object
   * bag, these are reused and the objects created added to the existing cache.
   * Also, any data already read into the investigation can be reused.  This
   * makes it possible to run IDF and SDRF importers in parallel, updating the
   * same object bag and investigation.
   * <p/>
   * A {@link ParseException} will be thrown if and only if there was a problem
   * with the SDRF file that meant parsing could not continue with any accuracy.
   * Note that you not use the absence of a parse exception to indicate that
   * parsing was successful, rather you should register an ErrorItemListener and
   * check any generated error items to determine whether the SDRF is suitable
   * for your own purposes.
   * <p/>
   * Note that normally the single parameter version of this method should be
   * called {@link #parse(java.net.URL)}.  This method can be used when calling
   * you require explicit control of the execution strategy, number of threads
   * etc.  However, you should take care using this form: if you parse IDF and
   * SDRF in the same service and limit the number of threads, it is possible to
   * cause deadlock as all active threads wait for information from a thread
   * that cannot start.
   * <p/>
   * This method does not shutdown the executor service on completion, so you
   * can reuse the same service if required.  Once all parsing operations
   * finish, calling classes are responsible for releasing any resources used.
   *
   * @param service    the ExecutorService to submit parsing tasks to
   * @param sdrfSource the URL of the sdrfFile to parse
   * @return the populated SDRF object
   * @throws ParseException if a problem occurred whilst parsing, such that
   *                        parsing could not continue with any accuracy
   */
  public SDRF parse(ExecutorService service, URL sdrfSource)
      throws ParseException {
    getLog().info("Starting SDRF parsing...");

    // test to see if this sdrfSource actually exists
    try {
      URLConnection urlConn = sdrfSource.openConnection();
      if (urlConn instanceof HttpURLConnection) {
        HttpURLConnection httpConn = (HttpURLConnection) urlConn;
        if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new IOException();
        }
      }
      else {
        // see if this url is available
        if (urlConn.getInputStream().available() <= 0) {
          throw new IOException();
        }
      }
    }
    catch (IOException e) {
      String message =
          "The SDRF file is not present or inaccessible [" + e + "]";

      ErrorItem error =
          ErrorItemFactory
              .getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  11,
                  this.getClass());

      // update error item with type of error
      error.setErrorType("parser error");

      // set the file being parsed
      error.setParsedFile(sdrfSource.toString());

      throw new ParseException(error, true, message, e);
    }

    // it exists, so we can try reading
    int rowIndex = 0;
    try {
      // indexes required, to map individual tasks to the line number in the file
      Map<Integer, Integer> readLinesToActualLines =
          new HashMap<Integer, Integer>();
      Map<Future, Integer> tasksToActualLines =
          new HashMap<Future, Integer>();
      Map<Future, Integer> tasksToActualColumns =
          new HashMap<Future, Integer>();

      // tabulate this file in a task
      Future<String[][]> tabulatorTask =
          service.submit(createTabulatorTask(sdrfSource,
                                             readLinesToActualLines));

      // get the table - this will block until complete
      String[][] sdrfTable = tabulatorTask.get();

      // we can read from the SDRF file ok, so set the location
      getInvestigation().SDRF.setLocation(sdrfSource);

      // if the sdrf file has no content, just update and return
      if (sdrfTable.length == 0) {
        getInvestigation().SDRF.increaseProgressBy(100);
        return getInvestigation().SDRF;
      }

      // now we have the table of results, farm them out to handlers

      // get the header row
      String[] headers = sdrfTable[0];

      // calculate the number of handlers - product of rowCount*handlers per line
      int rowCount = sdrfTable.length - 1;
      int hplCount = countHandlersRequired(headers);
      int handlerCount = rowCount * hplCount;


      // handler count is the number of tasks
      getInvestigation().SDRF.setNumberOfTasks(
          getInvestigation().SDRF.getNumberOfTasks() + handlerCount);

      // calculate percentage - work out what percent each task is worth
      percentage = ((double) (100)) / handlerCount;

      getLog().debug("Starting tasks to handle and import SDRF...");

      // graph structure reads left to right, so parse columsn from left to right
      List<Integer> handlerIndices = new ArrayList<Integer>();
      handlerIndices.addAll(handlerIndex.keySet());
      Collections.sort(handlerIndices);

      // each task increments as we read across columns
      int columnIndex = 0;

      // track whether any errorItems were fired that didn't throw an exception
      boolean zeroErrorItems = true;

      // farm out the table of results to handlers
      for (int nextStartPos : handlerIndices) {
        // create a set of the tasks that get created for this column
        List<Future<MAGETABInvestigation>> tasks =
            new ArrayList<Future<MAGETABInvestigation>>();

        // create a task to parse each bit of each row
        for (int i = 1; i < sdrfTable.length; i++) {
          // first task in the row is (rows-1)*hplCount
          rowIndex = (i - 1) * hplCount;

          // the sdrfTable is an array of rows, represented as String[]s
          String[] row = sdrfTable[i];

          // get the task number for this parser
          int taskIndex = rowIndex + columnIndex;

          // this is a fairly ugly hack for factor values, but the spec is a bit weird with them
          // check the header tag - special rule
          Future<MAGETABInvestigation> task;

          int colInFile = nextStartPos + 1;
          int lineInFile = readLinesToActualLines.get(i);

          if (FactorValueNodeHandler.class
              .isAssignableFrom(handlerIndex.get(nextStartPos))) {
            getLog().debug("Next start position is " + nextStartPos +
                ", this coincides with a FactorValueNodeHandler so using " +
                "factor value task allowing read-backwards");

            // special hack, pass all the bits of data
            task = service.submit(createSpecialParserTaskForFactorValues(
                headers, row,
                new Point(colInFile, lineInFile),
                nextStartPos,
                taskIndex));
          }
          else if (ProtocolHandler.class
              .isAssignableFrom(handlerIndex.get(nextStartPos))) {
            getLog().debug("Next start position is " + nextStartPos +
                ", this coincides with a ProtocolHandler so using " +
                "protocol task allowing read-backwards");

            // special hack, pass all the bits of data
            task = service.submit(createSpecialParserTaskForProtocols(
                headers, row,
                new Point(colInFile, lineInFile),
                nextStartPos,
                taskIndex));
          }
          else {
            // pass only the relevant bits of data to create the task
            final String[] headerData =
                MAGETABUtils
                    .extractRange(headers, nextStartPos, headers.length);
            final String[] rowData =
                MAGETABUtils.extractRange(row, nextStartPos, row.length);

            task = service.submit(createHandlerTask(
                headerData, rowData,
                new Point(colInFile, lineInFile),
                taskIndex));
          }

          // update indexes for this task
          tasksToActualLines.put(task, lineInFile);
          tasksToActualColumns.put(task, colInFile);

          tasks.add(task);
        }
        columnIndex++;

        // all tasks submitted for this column - only start the next once these have completed
        boolean nextPass = blockWhilstTasksComplete(
            tasks, sdrfSource, tasksToActualLines, tasksToActualColumns);

        zeroErrorItems = zeroErrorItems && nextPass;
      }

      if (zeroErrorItems) {
        // confirm 100% progress - sometimes this is required due to loss of precisions
        getInvestigation().SDRF.increaseProgressBy(
            100 - getInvestigation().SDRF.getProgress());
      }

      getLog().info("SDRF parsing and syntactic validation finished");

      // and return the populated bag
      return getInvestigation().SDRF;
    }
    catch (InterruptedException e) {
      getInvestigation().SDRF.setStatus(Status.FAILED);

      String message =
          "The SDRF file could not be imported; import was interrupted [" +
              e.getCause() + "]";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  999,
                  this.getClass());

      // update error item with type of error
      error.setErrorType("parse error");

      // update error item with the file
      error.setParsedFile(sdrfSource.toString());

      // update the error item with the line number
      error.setLine(rowIndex);

      throw new ParseException(error, true, message, e);
    }
    catch (ParseException e) {
      // can occur during line reformatting, or task to count handlers
      getInvestigation().SDRF.setStatus(Status.FAILED);

      // update error item with the file
      e.getErrorItem().setParsedFile(sdrfSource.toString());

      // update the error item with the line number
      if (e.getErrorItem().getLine() == -1) {
        e.getErrorItem().setLine(rowIndex);
      }

      // just rethrow
      throw e;
    }
    catch (ExecutionException e) {
      if (e.getCause() instanceof ParseException) {
        ParseException pe = (ParseException) e.getCause();

        // can occur during line reformatting, or task to count handlers
        getInvestigation().SDRF.setStatus(Status.FAILED);

        // update the error item with the file
        pe.getErrorItem().setParsedFile(sdrfSource.toString());

        // just rethrow
        throw pe;
      }
      else {
        // probably occurred during task to count handlers, rewrap in a generic message
        getInvestigation().SDRF.setStatus(Status.FAILED);

        String message =
            "The SDRF file could not be imported; import was interrupted [" +
                e.getCause() + "]";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    999,
                    this.getClass());

        // update error item with type of error
        error.setErrorType("system error");

        error.setParsedFile(sdrfSource.toString());

        throw new ParseException(error, true, message, e);
      }
    }
  }

  private Callable<String[][]> createTabulatorTask(
      final URL sdrfSource,
      final Map<Integer, Integer> readLinesToActualLines) {
    return new Callable<String[][]>() {
      public String[][] call() throws Exception {
        String[][] tabulated;

        // read from the file
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(
                sdrfSource.openConnection().getInputStream()));

        // read into a temp store to tabulate the data in the file
        boolean doneHeader = false;
        List<List<String>> tempStore = null;
        int lineIndex = 0;
        String line;
        while ((line = reader.readLine()) != null) {
          lineIndex++;
          int skippedLines = 0;

          // read lines - first non-empty line is the header
          line = removeWhitespaceNullsAndComments(line);

          // might need to reformat lines, compensating for escaping
          String firstLine = line;
          while (MAGETABUtils.endsWithEscapedNewline(firstLine)) {
            String secondLine = reader.readLine();
            line = MAGETABUtils.compensateForEscapedNewlines(
                firstLine, secondLine);
            firstLine = secondLine;
            skippedLines++;
          }

          // now ready to tabulate
          if (doneHeader) {
            // read lines and enter into tabulated
            if (!line.equals("") && !line.startsWith("#")) {
              // tokenize the line
              String[] tokens = MAGETABUtils.splitLine(line, false);

              // check this line is the same length as the header
              if (tokens.length > tempStore.get(0).size()) {
                String message =
                    "Wrong number of elements on this line - expected: " +
                        tempStore.get(0).size() + " found: " + tokens.length;

                ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(
                        getClass().getClassLoader()).generateErrorItem(
                        message,
                        ErrorCode.BAD_SDRF_ORDERING,
                        SDRFParser.this.getClass());
                error.setLine(lineIndex);

                throw new IllegalLineLengthException(error, false, message);
              }
              else {
                // initialize a string array, and add this row
                ArrayList<String> entry = new ArrayList<String>();
                entry.addAll(Arrays.asList(tokens));
                // also, pad any extra columns on the end with empty strings
                while (entry.size() < tempStore.get(0).size()) {
                  entry.add("");
                }

                // now add this entry to the tempStroe
                tempStore.add(entry);

                // update index for "read" line to actual line-in-file
                int arrayIndex = tempStore.size() - 1;
                readLinesToActualLines.put(arrayIndex, lineIndex);
              }
            }
          }
          else {
            // need to read the header - this is first non-empty line
            if (!line.equals("")) {
              // initialize the tempStore
              tempStore = new ArrayList<List<String>>();

              // tokenize the line
              String[] tokens = MAGETABUtils.splitLine(line, false);

              // initialise a string array, and add the headers
              ArrayList<String> headers = new ArrayList<String>();
              for (String header : tokens) {
                headers.add(MAGETABUtils.digestHeader(header));
              }

              // now add the headers to the tempStore
              tempStore.add(headers);

              // we've done the header now
              doneHeader = true;
            }
          }

          // update line counter
          lineIndex = lineIndex + skippedLines;
        }
        reader.close();

        // now we've populated tempStore with an list of lists, convert to array of arrays
        if (tempStore != null) {
          tabulated = new String[tempStore.size()][];
          for (int i = 0; i < tabulated.length; i++) {
            List<String> row = tempStore.get(i);
            tabulated[i] = new String[row.size()];
            tabulated[i] = row.toArray(new String[row.size()]);
          }

          return tabulated;
        }

        return new String[0][0];
      }
    };
  }

  private Callable<MAGETABInvestigation> createHandlerTask(
      final String[] headerData,
      final String[] rowData,
      final Point location,
      int taskIndex) {
    final int localIndex = taskIndex;
    return new Callable<MAGETABInvestigation>() {
      public MAGETABInvestigation call()
          throws ParseException, ObjectConversionException {
        String tag = MAGETABUtils.digestHeader(headerData[0]);
        getLog().trace(
            "Handler for tag \'" + tag + "\' located at index " + localIndex);

        try {
          SDRFHandler handler = handlers.getSDRFHandler(tag,
                                                        headerData, rowData,
                                                        getInvestigation(),
                                                        localIndex,
                                                        percentage);
          handler.setHandlerMode(mode);
          String row = "";
          for (String s : rowData) {
            row = row.concat(s + "; ");
          }
          getLog().trace(
              "Handling data with " + handler.toString() + "; tag = " + tag +
                  ", line data = " + row);

          handler.handle();

          // update location info
          getInvestigation().getLocationTracker()
              .trackLocation(handler, location);

          return getInvestigation();
        }
        catch (NullPointerException e) {
          getLog().debug("Service processing SDRF parsing tasks " +
              "encountered a " + e.getClass().getSimpleName() +
              ", rethrown as a ParseException.  " +
              "StackTrace dumped to error stream");

          String message =
              "Could not begin parsing due to an error creating handler(s)";

          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(message, 999,
                                     SDRFParser.class);

          throw new ParseException(error, true, message, e);
        }
      }
    };
  }

  private Callable<MAGETABInvestigation> createSpecialParserTaskForFactorValues(
      final String[] headerData,
      final String[] rowData,
      final Point location,
      int startIndex,
      int taskIndex) {
    final int localStartIndex = startIndex;
    final int localTaskIndex = taskIndex;
    return new Callable<MAGETABInvestigation>() {
      public MAGETABInvestigation call() throws Exception {
        String tag = MAGETABUtils.digestHeader(headerData[localStartIndex]);
        getLog().trace("Creating parser task for " + tag);

        try {
          SDRFHandler handler = handlers.getSDRFHandler(tag,
                                                        headerData, rowData,
                                                        getInvestigation(),
                                                        localTaskIndex,
                                                        percentage);
          getLog().trace("Handler obtained for " + tag + ", of type " +
              handler.getClass().getSimpleName());

          handler.setHandlerMode(ParserMode.READ_ONLY);
          if (handler instanceof FactorValueNodeHandler) {
            FactorValueNodeHandler fvnHandler =
                (FactorValueNodeHandler) handler;
            fvnHandler.setStartIndex(localStartIndex);

            handler.setHandlerMode(mode);
            String row = "";
            for (String s : rowData) {
              row = row.concat(s + "; ");
            }
            getLog().trace(
                "Handling data with " + handler.toString() + "; tag = " + tag +
                    ", line data = " + row);

            handler.handle();

            // update location info
            getInvestigation().getLocationTracker()
                .trackLocation(handler, location);

            return getInvestigation();
          }
          else {
            String message = "Bad handler generation - extra logic " +
                "implicit in FactorValues requires special handling";

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message, 999, SDRFParser.class);

            // you shouldn't use this method otherwise
            throw new ParseException(error, true, message);
          }
        }
        catch (NullPointerException e) {
          getLog().debug("Service processing SDRF parsing tasks " +
              "encountered a " + e.getClass().getSimpleName() +
              ", rethrown as a ParseException.  " +
              "StackTrace dumped to error stream");

          String message =
              "Could not begin parsing due to an error creating handler(s)";

          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(message, 999,
                                     SDRFParser.class);

          throw new ParseException(error, true, message, e);
        }
      }
    };
  }

  private Callable<MAGETABInvestigation> createSpecialParserTaskForProtocols(
      final String[] headerData,
      final String[] rowData,
      final Point location,
      int startIndex,
      int taskIndex) {
    final int localStartIndex = startIndex;
    final int localTaskIndex = taskIndex;
    return new Callable<MAGETABInvestigation>() {
      public MAGETABInvestigation call() throws Exception {
        String tag = MAGETABUtils.digestHeader(headerData[localStartIndex]);
        getLog().trace("Creating parser task for " + tag);

        try {
          SDRFHandler handler = handlers.getSDRFHandler(tag,
                                                        headerData, rowData,
                                                        getInvestigation(),
                                                        localTaskIndex,
                                                        percentage);
          getLog().trace("Handler obtained for " + tag + ", of type " +
              handler.getClass().getSimpleName());

          handler.setHandlerMode(ParserMode.READ_ONLY);
          if (handler instanceof ProtocolHandler) {
            ProtocolHandler protocolHandler =
                (ProtocolHandler) handler;
            protocolHandler.setStartIndex(localStartIndex);

            handler.setHandlerMode(mode);
            String row = "";
            for (String s : rowData) {
              row = row.concat(s + "; ");
            }
            getLog().trace(
                "Handling data with " + handler.toString() + "; tag = " + tag +
                    ", line data = " + row);

            handler.handle();

            // update location info
            getInvestigation().getLocationTracker()
                .trackLocation(handler, location);

            return getInvestigation();
          }
          else {
            String message = "Bad handler generation - extra logic " +
                "implicit in Protocols requires special handling";

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message, 999, SDRFParser.class);

            // you shouldn't use this method otherwise
            throw new ParseException(error, true, message);
          }
        }
        catch (NullPointerException e) {
          getLog().debug("Service processing SDRF parsing tasks " +
              "encountered a " + e.getClass().getSimpleName() +
              ", rethrown as a ParseException.  " +
              "StackTrace dumped to error stream");

          String message =
              "Could not begin parsing due to an error creating handler(s)";

          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(message, 999,
                                     SDRFParser.class);

          throw new ParseException(error, true, message, e);
        }
      }
    };
  }

  private int countHandlersRequired(String[] headers) throws ParseException {
    int handlerCount = 0;

    getLog().trace("Assessing the number of handlers required to parse SDRF");

    for (int index = 0; index < headers.length;) {
      handlerCount++;
      String tag = MAGETABUtils.digestHeader(headers[index]);

      // headerData
      String[] headerData =
          MAGETABUtils.extractRange(headers, index, headers.length);
      // dummy investigation
      MAGETABInvestigation investigation = new MAGETABInvestigation();
      // dummy "empty" row
      String[] rowData = new String[headerData.length];
      for (int i = 0; i < rowData.length; i++) {
        rowData[i] = "";
      }

      getLog()
          .trace("Next handler starts at tag: " + tag + ", index: " + index);

      SDRFHandler handler = handlers.getSDRFHandler(tag,
                                                    headerData, rowData,
                                                    investigation,
                                                    -1,
                                                    0);
      handler.setHandlerMode(ParserMode.READ_ONLY);
      handlerIndex.put(index, handler.getClass());

      getLog().trace(
          "Assessing handler [" + handler.getClass().getSimpleName() + ":" +
              handler.toString() + "]  for read-forward");
      index += handler.assess();
      getLog().trace(
          "Read-forward for handler of " + tag + " ends at " + index);
    }

    return handlerCount;
  }

  private boolean blockWhilstTasksComplete(
      List<Future<MAGETABInvestigation>> tasks,
      URL sdrfSource,
      Map<Future, Integer> tasksToActualLines,
      Map<Future, Integer> tasksToActualColumns)
      throws ParseException, InterruptedException {
    boolean outcome = true;

    for (Future<MAGETABInvestigation> task : tasks) {
      try {
        getLog().debug("Blocking on next task. SDRF section starts at " +
            tasksToActualLines.get(task) + ", " +
            tasksToActualColumns.get(task)); // test
        task.get();
      }
      catch (ExecutionException e) {
        // does this constitute a critical fail?
        if (e.getCause() instanceof ParseException &&
            !((ParseException) e.getCause()).isCriticalException()) {
          // can handle non-critical errors
          ErrorItem item =
              ((ParseException) e.getCause()).getErrorItem();

          // update error item with type of error
          item.setErrorType("parse warning");

          // update error item with the file
          item.setParsedFile(sdrfSource.toString());

          // update the error item with the line and column number
          item.setLine(tasksToActualLines.get(task));
          item.setCol(tasksToActualColumns.get(task));

          // just fire an error item
          fireErrorItemEvent(item);

          // and set the outcome to false, as we fired an error item
          outcome = false;
        }
        else if (e.getCause() instanceof ObjectConversionException &&
            !((ObjectConversionException) e.getCause())
                .isCriticalException()) {
          // can handle non-critical errors
          ErrorItem item =
              ((ObjectConversionException) e.getCause()).getErrorItem();

          // update error item with type of error
          item.setErrorType("write warning");

          // update error item with the file
          item.setParsedFile(sdrfSource.toString());

          // update the error item with the line and column number
          item.setLine(tasksToActualLines.get(task));
          item.setCol(tasksToActualColumns.get(task));

          // just fire an error item
          fireErrorItemEvent(item);

          // and set the outcome to false, as we fired an error item
          outcome = false;
        }
        else {
          getInvestigation().SDRF.setStatus(Status.FAILED);

          // this exception thrown if the computation threw an exception
          // check for type and throw the cause if possible
          if (e.getCause() instanceof ParseException) {
            ErrorItem item = ((ParseException) e.getCause()).getErrorItem();

            // update error item with type of error
            item.setErrorType("parse error");

            // update error item with the file
            item.setParsedFile(sdrfSource.toString());

            // update the error item with the line and column number
            item.setLine(tasksToActualLines.get(task));
            item.setCol(tasksToActualColumns.get(task));

            // throw the exception
            throw (ParseException) e.getCause();
          }
          else if (e.getCause() instanceof ObjectConversionException) {
            ObjectConversionException oce =
                (ObjectConversionException) e.getCause();
            ErrorItem item = oce.getErrorItem();

            // update error item with type of error
            item.setErrorType("write error");

            // update error item with the file
            item.setParsedFile(sdrfSource.toString());

            // update the error item with the line and column number
            item.setLine(tasksToActualLines.get(task));
            item.setCol(tasksToActualColumns.get(task));

            // throw the exception
            throw new ParseException(
                item, oce.isCriticalException(), oce.getMessage(), e);
          }
          else {
            e.getCause().printStackTrace();
            String message =
                "The SDRF file could not be parsed; the parse computation " +
                    "threw an unexpected exception [" + e.getCause() + "]";

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message,
                        999,
                        this.getClass());

            // update error item with type of error
            error.setErrorType("system error");

            // set the file being parsed
            error.setParsedFile(sdrfSource.toString());

            throw new ParseException(error, true, message, e);
          }
        }
      }
    }

    return outcome;
  }

  private String removeWhitespaceNullsAndComments(String line) {
    if (StringUtil.isEmpty(line.trim())) {
      return "";
    }
    else if (line.startsWith("#")) {
      return "";
    }
    else {
      return line;
    }
  }

  /**
   * Simply returns the amount of the SDRF that has been parsed
   *
   * @return the amount of SDRF parsed
   */
  public int getProgress() {
    return getInvestigation().SDRF.getProgress();
  }
}
