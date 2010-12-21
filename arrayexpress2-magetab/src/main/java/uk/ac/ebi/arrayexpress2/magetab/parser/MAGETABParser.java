package uk.ac.ebi.arrayexpress2.magetab.parser;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Imports MAGETAB documents into a {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}.
 * The MAGETABParser implements logic to discover the IDF and SDRF components
 * from the URL supplied. The URL supplied should match the location of the IDF,
 * and the SDRF should be referenced within.  This parser then distributes tasks
 * to IDFParser and SDRFParser components.  Alternatively, you can invoke these
 * classes separately - you will just need to instantiate a new
 * MAGETABInvestigation to read into yourself. Ultimately, this results in a set
 * of objects that can be obtained from a MAGETABInvestigation using public
 * fields or getters.
 *
 * @author Tony Burdett
 * @date 17-Dec-2008
 * @see uk.ac.ebi.arrayexpress2.magetab.parser.IDFParser
 * @see uk.ac.ebi.arrayexpress2.magetab.parser.SDRFParser
 * @see uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation
 */
public class MAGETABParser extends AbstractParser<MAGETABInvestigation> {
    private static final int MAX_IDF_THREADS = 64;
    private static final int MAX_SDRF_THREADS = 64;

    private ParserMode mode;
    private volatile boolean greenLight;

    /**
     * Constructor for a MAGE-TAB importer.  This creates a new ExecutorService to
     * run import jobs in, using a cached thread pool to acquire new threads. This
     * is equivalent to calling <code>new MAGETABParser(HandlerMode.READ_ONLY)</code>.
     */
    public MAGETABParser() {
        this(ParserMode.READ_ONLY);
    }

    /**
     * Construct a new MAGETABParser with the given mode of operation.  The
     * parsing mode can be one of READ_ONLY, WRITE_ONLY or READ_AND_WRITE, as
     * given by the {@link uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode}
     * enum.  The default mode of operation is READ_ONLY.
     * <p/>
     * In READ_ONLY mode, when you call {@link #parse(java.net.URL)} on a MAGE-TAB
     * file, the file is read into memory and a {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}
     * object returned.  In READ_AND_WRITE mode, this in-memory investigation is
     * written to an external storage format - this would typically be a database
     * but you could write new handler implementations to support other
     * strategies, e.g. writing to a file.
     * <p/>
     * In WRITE_ONLY mode, you must pass the Investigation object you want to
     * write, using the {@link #parse(java.net.URL, uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)}
     * method.  If you use the default form, a new MAGETABInvestigation gets
     * created and nothing will be written.  Note that if you pass an
     * investigation which is partially populated, and use reading modes, some
     * fields will be overwritten whilst others may be added to.  Doing this is
     * not advised, as merging mage-tab documents is not a logical operation to
     * perform.
     * <p/>
     * The {@link uk.ac.ebi.arrayexpress2.magetab.handler.Handler}s which come
     * preconfigured with this parser application do not support writing
     * operations.  If you specify READ_AND_WRITE or WRITE_ONLY as the mode of
     * operation, nothing additional will happen.
     * <p/>
     * To add support for writing operations, you must first configure the {@link
     * uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool} with the new Handler
     * implementations.  See {@link uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool}
     * for more details on how to do this.  You could then write code like:
     * <code><pre>
     * HandlerPool.getInstance().configureHandlers(myHandlerSet);
     * MAGETABParser parser = new MAGETABParser(HandlerMode.WRITE_ONLY,
     * myInvestigation);
     * parser.parse(myMagetabDoc);
     * </pre></code>
     *
     * @param parsingMode the mode to run the parser in
     */
    public MAGETABParser(ParserMode parsingMode) {
        mode = parsingMode;
    }

