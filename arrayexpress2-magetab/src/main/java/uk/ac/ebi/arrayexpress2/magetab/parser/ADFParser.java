package uk.ac.ebi.arrayexpress2.magetab.parser;

import com.google.common.io.Closeables;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.ADF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFGraphHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFHeaderHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Imports ADF documents into an {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.ADF}.
 * The ADFParser implements a parser for ADF documents, creating an in-memory
 * object model to cache this date. The data contained in the cache fairly
 * closely models the structure of the spreadsheet as described by the MAGETAB
 * specification.
 * <p/>
 * As for the {@link IDFParser}, this importer processes the document in
 * parallel and uses {@link uk.ac.ebi.arrayexpress2.magetab.handler.Handler}s to
 * distribute the work.  As handlers may, at some point, require data that is
 * being processed by another handler that is running in a parallel process,
 * there is a degree of waiting and communication between threads.
 * <p/>
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 * @see uk.ac.ebi.arrayexpress2.magetab.datamodel.ADF
 */
public class ADFParser extends AbstractParser<ADF> {
  private final static int MAX_THREADS = 64;

  private MAGETABArrayDesign arrayDesign;

  private ParserMode mode;

  private HandlerPool handlers;

  private final Set<String> tagsDone;

  private final Map<Integer, Class<? extends ADFGraphHandler>>
      handlerIndex;

  private double percentage;

  public ADFParser() {
    this.mode = ParserMode.READ_ONLY;

    // create the set of tags that have been done
    tagsDone = new HashSet<String>();

    // generate a map to index handler classes by column number
    handlerIndex = new HashMap<Integer, Class<? extends ADFGraphHandler>>();

    // get a handler pool
    handlers = HandlerPool.getInstance();
  }

  /**
   * Gets the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign}
   * that this parser will use to cache data parsed from ADF.  Essentially, this
   * acts as an in-memory model of the array design data as it gets parsed.
   *
   * @return the arrayDesign that is used to model this ADF and associated data
   */
  public synchronized MAGETABArrayDesign getArrayDesign() {
    if (arrayDesign == null) {
      arrayDesign = new MAGETABArrayDesign();
    }
    return arrayDesign;
  }

  /**
   * Sets the {@link MAGETABArrayDesign} that this parser will use to cache data
   * parsed from ADF.  Essentially, this acts as an in-memory model of the array
   * design data as it gets parsed.
   *
   * @param arrayDesign the investigation to use as an in-memory cache
   */
  public synchronized void setArrayDesign(
      final MAGETABArrayDesign arrayDesign) {
    this.arrayDesign = arrayDesign;
  }

  public synchronized ParserMode getParsingMode() {
    return mode;
  }

  public synchronized void setParsingMode(ParserMode mode) {
    this.mode = mode;
  }

  public ADF parse(URL parserSource) throws ParseException {
    // create executor service
    ExecutorService service = Executors.newFixedThreadPool(MAX_THREADS);

    try {
      // run parse - this will
      return parse(service, parserSource);
    }
    finally {
      service.shutdown();
    }
  }

