package uk.ac.ebi.arrayexpress2.magetab.parser;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler;
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
 * Imports IDF documents into a {@link IDF}. The IDFImporter implements a parser
 * for IDF documents, creating an in-memory object model to cache this date. The
 * data contained in the cache is then converted to AE2 object model instances,
 * and stored in a MAGETABObjectBag that can be validated, manipulated or
 * persisted as appropriate.
 * <p/>
 * This importer processes the document in parallel and uses {@link
 * uk.ac.ebi.arrayexpress2.magetab.handler.Handler}s to distribute the work.  As
 * handlers may, at some point, require data that is being processed by another
 * handler that is running in a parallel process, there is a degree of waiting
 * and communication between threads.  The IDFImporter manages this by using an
 * {@link ExecutorService} with a cached thread pool, but in some circumstances
 * this will lead to creation of excessive numbers of threads and will result in
 * a performance tail-off. However, this is better than the alternative,
 * deadlocking.  There may be a solution to this - intermittent checking of
 * threads and removal/resubmission to the executor service - but I don't think
 * it's possible without some seriously heavyweight taks management.
 * <p/>
 * Generally, it is advisable to use the MAGETABImporter rather than the
 * IDFImporter in isolation.
 *
 * @author Tony Burdett
 * @date 09-Mar-2009
 * @see MAGETABParser
 * @see SDRFParser
 * @see uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF
 */
public class IDFParser extends AbstractParser<IDF> {
    private final static int MAX_THREADS = 64;

    private MAGETABInvestigation investigation;

    private ParserMode mode;

    private HandlerPool handlers;

    private final Set<String> tagsDone;

    private double percentage;

    /**
     * Default constructor for SDRFImporter.  Normally, it is advisable to use
     * this, as doing so creates a new ExecutorService for this import and using a
     * new service minimises the chance of deadlock.
     */
    public IDFParser() {
        this.mode = ParserMode.READ_ONLY;

        // create the set of tags that have been done
        tagsDone = new HashSet<String>();

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

    public synchronized ParserMode getParsingMode() {
        return mode;
    }

    public synchronized void setParsingMode(ParserMode mode) {
        this.mode = mode;
    }

    /**
     * Performs parsing of the specified source, populating an {@link IDF} object.
     * If this parser has been set up with an existing investigation, the
     * investigation is reused and the objects created added to the existing IDF.
     * Also, any data already read into the investigation can be reused. This
     * makes it possible to run IDF and SDRF importers in parallel, updating the
     * same investigation.
     * <p/>
     * A {@link ParseException} will be thrown if and only if there was a problem
     * with the IDF file that meant parsing could not continue with any accuracy.
     * Note that you not use the absence of a parse exception to indicate that
     * parsing was successful, rather you should register an ErrorItemListener and
     * check any generated error items to determine whether the IDF is suitable
     * for your own purposes.
     * <p/>
     * This method dynamically creates and ExecutorService to submit parsing tasks
     * to, and terminates it once the parsing has completed.
     *
     * @param idfSource the URL of the IDF file to parse
     * @return the populated IDF objects
     * @throws ParseException if the file could not be parsed
     */
    public IDF parse(URL idfSource) throws ParseException {
        // create executor service
        ExecutorService service = Executors.newFixedThreadPool(MAX_THREADS);

        try {
            // run parse - this will
            return parse(service, idfSource);
        } finally {
            service.shutdown();
        }
    }

    /**
     * Performs parsing of the specified source, populating an {@link IDF} object,
     * and using the given ExecutorService for submission of any parsing tasks. If
     * this parser has been set up with an existing investigation, the
     * investigation is reused and the objects created added to the existing IDF.
     * Also, any data already read into the investigation can be reused. This
     * makes it possible to run IDF and SDRF importers in parallel, updating the
     * same investigation.
     * <p/>
     * A {@link ParseException} will be thrown if and only if there was a problem
     * with the IDF file that meant parsing could not continue with any accuracy.
     * Note that you not use the absence of a parse exception to indicate that
     * parsing was successful, rather you should register an ErrorItemListener and
     * check any generated error items to determine whether the IDF is suitable
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
     * @param service   the ExecutorService to submit parsing tasks to
     * @param idfSource the IDF source file to parse
     * @return the populated IDF object
     * @throws ParseException if the file could not be completely parsed
     */
    public IDF parse(ExecutorService service, URL idfSource)
            throws ParseException {
        getLog().info("Starting IDF parsing...");

        // clear the set of tagsDone
        tagsDone.clear();
        getLog().debug("Cleared tracker of those tags that have been parsed");

        int lineIndex = 0;
        try {
            // indexes required, to map individual tasks to the line number in the file
            getLog().debug("Building indices");
            Map<Integer, Integer> readLinesToActualLines =
                    new HashMap<Integer, Integer>();
            Map<Future, Integer> tasksToActualLines =
                    new HashMap<Future, Integer>();

            // create a set for the tasks we create
            List<Future<MAGETABInvestigation>> tasks =
                    new ArrayList<Future<MAGETABInvestigation>>();

            // map between future and callable
            Map<Future, Callable> callablesMap =
                    new HashMap<Future, Callable>(); // test

            getLog().debug("Opening connection to " + idfSource);

            // read from the file
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            idfSource.openConnection().getInputStream()));

            // grab every line in the file
            getLog().debug("Reading data from file");
            String line;
            List<String> lines = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                lineIndex++;
                int skippedLines = 0;

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
                            skippedLines++;
                        }