    /**
     * Get the parsing mode for this MAGETABParser.  Parsing modes can be one of
     * READ_ONLY, WRITE_ONLY or READ_AND_WRITE.  The default implementation builds
     * a READ_ONLY parser, as the default set of handlers are all read only.
     * However, you should override this if you have supplied handlers with write
     * functionality that you want to use.
     *
     * @return the parsing mode for this parser
     */
    public ParserMode getParsingMode() {
        return mode;
    }

    /**
     * Set the parsing mode for this MAGETABParser.  Parsing modes can be one of
     * READ_ONLY, WRITE_ONLY or READ_AND_WRITE.  The default implementation builds
     * a READ_ONLY parser, as the default set of handlers are all read only.
     * However, you should override this if you have supplied handlers with write
     * functionality that you want to use.
     *
     * @param mode the parsing mode for this parser
     */
    public void setParsingMode(ParserMode mode) {
        this.mode = mode;
    }

    /**
     * Determine whether this parser will shutdown after parsing has finished. The
     * parser is backed by a service that executes handler tasks in a parallel
     * way, but this service can remain active afer completion of a parse
     * operation.  This improves performance by maintaining an existing pool of
     * threads, but it means that the application will remain live.  If you would
     * prefer the application to shutdown after the parse job has completed, set
     * this field to true.
     * <p/>
     * NOTE - this method is deprecated, as appropriate resources are now created
     * upon each call to {@link #parse(java.net.URL)}
     *
     * @return true if this service will shutdown on completion of the parse job,
     *         false otherwise
     * @deprecated
     */
    @Deprecated
    public boolean getShutdownOnCompletion() {
//    return shutdownOnCompletion;
        return false;
    }

    /**
     * Sets whether this parser will shutdown after parsing has finished. The
     * parser is backed by a service that executes handler tasks in a parallel
     * way, but this service can remain active afer completion of a parse
     * operation.  This improves performance by maintaining an existing pool of
     * threads, but it means that the application will remain live.  If you would
     * prefer the application to shutdown after the parse job has completed, set
     * this field to true.
     * <p/>
     * NOTE - this method is deprecated, as appropriate resources are now created
     * upon each call to {@link #parse(java.net.URL)}
     *
     * @param shutdownOnCompletion set this to true if you want the service to
     *                             shutdown on completion
     * @deprecated
     */
    @Deprecated
    public void setShutdownOnCompletion(boolean shutdownOnCompletion) {
//    this.shutdownOnCompletion = shutdownOnCompletion;
    }

    /**
     * Performs parsing of the specified source, populating a new {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}.  This
     * delegates parsing of the appropriate files to IDF and SDRF parsers, and
     * then performs content validation and integrity checking on the resultant
     * investigation model.  The source should point to the IDF file of this
     * MAGE-TAB document.
     *
     * @param magetabSource the URL of the IDF file for this MAGE-TAB document to
     *                      parse and validate
     * @return the populated investigation
     * @throws ParseException if the file could not be parsed
     */
    public MAGETABInvestigation parse(final URL magetabSource)
            throws ParseException {
        // create the investigation that we'll populate
        getLog().debug("Creating new investigation, and monitoring progress");
        MAGETABInvestigation investigation = new MAGETABInvestigation();

        return parse(magetabSource, investigation);
    }