  public ADF parse(ExecutorService service, URL parserSource)
      throws ParseException {
    getLog().info("Starting ADF parsing...");

    try {
      // first, create an iterator over the graph part of the ADF
      ADFGraphIterator adfGraphIterator = new ADFGraphIterator(parserSource);

      // now, grab the header lines
      BufferedReader reader = null;
        List<String> headerLines;
        Map<Integer, Integer> readLinesToActualLines;
        Map<Future, Integer> tasksToActualLines;
        Map<Future, Integer> tasksToActualColumns;
        try {
            reader = new BufferedReader(new InputStreamReader(
              parserSource.openConnection().getInputStream()));

            // grab every header line in the file
            headerLines = new ArrayList<String>();

            // indexes required, to map individual tasks to the line number in the file
            readLinesToActualLines = new HashMap<Integer, Integer>();
            tasksToActualLines = new HashMap<Future, Integer>();
            tasksToActualColumns = new HashMap<Future, Integer>();

            // read in the header parts
            for (int i = 0; i < adfGraphIterator.getGraphOffset(); i++) {
              String line = reader.readLine();

              // ignore empty lines
              if (!line.trim().equals("")) {
                if (!line.startsWith("#")) {
                  // reformat and unescape lines
                  String firstLine = line;
                  while (MAGETABUtils.endsWithEscapedNewline(firstLine)) {
                    String secondLine = reader.readLine();
                    line = MAGETABUtils
                        .compensateForEscapedNewlines(firstLine, secondLine);
                    firstLine = secondLine;
                  }
                  headerLines.add(line);

                  // update index for "read" line to actual line-in-file
                  int arrayIndex = headerLines.size() - 1;
                  readLinesToActualLines.put(arrayIndex, i);
                }
              }
            }
        } finally {
            Closeables.closeQuietly(reader);
        }

      // create the set for all our parsing tasks
      Set<Future<MAGETABArrayDesign>> tasks =
          new HashSet<Future<MAGETABArrayDesign>>();

      // check whether the graph part is present
      if (adfGraphIterator.getGraphHeader() != null) {
        // we now have the header lines cached and some stats about the graph part
        int hplCount = countHandlersRequired(adfGraphIterator.getGraphHeader());
        int graphHandlerCount = adfGraphIterator.getGraphLineCount() * hplCount;
        int totalTasks = headerLines.size() + graphHandlerCount;

        // we can read from the IDF file ok, so set the location
        getArrayDesign().ADF.setLocation(parserSource);

        // set the total number of IDF tasks
        getArrayDesign().ADF.setNumberOfTasks(totalTasks);

        // calculate percentage - work out what percent each task is worth
        percentage = ((double) (100)) / totalTasks;

        // now start the business of parsing the two parts of the file
        parseHeaderPart(service,
                        tasks,
                        headerLines,
                        readLinesToActualLines,
                        tasksToActualLines);
        parseGraphPart(service,
                       tasks,
                       adfGraphIterator,
                       headerLines.size(),
                       tasksToActualLines,
                       tasksToActualColumns);
      }
      else {
        // no graph part, so just read header
        // we can read from the IDF file ok, so set the location
        getArrayDesign().ADF.setLocation(parserSource);

        // set the total number of IDF tasks
        getArrayDesign().ADF.setNumberOfTasks(headerLines.size());

        // calculate percentage - work out what percent each task is worth
        percentage = ((double) (100)) / headerLines.size();

        // now start the business of parsing the header part of the file
        parseHeaderPart(service,
                        tasks,
                        headerLines,
                        readLinesToActualLines,
                        tasksToActualLines);
      }

      // now block until everything is done
      // block until all tasks are done
      boolean zeroErrorItems = true;
      for (Future<MAGETABArrayDesign> task : tasks) {
        try {
          task.get();
        }
        catch (ExecutionException e) {
          // we've encountered an error item
          zeroErrorItems = false;

          // does this constitute a critical fail?
          if (e.getCause() instanceof ParseException &&
              !((ParseException) e.getCause()).isCriticalException()) {
            // can handle non-critical errors
            ErrorItem item = ((ParseException) e.getCause()).getErrorItem();

            // update error item with type of error
            item.setErrorType("parse warning");

            // update error item with the file
            item.setParsedFile(parserSource.toString());

            // update the error item with the line number
            item.setLine(tasksToActualLines.get(task));
            if (tasksToActualColumns.containsKey(task)) {
              item.setCol(tasksToActualColumns.get(task));
            }
            else {
              // by default, if not set column is 1
              // as we're probably detecting a header problem
              if (item.getCol() == -1) {
                item.setCol(1);
              }
            }

            fireErrorItemEvent(item);
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
            item.setParsedFile(parserSource.toString());

            // update the error item with the line and column number
            item.setLine(tasksToActualLines.get(task));
            if (tasksToActualColumns.containsKey(task)) {
              item.setCol(tasksToActualColumns.get(task));
            }
            else {
              // by default, if not set column is 1
              // as we're probably detecting a header problem
              if (item.getCol() == -1) {
                item.setCol(1);
              }
            }

            // just fire an error item
            fireErrorItemEvent(item);
          }
          else {
            getArrayDesign().ADF.setStatus(Status.FAILED);

            // this exception thrown if the computation threw an exception
            // check for type and throw the cause if possible
            if (e.getCause() instanceof ParseException) {
              ErrorItem item = ((ParseException) e.getCause()).getErrorItem();

              // update error item with type of error
              item.setErrorType("parse error");

              // update error item with the file
              item.setParsedFile(parserSource.toString());

              // update the error item with the line number
              item.setLine(tasksToActualLines.get(task));
              if (tasksToActualColumns.containsKey(task)) {
                item.setCol(tasksToActualColumns.get(task));
              }
              else {
                // by default, if not set column is 1
                // as we're probably detecting a header problem
                if (item.getCol() == -1) {
                  item.setCol(1);
                }
              }

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
              item.setParsedFile(parserSource.toString());

              // update the error item with the line number
              item.setLine(tasksToActualLines.get(task));

              // throw the exception
              throw new ParseException(item, true, oce.getMessage(), e);
            }
            else {
              String message =
                  "The IDF file could not be parsed; the parse computation " +
                      "threw an unexpected exception [" + e.getCause() + "]";

              ErrorItem error =
                  ErrorItemFactory
                      .getErrorItemFactory(getClass().getClassLoader())
                      .generateErrorItem(
                          message,
                          999,
                          this.getClass());

              // set the file being parsed
              error.setParsedFile(parserSource.toString());

              throw new ParseException(error, true, message, e);
            }
          }
        }
        catch (InterruptedException e) {
          getArrayDesign().ADF.setStatus(Status.FAILED);

          String message =
              "The IDF file could not be parsed; parsing was interrupted [" +
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
          error.setParsedFile(parserSource.toString());

          // update the error item with the line number
          error.setLine(tasksToActualLines.get(task));

          throw new ParseException(error, true, message, e);
        }
      }

      if (zeroErrorItems) {
        // confirm 100% progress - sometimes this is required due to loss of precisions
        getArrayDesign().ADF.increaseProgressBy(
            100 - getArrayDesign().ADF.getProgress());
      }


      getLog().info("ADF parsing and syntactic validation finished");
      return arrayDesign.ADF;
    }
    catch (IOException e) {
      getArrayDesign().ADF.setStatus(Status.FAILED);

      String message =
          "The ADF file could not be parsed; parsing was interrupted [" +
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
      error.setParsedFile(parserSource.toString());

      throw new ParseException(error, true, message, e);
    }
  }