                        lines.add(line);

                        // update index for "read" line to actual line-in-file
                        int arrayIndex = lines.size() - 1;
                        readLinesToActualLines.put(arrayIndex, lineIndex);
                        lineIndex = lineIndex + skippedLines;
                    }
                }
            }
            reader.close();

            getLog().debug("Acquired all data to parse from IDF");

            // we can read from the IDF file ok, so set the location
            getInvestigation().IDF.setLocation(idfSource);

            // set the total number of IDF tasks
            getInvestigation().IDF.setNumberOfTasks(lines.size());

            // percentage is 100/number of lines
            percentage = ((double) 100) / ((double) lines.size());

            getLog().debug("Handling data...");

            // now, create handler tasks for each line
            for (int i = 0; i < lines.size(); i++) {
                int lineInFile = readLinesToActualLines.get(i);

                String nextLine = lines.get(i);
                Callable<MAGETABInvestigation> c =
                        createHandlerTask(nextLine, new Point(0, lineInFile), i);
                Future<MAGETABInvestigation> f = service.submit(c);
                callablesMap.put(f, c); // test
                tasks.add(f);

                // update index for task to actual line-in-file
                tasksToActualLines.put(f, lineInFile);
            }

            // block until complete
            for (Future<MAGETABInvestigation> task : tasks) {
                try {
                    getLog().debug("Blocking on next task: " +
                            callablesMap.get(task).toString()); // test
                    task.get();
                } catch (ExecutionException e) {
                    // does this constitute a critical fail?
                    if (e.getCause() instanceof ParseException &&
                            !((ParseException) e.getCause()).isCriticalException()) {
                        // can handle non-critical errors
                        ErrorItem item = ((ParseException) e.getCause()).getErrorItem();

                        // update error item with type of error
                        item.setErrorType("parse warning");

                        // update error item with the file
                        item.setParsedFile(idfSource.toString());

                        // update the error item with the line number
                        item.setLine(tasksToActualLines.get(task));
                        // by default, if not set column is 1
                        // as we're probably detecting a header problem
                        if (item.getCol() == -1) {
                            item.setCol(1);
                        }

                        fireErrorItemEvent(item);
                    } else if (e.getCause() instanceof ObjectConversionException &&
                            !((ObjectConversionException) e.getCause())
                                    .isCriticalException()) {
                        // can handle non-critical errors
                        ErrorItem item =
                                ((ObjectConversionException) e.getCause()).getErrorItem();

                        // update error item with type of error
                        item.setErrorType("write warning");

                        // update error item with the file
                        item.setParsedFile(idfSource.toString());

                        // update the error item with the line and column number
                        item.setLine(tasksToActualLines.get(task));
                        // by default, if not set column is 1
                        // as we're probably detecting a header problem
                        if (item.getCol() == -1) {
                            item.setCol(1);
                        }

                        // just fire an error item
                        fireErrorItemEvent(item);
                    } else {
                        getInvestigation().IDF.setStatus(Status.FAILED);

                        // this exception thrown if the computation threw an exception
                        // check for type and throw the cause if possible
                        if (e.getCause() instanceof ParseException) {
                            ErrorItem item = ((ParseException) e.getCause()).getErrorItem();

                            // update error item with type of error
                            item.setErrorType("parse error");

                            // update error item with the file
                            item.setParsedFile(idfSource.toString());

                            // update the error item with the line number
                            item.setLine(tasksToActualLines.get(task));
                            // by default, if not set column is 1
                            // as we're probably detecting a header problem
                            if (item.getCol() == -1) {
                                item.setCol(1);
                            }

                            // throw the exception
                            throw (ParseException) e.getCause();
                        } else if (e.getCause() instanceof ObjectConversionException) {
                            ObjectConversionException oce =
                                    (ObjectConversionException) e.getCause();
                            ErrorItem item = oce.getErrorItem();

                            // update error item with type of error
                            item.setErrorType("write error");

                            // update error item with the file
                            item.setParsedFile(idfSource.toString());

                            // update the error item with the line number
                            item.setLine(tasksToActualLines.get(task));

                            // throw the exception
                            throw new ParseException(item, true, oce.getMessage(), e);
                        } else {
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
                            error.setParsedFile(idfSource.toString());

                            throw new ParseException(error, true, message, e);
                        }
                    }
                }
            }

            getLog().info("IDF parsing and syntactic validation finished");

            // and return the populated bag of objects
            return getInvestigation().IDF;
        } catch (InterruptedException e) {
            getInvestigation().IDF.setStatus(Status.FAILED);
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
            error.setParsedFile(idfSource.toString());

            // update the error item with the line number
            error.setLine(lineIndex);

            throw new ParseException(error, true, message, e);
        } catch (IOException e) {
            getInvestigation().IDF.setStatus(Status.FAILED);
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
            error.setParsedFile(idfSource.toString());

            // update the error item with the line number
            error.setLine(lineIndex);

            throw new ParseException(error, true, message, e);
        } catch (ParseException pe) {
            getInvestigation().IDF.setStatus(Status.FAILED);
            // can occur during line reformatting

            // update error item with the file
            pe.getErrorItem().setParsedFile(idfSource.toString());

            // update the error item with the line number
            if (pe.getErrorItem().getLine() == -1) {
                pe.getErrorItem().setLine(lineIndex);
            }

            // just rethrow
            throw pe;
        } catch (Exception e) {
            getInvestigation().IDF.setStatus(Status.FAILED);
            ErrorItem error = ErrorItemFactory
                    .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
                    .generateErrorItem(
                            "Unexpected error", 999, MAGETABUtils.class);

            // just rethrow a parsed excpetion
            throw new ParseException(error, true, e);
        }
    }

    private Callable<MAGETABInvestigation> createHandlerTask(
            final String lineData, final Point location, int taskIndex) {
        final int localTaskIndex = taskIndex;
        getLog().trace("Creating handler for " + lineData);
        return new Callable<MAGETABInvestigation>() {

            public MAGETABInvestigation call()
                    throws ParseException, ObjectConversionException {
                getLog().trace("Calling handler for " + lineData);
                // read the line and split it up appropriately
                String[] tokens = MAGETABUtils.splitLine(lineData, false);
                String tag = tokens[0];

                tag = MAGETABUtils.digestHeader(tag);

                boolean doneTag = false;
                synchronized (tagsDone) {
                    if (tagsDone.contains(tag) && !tag.startsWith("comment")) {
                        doneTag = true;
                    }
                }

                if (doneTag) {
                    // if we have done this tag already, fail, because it's a duplicate
                    getLog().debug("Tag '" + tag + "' (task index " + localTaskIndex +
                            ") is a duplicate, no handler requested");

                    // make sure progress updating happens, so parsing doesn't stall
                    getInvestigation().IDF.increaseProgressBy(percentage);
                    getInvestigation().IDF.updateTaskList(
                            localTaskIndex, Status.COMPLETE);

                    String message = tag + " is a duplicate tag, and will be ignored";

                    ErrorItem error =
                            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(
                                            message,
                                            ErrorCode.DUPLICATE_IDF_COLUMN,
                                            this.getClass());

                    throw new ParseException(error, false, message);
                } else {
                    try {
                        // add to the done tags
                        synchronized (tagsDone) {
                            tagsDone.add(tag);
                        }

                        // lookup the handler for each tag and handle it
                        IDFHandler handler = handlers.getIDFHandler(tag,
                                lineData,
                                getInvestigation(),
                                localTaskIndex,
                                percentage);
                        if (handler != null) {
                            // actually handle the data
                            handler.setHandlerMode(mode);
                            handler.handle();

                            // update location info
                            getInvestigation().getLocationTracker()
                                    .trackLocation(handler, location);
                        }

                        // and return the investigation
                        return investigation;
                    } catch (NullPointerException e) {
                        getLog().debug("Service processing IDF parsing tasks " +
                                "encountered a " + e.getClass().getSimpleName() +
                                ", rethrown as a ParseException.  " +
                                "StackTrace dumped to error stream");

                        String message =
                                "Could not begin parsing due to an error creating handler(s)";

                        ErrorItem error = ErrorItemFactory
                                .getErrorItemFactory(getClass().getClassLoader())
                                .generateErrorItem(message, 999,
                                        IDFParser.class);

                        throw new ParseException(error, true, message, e);
                    }
                }
            }

            public String toString() {
                try {
                    return "IDFHandler for " + MAGETABUtils.splitLine(lineData, false)[0];
                } catch (ParseException e) {
                    return "IDFHandler for {" + lineData + "}";
                }
            }
        };
    }

    /**
     * Simply return the amount of IDF that has been parsed.
     *
     * @return the progress so far
     */
    public int getProgress() {
        return getInvestigation().IDF.getProgress();
    }
}