    /**
     * Performs parsing of the specified source, populating the specified {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}.  This
     * delegates parsing of the appropriate files to IDF and SDRF parsers,  and
     * then performs content validation and integrity checking on the resultant
     * investigation model.  The source should point to the IDF file of this
     * MAGE-TAB document.
     * <p/>
     * This form of the method creates services for executing parallel parsing
     * tasks on IDF and SDRF, and releases resources once parsing is finished.
     * Normally you should use this forom of the method, rather than configure
     * your own services.  However, this is also possible using the four-argument
     * form of the method (see {@link #parse(java.net.URL,
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
     * java.util.concurrent.ExecutorService, java.util.concurrent.ExecutorService)})
     * if you desire to reuse services across many parsing operations.
     * <p/>
     *
     * @param magetabSource the URL of the IDF file for this MAGE-TAB document to
     *                      parse and validate
     * @param investigation the investigation object that will be populated.
     * @return a reference to the investigation parameter, which is now
     *         populated.
     * @throws ParseException if the file could not be parsed
     */
    public MAGETABInvestigation parse(final URL magetabSource,
                                      final MAGETABInvestigation investigation)
            throws ParseException {
        // create executor service
        ExecutorService idfService =
                Executors.newFixedThreadPool(MAX_IDF_THREADS);
        ExecutorService sdrfService =
                Executors.newFixedThreadPool(MAX_SDRF_THREADS);

        try {
            // run parse - this will
            return parse(magetabSource, investigation, idfService, sdrfService);
        } finally {
            try {
                idfService.shutdownNow();
                sdrfService.shutdownNow();

                boolean idfTerminated =
                        idfService.awaitTermination(5, TimeUnit.SECONDS);
                boolean sdrfTerminated =
                        idfService.awaitTermination(5, TimeUnit.SECONDS);

                if (!idfTerminated) {
                    // failed to shutdown IDF service due to unresponsive tasks
                    getLog().error("Failed to stop IDF parser resources - there are " +
                            "unresponsive handler tasks that could not be terminated. " +
                            "This is an unrecoverable error - you should kill this process.");
                }

                if (!sdrfTerminated) {
                    // failed to shutdown SDRF service due to unresponsive tasks
                    getLog().error("Failed to stop SDRF parser resources - there are " +
                            "unresponsive handler tasks that could not be terminated. " +
                            "This is an unrecoverable error - you should kill this process.");
                }
            } catch (InterruptedException e) {
                getLog().error("Unexpected error: ", e);
            }
        }
    }