  /**
   * Hand off the lines from the source file that constitute the ADF header for
   * parsing.  This method will produce and allocate the handlers for each part
   * of the ADF.
   *
   * @param service                the executor service to submit parsing tasks
   *                               to
   * @param tasks                  the set of tasks to add new parsing tasks to
   * @param headerLines            the lines making up the header
   * @param readLinesToActualLines a map that tracks those lines which are part
   *                               of the ADF header to actual line numbers in
   *                               the file.  Useful when this ADF contains
   *                               comments, for example.
   * @param tasksToActualLines     indexes newly created tasks to the line in
   *                               the file which they are responsible for
   *                               handling
   * @return a reference to the ADF object after new data has been handled
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if parsing failed for any reason
   */
  protected synchronized ADF parseHeaderPart(
      ExecutorService service,
      Set<Future<MAGETABArrayDesign>> tasks,
      List<String> headerLines,
      Map<Integer, Integer> readLinesToActualLines,
      Map<Future, Integer> tasksToActualLines)
      throws ParseException {
    getLog().debug("Starting tasks to handle header part of ADF...");

    // now, create handler tasks for each line
    for (int i = 0; i < headerLines.size(); i++) {
      int lineInFile = readLinesToActualLines.get(i);

      String nextLine = headerLines.get(i);
      Future<MAGETABArrayDesign> f = service.submit(
          createHeaderHandlerTask(nextLine, new Point(0, lineInFile), i));
      tasks.add(f);

      // update index for task to actual line-in-file
      tasksToActualLines.put(f, lineInFile);
    }

    return arrayDesign.ADF;
  }

  /**
   * Handles parsing the graph part of the ADF.  This method takes several
   * parameters - the ExecutorService that should be used to execute any new
   * parsing tasks, the set of parsing tasks, an ADFGraphIterator object for
   * extracting data from the ADF graph, the task index of the first parsing
   * task, an index that tracks new tasks to the actual line in the file, and an
   * index that tracks new tasks to the actual column in the file.
   * <p/>
   * New tasks should update the ADF with their status upon fail/completion.  To
   * do this, they must update by index.  This is what the 'firstTaskIndex'
   * handles - parsing tasks for the header part occupy the first batch of
   * indices, so graph parsing tasks should be created from this point onwards.
   * You can implement a simple counter that creates task indices upwards of
   * this value.
   *
   * @param service              the executor service to submit parsing tasks
   *                             to
   * @param tasks                the set of tasks to add new parsing tasks to
   * @param adfGraphIterator     an iterator over the adf graph part of the
   *                             source
   * @param firstTaskIndex       the first task index position to use
   * @param tasksToActualLines   indexes newly created tasks to the actual line
   *                             in the file
   * @param tasksToActualColumns indexes newly created tasks to the actual
   *                             column in the file
   * @return a reference to the ADF after graph parsing has finished
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if parsing fails for any reason
   */
  protected synchronized ADF parseGraphPart(
      ExecutorService service,
      Set<Future<MAGETABArrayDesign>> tasks,
      ADFGraphIterator adfGraphIterator,
      int firstTaskIndex,
      Map<Future, Integer> tasksToActualLines,
      Map<Future, Integer> tasksToActualColumns)
      throws ParseException {
    getLog().debug("Starting tasks to handle graph part of ADF...");

    // graph structure reads left to right, so parse columsn from left to right
    List<Integer> handlerIndices = new ArrayList<Integer>();
    handlerIndices.addAll(handlerIndex.keySet());
    Collections.sort(handlerIndices);

    // each task increments as we read across columns
    int currentLine = adfGraphIterator.getGraphOffset();
    int taskIndex = firstTaskIndex;

    // iterate line by line, as provided by the adfGraphIterator
    while (adfGraphIterator.hasNext()) {
      currentLine++;
      String[] values = adfGraphIterator.next();

      // now loop over blocks that can be handled together
      for (int nextStartPos : handlerIndices) {
        // get the task number for this parser
        // pass only the relevant bits of data to create the task
        final String[] headerData = MAGETABUtils.extractRange(
            adfGraphIterator.getGraphHeader(),
            nextStartPos,
            adfGraphIterator.getGraphHeader().length);
        final String[] rowData = MAGETABUtils.extractRange(
            values,
            nextStartPos,
            values.length);

        Future<MAGETABArrayDesign> task =
            service.submit(createGraphHandlerTask(
                headerData,
                rowData,
                new Point(nextStartPos, currentLine),
                taskIndex));

        // update indexes for this task
        tasksToActualLines.put(task, currentLine);
        tasksToActualColumns.put(task, nextStartPos);

        tasks.add(task);
        taskIndex++;
      }
    }

    return arrayDesign.ADF;
  }

  private String[] parseGraphHeader(String line) throws ParseException {
    // tokenize the line
    String[] tokens = MAGETABUtils.splitLine(line, false);

    // initialise a string array, and add the headers
    ArrayList<String> headers = new ArrayList<String>();
    for (String header : tokens) {
      headers.add(MAGETABUtils.digestHeader(header));
    }

    return headers.toArray(new String[headers.size()]);
  }