    /**
     * Performs parsing of the specified source, writing into the supplied {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}.
     * <p/>
     * This method is extremely useful if you want to do concurrent monitoring of
     * the progress of a parse operation.  First, create a new {@link
     * MAGETABInvestigation}, then make a new thread that checks the progress of
     * the investigation (by calling the <code>getProgress()</code> method) and
     * does something with this information. Start this thread, then do the parse
     * operation.  It is perfectly possible to do this by java object monitors -
     * so, for example:
     * <code><pre>
     *  // create a new investigation
     *  final MAGETABInvestigation inv = new MAGETABInvestigation();
     * <p/>
     *  // set up a new thread to monitor the investigation
     *  new Thread(new Runnable() {
     *    public void run() {
     *      System.out.print("Parsing");
     *      while (inv.getProgress() < 100 && inv.getStatus() != Status.FAILED) {
     *        System.out.print(".");
     *        try {
     *          synchronized (inv) {
     *            inv.wait();
     *          }
     *        }
     *        catch (InterruptedException e) {}
     *      }
     *      System.out.println("100%!");
     *    }
     *  }).start();
     * <p/>
     *  // now do the parse, our thread will monitor it's progress
     *  MAGETABParser parser = new MAGETABParser();
     *  parser.parse(myFile, inv);
     * </pre></code>
     * <p/>
     * The above code is very simple but should work - obviously you should do
     * something more sensible with the progress output than just print to stdout
     * and you should manage your threads better, but this gives a clear idea of
     * how to create an investigation and have the parser monitor its progress.
     * <p/>
     * Using this method, it is possible to pass an investigation that has been
     * previously populated here.  This is strongly disadvised, as in some places
     * of the model overwriting of fields will occur but addition will happen in
     * other places.  Nothing will be taken out, but whre cardinalities are fixed
     * the new data will overwrite the old. Effectively, "merging" with some
     * overwriting will occur.  As there is no description in the MAGE-TAB spec of
     * what should occur when merging files, and there is no usecase for this,
     * results of a merge are not guaranteed to be correct and may contain an
     * incompatible subset of all the information supplied.
     * <p/>
     * This method uses the supplied services to execute any parsing tasks
     * required.  This method does NOT shutdown services on completion: if you use
     * this form of the method, calling classes are responsible for reclaiming
     * resources on completion.  This allows you to reuse services to execute many
     * MAGE-TAB files in parallel, if required.  Be warned though: there is a
     * potential for deadlock if you use the same service for IDF and SDRF
     * parsing, as these files can have circular dependencies.  Also, if the pool
     * size of the service is bounded, the order of parsing may become significant
     * when reusing the same service: some threads may block whilst waiting for
     * data from a service yet to start (but only if that data comes from a
     * different file).  In normal usage, you are advised to use the two-parameter
     * form of this method ({@link #parse(java.net.URL, uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)}).
     *
     * @param magetabSource the URL of the IDF file for this MAGE-TAB document to
     *                      parse and validate
     * @param investigation the investigation to write to
     * @param idfService    the executor service that will be used to execute IDF
     *                      parsing tasks
     * @param sdrfService   the executor service that will be used to execute SDRF
     *                      parsing tasks
     * @return the finalised investigation
     * @throws ParseException if the file could not be parsed
     */
    public MAGETABInvestigation parse(final URL magetabSource,
                                      final MAGETABInvestigation investigation,
                                      final ExecutorService idfService,
                                      final ExecutorService sdrfService)
            throws ParseException {
        // create an executor service that has a thread pool for executing jobs
        final ExecutorService service = Executors.newCachedThreadPool();

        try {
            // retrieve the source file
            getLog().info("Parsing MAGE-TAB from  " + magetabSource.toString());

            // start monitoring so we can give progress updates
            greenLight = true;
            monitorProgress(service, investigation, magetabSource);

            // update the progress to zero, as we're not indeterminate any more
            updateProgress(0);

            // load IDF with IDFImporter
            getLog().debug("Setting up IDF parser...");
            // set up IDF importer
            // register listener here too and set mode and investigation
            final IDFParser idfParser = new IDFParser();
            investigation.setNumberOfFiles(1);
            for (ErrorItemListener listener : getErrorItemListeners()) {
                idfParser.addErrorItemListener(listener);
            }
            idfParser.setParsingMode(mode);
            idfParser.setInvestigation(investigation);
            Future idfParse = service.submit(new Callable<IDF>() {
                public IDF call() throws ParseException {
                    try {
                        return idfParser.parse(idfService, magetabSource);
                    } catch (Exception e) {
                        if (e instanceof ParseException) {
                            ParseException pe = (ParseException) e;
                            if (pe.isCriticalException()) {
                                // fail if critical
                                // retrieve the error item
                                ErrorItem item = pe.getErrorItem();

                                // set the file being parsed?
                                if (item.getParsedFile() == null) {
                                    item.setParsedFile(magetabSource.toString());
                                }

                                // and fire the error
                                fireErrorItemEvent(item);
                                greenLight = false;
                                throw pe;
                            } else {
                                // log warning and continue
                                // retrieve the error item
                                ErrorItem item = pe.getErrorItem();

                                // set the file being parsed?
                                if (item.getParsedFile() == null) {
                                    item.setParsedFile(magetabSource.toString());
                                }

                                // and fire the error
                                fireErrorItemEvent(item);
                                return idfParser.getInvestigation().IDF;
                            }
                        } else {
                            // some unexpected error whilst parsing the IDF, need to explicitly
                            // handle to prevent deadlock in the following sdrf lookup
                            String message =
                                    "The MAGE-TAB document could not be parsed; parsing failed " +
                                            "due to an unexpected error [" +
                                            e.getClass().getSimpleName() + "]";

                            ErrorItem error =
                                    ErrorItemFactory
                                            .getErrorItemFactory(getClass().getClassLoader())
                                            .generateErrorItem(
                                                    message,
                                                    999,
                                                    this.getClass());

                            // set the file being parser
                            error.setParsedFile(magetabSource.toString());

                            // and fire the error
                            fireErrorItemEvent(error);
                            throw terminateService(service, e, magetabSource);
                        }
                    }
                }
            });
            getLog().debug("IDF parser started");

            // starts SDRF import once we have obtained the SDRF file from IDF
            boolean checking = true;
            while (checking) {
                getLog().trace("Checking possibility of SDRF parse starting...");
                // keep checking if we havent got sdrf file
                checking = checking &&
                        investigation.IDF.sdrfFile != null;

                if (checking) {
                    // keep checking if IDF has not finished reading?
                    checking = checking &&
                            investigation.IDF.getStatus().ordinal() <=
                                    Status.READING.ordinal();

                    if (checking) {
                        // keep checking if IDF has not failed
                        checking = checking &&
                                investigation.IDF.getStatus() != Status.FAILED;

                        if (checking) {
                            // only if we get to here do we wait
                            synchronized (investigation.IDF) {
                                try {
                                    investigation.IDF.wait(10000);
                                } catch (InterruptedException e) {
                                    // ignore this
                                }
                            }
                        } else {
                            getLog().trace("...exiting wait loop, status failed");
                        }
                    } else {
                        getLog().trace("...exiting wait loop, IDF finished reading");
                    }
                } else {
                    getLog().trace("...exiting wait loop, read sdrf location");
                }
            }

            // IDF has finished reading, so grab sdrf file(s)
            getLog().debug("Setting up SDRF parser(s)");
            Set<Future> sdrfParse = new HashSet<Future>();
            for (final String sdrfFileString : investigation.IDF.sdrfFile) {
                investigation.setNumberOfFiles(investigation.getNumberOfFiles() + 1);
                File magetabFilePath = new File(magetabSource.getFile());
                File sdrfFilePath =
                        new File(magetabFilePath.getParentFile(), sdrfFileString);

                // NB. making sure we replace File separators with '/' to guard against windows issues
                final URL sdrfSource = magetabSource.getPort() == -1 ?
                        new URL(magetabSource.getProtocol(),
                                magetabSource.getHost(),
                                sdrfFilePath.toString().replaceAll("\\\\", "/")) :
                        new URL(magetabSource.getProtocol(),
                                magetabSource.getHost(),
                                magetabSource.getPort(),
                                sdrfFilePath.toString().replaceAll("\\\\", "/"));

                getLog().debug("Attempting parse from " + sdrfSource.toString());

                // load SDRF with SDRFImporter
                final SDRFParser sdrfParser = new SDRFParser();
                // set up SDRF importer
                // register listener here too and set mode and investigation
                for (ErrorItemListener listener : getErrorItemListeners()) {
                    sdrfParser.addErrorItemListener(listener);
                }
                sdrfParser.setParsingMode(mode);
                sdrfParser.setInvestigation(investigation);
                sdrfParse.add(service.submit(new Callable<SDRF>() {
                    public SDRF call() throws Exception {
                        try {
                            return sdrfParser.parse(sdrfService, sdrfSource);
                        } catch (Exception e) {
                            if (e instanceof ParseException) {
                                ParseException pe = (ParseException) e;
                                if (pe.isCriticalException()) {
                                    // fail if critical
                                    // retrieve the error item
                                    ErrorItem item = pe.getErrorItem();

                                    // set line and column?
                                    if (item.getLine() == -1) {
                                        item.setLine(
                                                investigation.getLocationTracker().getIDFLocations(
                                                        "sdrffile"));
                                    }
                                    if (item.getCol() == -1) {
                                        item.setCol(
                                                investigation.IDF.sdrfFile.indexOf(sdrfFileString) + 2);
                                    }

                                    // set the file being parsed?
                                    if (item.getParsedFile() == null) {
                                        item.setParsedFile(magetabSource.toString());
                                    }

                                    // and fire the error
                                    fireErrorItemEvent(item);
                                    greenLight = false;
                                    throw e;
                                } else {
                                    // log warning and continue
                                    // retrieve the error item
                                    ErrorItem item = pe.getErrorItem();

                                    // set the file being parsed?
                                    if (item.getParsedFile() == null) {
                                        item.setParsedFile(magetabSource.toString());
                                    }

                                    // and fire the error
                                    fireErrorItemEvent(item);
                                    return sdrfParser.getInvestigation().SDRF;
                                }
                            } else {
                                // some unexpected error whilst parsing the IDF, need to explicitly
                                // handle to prevent deadlock in the following sdrf lookup
                                String message =
                                        "The MAGE-TAB document could not be parsed; parsing failed " +
                                                "due to an unexpected error [" +
                                                e.getClass().getSimpleName() + "]";

                                ErrorItem error =
                                        ErrorItemFactory
                                                .getErrorItemFactory(getClass().getClassLoader())
                                                .generateErrorItem(
                                                        message,
                                                        999,
                                                        this.getClass());

                                // set the file being parser
                                error.setParsedFile(magetabSource.toString());

                                // and fire the error
                                fireErrorItemEvent(error);
                                throw terminateService(service, e, magetabSource);
                            }
                        }
                    }
                }));
            }
            getLog().debug("SDRF parser(s) started");

            // wait for completion of all import, before validation
            idfParse.get();
            for (Future sdrfImport : sdrfParse) {
                sdrfImport.get();
            }

            // validate the content of this file
            getLog().debug("Validating MAGE-TAB contents");

            if (getValidator() != null) {
                getValidator().validate(investigation);
            }

            // check progress - should be 100, but if not
            // update it to 100 cos we have finished without exception
            if (getProgress() != 100) {
                getLog().debug(
                        "Finished parse, progress should be 100% but wasn't, fixing");
                updateProgress(100);
            }

            // and return the populated bag
            return investigation;
        } catch (MalformedURLException e) {
            throw terminateService(service, e, magetabSource);
        } catch (ExecutionException e) {
            throw terminateService(service, e.getCause(), magetabSource);
        } catch (InterruptedException e) {
            String message =
                    "The MAGE-TAB document could not be parsed; " +
                            "parsing was interrupted [" + e.getCause() + "]";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    999,
                                    this.getClass());

            // set the file being parser
            error.setParsedFile(magetabSource.toString());

            // and fire the error
            fireErrorItemEvent(error);
            throw terminateService(service, e, magetabSource);
        } catch (Exception e) {
            String message =
                    "The MAGE-TAB document could not be parsed; parsing failed " +
                            "due to an unexpected error [" +
                            e.getClass().getSimpleName() + "]";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    999,
                                    this.getClass());