  private int countHandlersRequired(String[] headers) throws ParseException {
    int handlerCount = 0;

    getLog().debug("Assessing the number of handlers required to parse ADF");

    for (int index = 0; index < headers.length;) {
      handlerCount++;
      String tag = MAGETABUtils.digestHeader(headers[index]);

      // headerData
      String[] headerData =
          MAGETABUtils.extractRange(headers, index, headers.length);
      // dummy investigation
      MAGETABArrayDesign arrayDesign = new MAGETABArrayDesign();
      // dummy "empty" row
      String[] rowData = new String[headerData.length];
      for (int i = 0; i < rowData.length; i++) {
        rowData[i] = "";
      }

      getLog()
          .debug("Next handler starts at tag: " + tag + ", index: " + index);

      ADFGraphHandler handler = handlers.getADFGraphHandler(tag,
                                                            headerData, rowData,
                                                            arrayDesign,
                                                            -1,
                                                            0);
      handler.setHandlerMode(ParserMode.READ_ONLY);
      handlerIndex.put(index, handler.getClass());

      getLog().debug(
          "Assessing handler [" + handler.getClass().getSimpleName() + ":" +
              handler.toString() + "]  for read-forward");
      index += handler.assess();
      getLog().debug(
          "Read-forward for handler of " + tag + " ends at " + index);
    }

    return handlerCount;
  }

  private Callable<MAGETABArrayDesign> createHeaderHandlerTask(
      final String lineData, final Point location, int taskIndex) {
    final int localTaskIndex = taskIndex;
    return new Callable<MAGETABArrayDesign>() {

      public MAGETABArrayDesign call()
          throws ParseException, ObjectConversionException {
        // read the line and split it up appropriately
        String[] tokens = MAGETABUtils.splitLine(lineData, false);
        String tag = tokens[0];

        tag = MAGETABUtils.digestHeader(tag);
        if (tagsDone.contains(tag)) {
          // if we have done this tag already, fail, because it's a duplicate
          getLog().debug("Tag '" + tag + "' (task index " + localTaskIndex +
              ") is a duplicate, no handler requested");

          // make sure progress updating happens, so parsing doesn't stall
          getArrayDesign().ADF.increaseProgressBy(percentage);
          getArrayDesign().ADF.updateTaskList(
              localTaskIndex, Status.COMPLETE);

          String message = tag + " is a duplicate tag, and will be ignored";

          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(
                      message,
                      ErrorCode.DUPLICATE_IDF_COLUMN,
                      this.getClass());

          throw new ParseException(error, false, message);
        }
        else {
          try {
            // add to the done tags
            tagsDone.add(tag);

            // lookup the handler for each tag and handle it
            ADFHeaderHandler handler = handlers.getADFHeaderHandler(
                tag,
                lineData,
                getArrayDesign(),
                localTaskIndex,
                percentage);

            if (handler != null) {
              // actually handle the data
              handler.setHandlerMode(mode);
              handler.handle();

              // update location info
              getArrayDesign().getLocationTracker()
                  .trackLocation(handler, location);
            }

            // and return the investigation
            return arrayDesign;
          }
          catch (NullPointerException e) {
            getLog().debug("Service processing ADF parsing tasks " +
                "encountered a " + e.getClass().getSimpleName() +
                ", rethrown as a ParseException.  " +
                "StackTrace dumped to error stream");

            String message =
                "Could not begin parsing due to an error creating handler(s)";

            ErrorItem error = ErrorItemFactory
                .getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(message, 999,
                                   ADFParser.class);

            throw new ParseException(error, true, message, e);
          }
        }
      }
    };
  }

  private Callable<MAGETABArrayDesign> createGraphHandlerTask(
      final String[] headerData,
      final String[] rowData,
      final Point location,
      int taskIndex) {
    final int localIndex = taskIndex;
    return new Callable<MAGETABArrayDesign>() {
      public MAGETABArrayDesign call()
          throws ParseException, ObjectConversionException {
        String tag = MAGETABUtils.digestHeader(headerData[0]);
        getLog().debug(
            "Handler for tag \'" + tag + "\' located at index " + localIndex);

        try {
          ADFGraphHandler handler = handlers.getADFGraphHandler(
              tag,
              headerData,
              rowData,
              getArrayDesign(),
              localIndex,
              percentage);

          handler.setHandlerMode(mode);
          String row = "";
          for (String s : rowData) {
            row = row.concat(s + "; ");
          }
          getLog().debug(
              "Handling data with " + handler.toString() + "; tag = " + tag +
                  ", line data = " + row);

          handler.handle();

          // update location info
          getArrayDesign().getLocationTracker()
              .trackLocation(handler, location);

          return getArrayDesign();
        }
        catch (NullPointerException e) {
          getLog().debug("Service processing ADF parsing tasks " +
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

  /**
   * A class for iterating over an ADF document.  This class is backed by a URL
   * and internally manages the connection to the document.  You should
   * instantiate an ADFGraphIterator with an offset value, marking the point
   * within the document the graph part starts.
   */
  private class ADFGraphIterator implements Iterator<String[]> {
    private URL parserSource;
    private BufferedReader sourceReader;

    private int currentLineNumber = 0;
    private String thisLine;
    private String nextLine;

    private int graphOffset;
    private int graphLineCount;
    private String[] graphHeader;

    public ADFGraphIterator(URL parserSource)
        throws IOException, ParseException {
      this.parserSource = parserSource;
      this.sourceReader = new BufferedReader(new InputStreamReader(
          parserSource.openConnection().getInputStream()));
      init();
    }

    public synchronized int getGraphOffset() {
      return graphOffset;
    }

    public synchronized int getGraphLineCount() {
      return graphLineCount;
    }

    public synchronized String[] getGraphHeader() {
      return graphHeader;
    }

    public synchronized boolean hasNext() {
      try {
        if (nextLine == null) {
          sourceReader.close();
          return false;
        } else {
          return true;
        }
      }
      catch (IOException e) {
        return false;
      }
    }

    public synchronized String[] next() {
      try {
        if (hasNext()) {
          // we have a valid next line, so split it into its tokens
          currentLineNumber++;
          thisLine = nextLine;
          nextLine = sourceReader.readLine();
          return MAGETABUtils.splitLine(thisLine, false);
        }
        else {
          throw new NoSuchElementException("No more elements");
        }
      }
      catch (ParseException e) {
        throw new NoSuchElementException(
            "Escaping in the underlying document " +
                "was invalid, the connection was closed");
      }
      catch (IOException e) {
        throw new NoSuchElementException(
            "No more elements could be read, the underlying connection was " +
                "broken");
      }
    }

    public synchronized void remove() {
      throw new UnsupportedOperationException(
          "remove() is not supported by " + getClass().getName());
    }

    private synchronized void init() throws IOException, ParseException {
      try {
        String line;
        int lineIndex = 0;
        boolean finishedHeader = false;
        while ((line = sourceReader.readLine()) != null) {
          if (!finishedHeader) {
            // still looping over the header
            // ignore empty lines
            if (!line.trim().equals("")) {
              if (!line.startsWith("#")) {
                if (evaluateLineForGraphStart(line)) {
                  // this is the first line of the graph part
                  graphOffset = lineIndex;
                  graphHeader = parseGraphHeader(line);
                  finishedHeader = true;
                }
              }
            }
          }
          else {
            // we're into the graph part, all we care about is line number count
            graphLineCount++;
          }
          lineIndex++;
        }
        sourceReader.close();
      }
      finally {
        // open a new reader ready to iterate
        sourceReader = new BufferedReader(new InputStreamReader(
            parserSource.openConnection().getInputStream()));
        for (int i = 0; i <= graphOffset; i++) {
          currentLineNumber++;
          sourceReader.readLine();
        }

        // nextLine is null, cos we grab nextLine whenever we do next() (i.e. straight away)
        thisLine = null;
        nextLine = sourceReader.readLine();
      }
    }

    private boolean evaluateLineForGraphStart(String line)
        throws ParseException {
      // this is a valid magetab line, not an empty line or something
      String[] tokens = MAGETABUtils.splitLine(line, false);
      if (tokens.length > 0) {
        String firstToken = MAGETABUtils.digestHeader(tokens[0]);
        if (firstToken.equals("blockcolumn") ||
            firstToken.equals("reportername") ||
            firstToken.equals("compositeelementname")) {
          // this is the first line of the header
          return true;
        }
      }
      return false;
    }
  }
}