            // set the file being parser
            error.setParsedFile(magetabSource.toString());

            // and fire the error
            fireErrorItemEvent(error);
            throw terminateService(service, e, magetabSource);
        } finally {
            greenLight = false;
            getLog().debug("Jobs finished, shutting down service...");
            service.shutdownNow();
            getLog().debug("MAGETABParser done");
        }
    }

    /**
     * Starts a dedicated thread to monitor the progress of the investigation.
     * Once started, this thread will update the progress of the Importer every
     * time the progress of the investigation changes, until the investigation has
     * completed, at which point the thread will terminate itself.
     *
     * @param service       the service for the current parse operation, that will
     *                      be used to monitor progress
     * @param investigation the investigation to monitor
     * @param parseFromURL  the URL that is being parsed, in case the accession
     *                      cannot be obtained in logging
     */
    protected void monitorProgress(final ExecutorService service,
                                   final MAGETABInvestigation investigation,
                                   final URL parseFromURL) {
        // start a dedicated thread to update the progress of the import from the investigation
        service.submit(new Runnable() {
            public void run() {
                while (greenLight &&
                        investigation.getProgress() < 100 &&
                        investigation.getStatus() != Status.FAILED) {
                    getLog().trace("" +
                            "Monitoring parse of " + parseFromURL + "... " +
                            "criteria: " +
                            "greenLight = " + greenLight + "; " +
                            "progress = " + investigation.getProgress() + "; " +
                            "status = " + investigation.getStatus());

                    // update our progress to match the investigation total progress
                    getLog().trace(
                            "Investigation progress now " + investigation.getProgress() +
                                    ", status " + investigation.getStatus() + " " +
                                    "(" + investigation.IDF.getProgress() + " + " +
                                    investigation.SDRF.getProgress() + ")");
                    updateProgress(investigation.getProgress());

                    // need to wait to make sure we only update every second
                    try {
                        synchronized (investigation) {
                            investigation.wait(5000);
                        }
                    } catch (InterruptedException e) {
                        // do nothing, just resume
                    }
                }

                if (investigation.getStatus() != Status.FAILED) {
                    if (!greenLight) {
                        getLog().debug(
                                "Parsing exited normally following greenlight termination, " +
                                        "halting progress monitor");
                    } else {
                        getLog().debug("Parsing exited due to 100% completion, " +
                                "halting progress monitor");
                    }
                    // parsing exited but due to termination or completion, not FAIL
                } else {
                    getLog().error(
                            "Parsing exited following update to 'failed' status, " +
                                    "halting progress monitor");
                }
                updateProgress(investigation.getProgress());

                String accString = investigation.accession;
                // overwrite if null
                if (investigation.accession == null) {
                    accString = "investigation from " + parseFromURL;
                }

                // log termination statements
                getLog().info("Parsed " + getProgress() + "% of " + accString + ".");
            }
        });
    }

    private ParseException terminateService(ExecutorService service,
                                            Throwable cause,
                                            URL fileParsing) {
        String errorMesg =
                cause.getClass().getSimpleName() + " at " +
                        cause.getStackTrace()[0].getFileName() + ", line " +
                        cause.getStackTrace()[0].getLineNumber();

        getLog().error(
                "Encountered an error (" + errorMesg + ") " +
                        "whilst parsing - " + fileParsing.toString() + ", " +
                        "terminating all MAGE-TAB import tasks...");

        // shutdown the service - this import failed
        long start = System.currentTimeMillis();
        service.shutdown();
        try {
            int timeout = 10;
            boolean terminated =
                    service.awaitTermination(timeout, TimeUnit.SECONDS);
            if (terminated) {
                long end = System.currentTimeMillis();
                double time = (end - start) / 1000.0;
                getLog().error("All import tasks exited after " + time + "s.");
            } else {
                getLog().error("Attempting to forcibly shutdown parser...");

                // last resort, force a shutdown
                service.shutdownNow();
                terminated = service.awaitTermination(timeout, TimeUnit.SECONDS);
                if (!terminated) {
                    // failed to shutdown service due to unresponsive tasks
                    getLog().error("Failed to shutdown parser - there are " +
                            "unresponsive handler tasks that could not be terminated. " +
                            "This is an unrecoverable error - you should kill this process.");
                }
            }
        } catch (InterruptedException e1) {
            getLog().error("Failed to shutdown parser - there are " +
                    "unresponsive handler tasks that could not be terminated. " +
                    "This is an unrecoverable error - you should kill this process.");
        }

        if (cause instanceof ParseException) {
            ErrorItem item = ((ParseException) cause).getErrorItem();

            // update the error item with the parsed file, if it's not set already
            if (item.getParsedFile() == null) {
                item.setParsedFile(fileParsing.toString());
            }

            return (ParseException) cause;
        } else {
            // we need to wrap the error inside a parse exception
            String message = "An unexpected program error caused parsing to fail";
            if (cause.getMessage() != null) {
                message = message.concat(": " + cause.getMessage());
            }

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, getClass());

            error.setParsedFile(fileParsing.toString());

            return new ParseException(error, true, cause);
        }
    }
}

