package uk.ac.ebi.arrayexpress2.magetab.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFGraphHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFHeaderHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

import java.util.*;

/**
 * A singleton class that provides access to a set of IDF and SDRF handlers,
 * meaning handlers can be reused where possible.  This class needs to be
 * preconfigured with a set of handler classes, but at some point I may move
 * this into spring configuration.
 * <p/>
 * A new instance of a handler pool is requested using HandlerPool.getInstance(),
 * and from this pool, handlers can be obtained using getIDFHandler() and
 * getSDRFHandler() methods.  If there is a handler of the requested type in the
 * pool already, that is not being used, it will be returned, otherwise a new
 * one will be instantiated.
 * <p/>
 * If you need to modify the set of handlers that are used in MAGE-TAB parsing
 * operations (see {@link uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser}),
 * you can supply a set of class files this handler pool will manage using the
 * {@link #setHandlerClasses(java.util.Set)} method, supplying a set of classes
 * that implement either the {@link uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler}
 * or {@link uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler}
 * interfaces.  If you don't need writing functionality, you can use the {@link
 * #useDefaultHandlers()} method to get a handler pool preloaded with default
 * handlers.
 */
public class HandlerPool {
  private static HandlerPool ourInstance = new HandlerPool();

  /**
   * Obtain a singleton instance of the HandlerPool.  This HandlerPool can be
   * used directly to obtain and relinquish handlers.
   *
   * @return the handler pool instances
   */
  public static HandlerPool getInstance() {
    return ourInstance;
  }

  // idf handlers that can be acquired - indexed by class, and stored
  private final List<Class<? extends IDFHandler>>
      idfHandlerClasses;
  private final Map<String, Class<? extends IDFHandler>>
      idfHandlerMap;
  private final Map<Class<? extends IDFHandler>, List<IDFHandler>>
      idfHandlerStore;

  // sdrf handlers that can be acquired - indexed by class, and stored
  private final List<Class<? extends SDRFHandler>>
      sdrfHandlerClasses;
  private final Map<String, Class<? extends SDRFHandler>>
      sdrfHandlerMap;
  private final Map<Class<? extends SDRFHandler>, List<SDRFHandler>>
      sdrfHandlerStore;

  // adf header handlers that can be acquired - indexed by class, and stored
  private final List<Class<? extends ADFHeaderHandler>>
      adfHeaderHandlerClasses;
  private final Map<String, Class<? extends ADFHeaderHandler>>
      adfHeaderHandlerMap;
  private final Map<Class<? extends ADFHeaderHandler>, List<ADFHeaderHandler>>
      adfHeaderHandlerStore;

  // adfGraph handlers that can be acquired - indexed by class, and stored
  private final List<Class<? extends ADFGraphHandler>>
      adfGraphHandlerClasses;
  private final Map<String, Class<? extends ADFGraphHandler>>
      adfGraphHandlerMap;
  private final Map<Class<? extends ADFGraphHandler>, List<ADFGraphHandler>>
      adfGraphHandlerStore;

  private Log log = LogFactory.getLog(this.getClass());

  private HandlerPool() {
    // create the handler mappings - actual handlers are semi-lazily instantiated
    idfHandlerClasses = new ArrayList<Class<? extends IDFHandler>>();
    idfHandlerMap = new HashMap<String, Class<? extends IDFHandler>>();
    idfHandlerStore =
        new HashMap<Class<? extends IDFHandler>, List<IDFHandler>>();

    // create the handler mappings - actual handlers are semi-lazily instantiated
    sdrfHandlerClasses = new ArrayList<Class<? extends SDRFHandler>>();
    sdrfHandlerMap = new HashMap<String, Class<? extends SDRFHandler>>();
    sdrfHandlerStore =
        new HashMap<Class<? extends SDRFHandler>, List<SDRFHandler>>();

    adfHeaderHandlerClasses =
        new ArrayList<Class<? extends ADFHeaderHandler>>();
    adfHeaderHandlerMap =
        new HashMap<String, Class<? extends ADFHeaderHandler>>();
    adfHeaderHandlerStore =
        new HashMap<Class<? extends ADFHeaderHandler>, List<ADFHeaderHandler>>();

    // create the handler mappings - actual handlers are semi-lazily instantiated
    adfGraphHandlerClasses = new ArrayList<Class<? extends ADFGraphHandler>>();
    adfGraphHandlerMap =
        new HashMap<String, Class<? extends ADFGraphHandler>>();
    adfGraphHandlerStore =
        new HashMap<Class<? extends ADFGraphHandler>, List<ADFGraphHandler>>();

    // add all the handler classes we will need
    useDefaultHandlers();
  }

  /**
   * Configures this HandlerPool with a default set of handlers.  This set
   * includes all the handler classes in the {@link uk.ac.ebi.arrayexpress2.magetab.handler.idf}
   * and {@link uk.ac.ebi.arrayexpress2.magetab.handler.sdrf} packages.
   * <p/>
   * Note that calling this method merely sets up the pool with a known set of
   * classes.  It does not instantiate any handlers - this happens on demand
   * whenever handlers are requested.
   */
  public void useDefaultHandlers() {
    Set<Class<? extends Handler>> handlerClasses =
        new HashSet<Class<? extends Handler>>();

    // IDF
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.CommentHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.DateOfExperimentHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalDesignHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalDesignTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalDesignTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalFactorNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalFactorTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalFactorTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentalFactorTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ExperimentDescriptionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.InvestigationTitleHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.MAGETABVersionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.NormalizationTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.NormalizationTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.NormalizationTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAddressHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAffiliationHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonEmailHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonFaxHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonFirstNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonLastNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonMidInitialsHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonPhoneHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonRolesHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonRolesTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonRolesTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolContactHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolDescriptionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolHardwareHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolParametersHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolSoftwareHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ProtocolTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationAuthorListHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationDoiHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationStatusHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationStatusTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationStatusTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicationTitleHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PublicReleaseDateHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PubMedIdHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.QualityControlTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.QualityControlTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.QualityControlTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ReplicateTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ReplicateTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.ReplicateTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.SDRFFileHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.TermSourceFileHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.TermSourceNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.TermSourceVersionHandler.class);
    // SDRF
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.SourceHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.SampleHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ExtractHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.LabeledExtractHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.FactorValueNodeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ScanHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.NormalizationHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ArrayDataHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ArrayDataMatrixHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ImageHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.ProtocolHandler.class);
    // ADF header
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.AccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.ArrayDesignNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.CommentHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.PrintingProtocolHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.ProviderHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SequencePolymerTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SequencePolymerTypeTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SequencePolymerTypeTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SubstrateTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SubstrateTypeTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SubstrateTypeTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SurfaceTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SurfaceTypeTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.SurfaceTypeTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TechnologyTypeHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TechnologyTypeTermAccessionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TechnologyTypeTermSourceRefHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TermSourceFileHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TermSourceNameHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TermSourceVersionHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.VersionHandler.class);
    //ADF graph
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.FeatureHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.ReporterHandler.class);
    handlerClasses.add(
        uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.CompositeElementHandler.class);


    // clear current handler classes
    idfHandlerClasses.clear();
    idfHandlerMap.clear();
    idfHandlerStore.clear();
    sdrfHandlerClasses.clear();
    sdrfHandlerMap.clear();
    sdrfHandlerStore.clear();
    adfHeaderHandlerClasses.clear();
    adfHeaderHandlerMap.clear();
    adfHeaderHandlerStore.clear();
    adfGraphHandlerClasses.clear();
    adfGraphHandlerMap.clear();
    adfGraphHandlerStore.clear();

    // add new ones
    addHandlerClasses(handlerClasses);
  }

  /**
   * Set the handler classes that can be used by this pool.  This set shold be
   * comprehensive: if you use this method to set up the handlers attached to
   * this pool and a tag is encountered that cannot be handled by the handlers
   * you supply, it will be ignored.
   * <p/>
   * All classes supplied here should have default constructors, as handlers are
   * instantiated on demand when requested.
   *
   * @param handlerClasses the set of handler classes to be supplied to this
   *                       pool.  They will be instantiated on demand.
   */
  public void setHandlerClasses(Set<Class<? extends Handler>> handlerClasses) {
    // clear current handler classes
    idfHandlerClasses.clear();
    idfHandlerMap.clear();
    idfHandlerStore.clear();
    sdrfHandlerClasses.clear();
    sdrfHandlerMap.clear();
    sdrfHandlerStore.clear();
    adfHeaderHandlerClasses.clear();
    adfHeaderHandlerMap.clear();
    adfHeaderHandlerStore.clear();
    adfGraphHandlerClasses.clear();
    adfGraphHandlerMap.clear();
    adfGraphHandlerStore.clear();

    // add new ones
    addHandlerClasses(handlerClasses);
  }

  /**
   * Get the set of handler classes currently configured for this handler pool.
   * There may or may not be instances of these handlers currently available
   * from this pool.
   *
   * @return the set of handler classes that are currently configured on this
   *         handler pool
   */
  public Set<Class<? extends Handler>> getHandlerClasses() {
    Set<Class<? extends Handler>> response =
        new HashSet<Class<? extends Handler>>();

    response.addAll(idfHandlerClasses);
    response.addAll(sdrfHandlerClasses);
    response.addAll(adfHeaderHandlerClasses);
    response.addAll(adfGraphHandlerClasses);

    return response;
  }

  /**
   * In the handler pool, replace the current handler with a new one.  This is
   * useful if you want to provide additional functionality for a small number
   * of handlers, replacing them with new implementations, without having to
   * redefine all the default handlers.
   * <p/>
   * All the default handlers, as set up by {@link #useDefaultHandlers()}, will
   * be used, but the currentHandler will be removed from the pool and replaced
   * with the new class supplied.  Again, the handler class supplied should
   * include a default constructor as this class will be instantiated on demand
   * as requested.
   * <p/>
   * This method returns true if the replacement operation is successful and
   * false otherwise.  If the current handler is either not configured or the
   * new handler cannot be assigned to the IDFHandler listing or SDRFHandler
   * listing, then it may not be possible to complete the replacement.
   *
   * @param currentHandler the handler that is currently configured
   * @param newHandler     the handler you want to replace the current handler
   *                       with
   * @return true if the replacement succeeds, false otherwise
   */
  public synchronized boolean replaceHandlerClass(
      Class<? extends Handler> currentHandler,
      Class<? extends Handler> newHandler) {
    boolean replaced = false;

    if (idfHandlerClasses.contains(currentHandler)) {
      if (IDFHandler.class.isAssignableFrom(newHandler)) {
        log.debug("Replacing IDFHandler; " + currentHandler.getSimpleName() +
            " -> " + newHandler.getSimpleName());
        idfHandlerClasses.remove(currentHandler);
        idfHandlerClasses.add((Class<IDFHandler>) newHandler);
        replaced = true;
      }
    }
    // returns true if idf handler was replaced
    if (!replaced) {
      // continue if idf handler wasn't replaced
      if (sdrfHandlerClasses.contains(currentHandler)) {
        if (SDRFHandler.class.isAssignableFrom(newHandler)) {
          log.debug("Replacing SDRFHandler; " + currentHandler.getSimpleName() +
              " -> " + newHandler.getSimpleName());
          sdrfHandlerClasses.remove(currentHandler);
          sdrfHandlerClasses.add((Class<SDRFHandler>) newHandler);
          replaced = true;
        }
      }
    }
    // returns true if sdrf handler was replaced
    if (!replaced) {
      // continue if idf handler wasn't replaced
      if (adfHeaderHandlerClasses.contains(currentHandler)) {
        if (ADFHeaderHandler.class.isAssignableFrom(newHandler)) {
          log.debug(
              "Replacing ADFHeaderHandler; " + currentHandler.getSimpleName() +
                  " -> " + newHandler.getSimpleName());
          adfHeaderHandlerClasses.remove(currentHandler);
          adfHeaderHandlerClasses.add((Class<ADFHeaderHandler>) newHandler);
          replaced = true;
        }
      }
    }
    // returns true if adf header handler was replaced
    if (!replaced) {
      // continue if idf handler wasn't replaced
      if (adfGraphHandlerClasses.contains(currentHandler)) {
        if (ADFGraphHandler.class.isAssignableFrom(newHandler)) {
          log.debug(
              "Replacing ADFGraphHandler; " + currentHandler.getSimpleName() +
                  " -> " + newHandler.getSimpleName());
          adfGraphHandlerClasses.remove(currentHandler);
          adfGraphHandlerClasses.add((Class<ADFGraphHandler>) newHandler);
          replaced = true;
        }
      }
    }

    // if we've replaced classes, but we currently have cached instances,
    // we need to clear the relevant map and store

    // try IDF caches first
    if (idfHandlerMap.values().contains(currentHandler)) {
      // we have initialised the map, so find the key
      String keyToRemove = null;
      for (String key : idfHandlerMap.keySet()) {
        if (idfHandlerMap.get(key).equals(currentHandler)) {
          keyToRemove = key;
          break;
        }
      }
      if (keyToRemove != null) {
        // now, remove the key from the map.
        log.debug("Removing key " + keyToRemove + " from idfHandlerMap");
        idfHandlerMap.remove(keyToRemove);
        // also, remove and cached instances of the current handler
        if (idfHandlerStore.containsKey(currentHandler)) {
          log.debug("Removing key " + currentHandler.getSimpleName() +
              " from idfHandlerStore");
          idfHandlerStore.remove(currentHandler);
        }
      }
    }
    // now try SDRF caches
    if (sdrfHandlerMap.values().contains(currentHandler)) {
      // we have initialised the map, so find the key
      String keyToRemove = null;
      for (String key : sdrfHandlerMap.keySet()) {
        if (sdrfHandlerMap.get(key).equals(currentHandler)) {
          keyToRemove = key;
          break;
        }
      }
      if (keyToRemove != null) {
        // now, remove the key from the map.
        log.debug("Removing key " + keyToRemove + " from sdrfHandlerMap");
        sdrfHandlerMap.remove(keyToRemove);
        // also, remove and cached instances of the current handler
        if (sdrfHandlerStore.containsKey(currentHandler)) {
          log.debug("Removing key " + currentHandler.getSimpleName() +
              " from sdrfHandlerStore");
          sdrfHandlerStore.remove(currentHandler);
        }
      }
    }
    // now try ADF header caches
    if (adfHeaderHandlerMap.values().contains(currentHandler)) {
      // we have initialised the map, so find the key
      String keyToRemove = null;
      for (String key : adfHeaderHandlerMap.keySet()) {
        if (adfHeaderHandlerMap.get(key).equals(currentHandler)) {
          keyToRemove = key;
          break;
        }
      }
      if (keyToRemove != null) {
        // now, remove the key from the map.
        log.debug("Removing key " + keyToRemove + " from adfHeaderHandlerMap");
        adfHeaderHandlerMap.remove(keyToRemove);
        // also, remove and cached instances of the current handler
        if (adfHeaderHandlerStore.containsKey(currentHandler)) {
          log.debug("Removing key " + currentHandler.getSimpleName() +
              " from adfHeaderHandlerStore");
          adfHeaderHandlerStore.remove(currentHandler);
        }
      }
    }
    // now try ADF graph caches
    if (adfGraphHandlerMap.values().contains(currentHandler)) {
      // we have initialised the map, so find the key
      String keyToRemove = null;
      for (String key : adfGraphHandlerMap.keySet()) {
        if (adfGraphHandlerMap.get(key).equals(currentHandler)) {
          keyToRemove = key;
          break;
        }
      }
      if (keyToRemove != null) {
        // now, remove the key from the map.
        log.debug("Removing key " + keyToRemove + " from adfGraphHandlerMap");
        adfGraphHandlerMap.remove(keyToRemove);
        // also, remove and cached instances of the current handler
        if (adfGraphHandlerStore.containsKey(currentHandler)) {
          log.debug("Removing key " + currentHandler.getSimpleName() +
              " from adfGraphHandlerStore");
          adfGraphHandlerStore.remove(currentHandler);
        }
      }
    }

    // returns true if handler was replaced,
    // else false if we didn't replace anything
    return replaced;
  }

  /**
   * Request an IDF Handler, acquiring one from the pool if possible or
   * otherwise lazily instantiating a new one.  The handler will be
   * preconfigured with the passed variables, and is therefore ready to call
   * handler.handle() on immediately.  Once handling has completed, you should
   * return this handler by calling HandlerPool.relinquishHandler().
   * <p/>
   * Requesting a handler for a tag that cannot be handled (because there is no
   * handler configured) results in a {@link uk.ac.ebi.arrayexpress2.magetab.exception.ParseException}
   * being thrown.  This can be safely ignored: the file can still be read, but
   * the current tag cannot and will not be read
   *
   * @param tag               the tag (i.e. the IDF header) for the data that
   *                          the new handler should be able to handle
   * @param lineData          the actual data this handler will handle
   * @param investigation     the investigation this handler will parse its data
   *                          into
   * @param taskNumber        the index this handler should use when updating
   *                          the progress of the investigation
   * @param progressIncrement the total value this handler can increment the
   *                          progress by upon completion
   * @return a new fully configured handler, or null if there was no handler for
   *         this tag
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if a tag is encountered that cannot be handled.
   */
  public IDFHandler getIDFHandler(String tag,
                                  String lineData,
                                  MAGETABInvestigation investigation,
                                  int taskNumber,
                                  double progressIncrement)
      throws ParseException {
    IDFHandler handler = null;
    log.debug("Requesting handler for '" + tag + "', task index " + taskNumber);

    synchronized (idfHandlerMap) {
      String strippedTag = tag.contains("[")
          ? tag.substring(0, tag.indexOf("[") + 1).concat(
          tag.substring(tag.indexOf("]"), tag.length()))
          : tag;
      if (tag.equalsIgnoreCase("comment[arrayexpressaccession]")) {
        strippedTag = tag;
      }

      if (idfHandlerMap.containsKey(strippedTag)) {
        Class<? extends IDFHandler> handlerClass =
            idfHandlerMap.get(strippedTag);
        synchronized (idfHandlerStore) {
          List<? extends IDFHandler> handlers =
              idfHandlerStore.get(handlerClass);

          if (handlers != null && handlers.size() > 0) {
            for (IDFHandler next : handlers) {
              if (next.canHandle(tag)) {
                log.debug("Using pre-instantiated handler, " + next.toString());
                handler = next;
              }
            }
          }
          else {
            try {
              log.debug("Instantiating new handler for " + tag +
                  ", new handler of known type needed");
              handler = handlerClass.newInstance();
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }

          // must remove from handler store before returning,
          // otherwise could be used more than once
          idfHandlerStore.get(handlerClass).remove(handler);
        }
      }
      else {
        synchronized (idfHandlerClasses) {
          List<Class<? extends IDFHandler>> handlerClasses =
              new ArrayList<Class<? extends IDFHandler>>();
          handlerClasses.addAll(idfHandlerClasses);

          // no reusable handler, so instantiate next handler and check
          for (Class<? extends IDFHandler> handlerClass : handlerClasses) {
            try {
              // keep instantiating handlers till we get the right one!
              log.debug("Instantiating new handler for " + tag +
                  ", never before seen");
              handler = handlerClass.newInstance();

              // setup the handler properly and add to the map
              idfHandlerMap.put(handler.handlesTag(), handler.getClass());
              // also add a key to the store, if there isn't one already
              if (!idfHandlerStore.containsKey(handler.getClass())) {
                idfHandlerStore
                    .put(handler.getClass(), new ArrayList<IDFHandler>());
              }

              // remove from the class list - this doesn't need instantiating again
              idfHandlerClasses.remove(handler.getClass());

              if (handler.canHandle(tag)) {
                // we have the right handler, so break
                break;
              }
              else {
                // this isn't the right sort of handler, so store it
                synchronized (idfHandlerStore) {
                  idfHandlerStore.get(handler.getClass()).add(handler);
                }

                // set handler to null, because this one was wrong
                handler = null;
              }
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }
        }
      }

      // we should have a handler, unless no classes match - in which case we can't handle this tag
      if (handler == null) {
        String message = "Ignoring tag '" + tag + "', " +
            "this header cannot be read in the current configuration";

        // log a warning - we'll ignore this tag
        log.warn(message);

        // update the status of this task so we don't wait for it
        investigation.IDF.increaseProgressBy(progressIncrement);
        investigation.IDF.updateTaskList(taskNumber, Status.COMPLETE);

        // throw non-critical exception
        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, ErrorCode.UNKNOWN_IDF_HEADING, this.getClass());

        throw new ParseException(error, false);
      }
      // calibrate the handler with specified params
      log.debug("Calibrating handler with required parameters...");
      // set references
      handler.setInvestigation(investigation);
      log.debug(
          "Adding data (" + lineData + ") to handler [" + handler.toString() +
              "]");
      handler.setData(lineData);
      // set progress indices
      handler.setTaskIndex(taskNumber);
      handler.increasesProgressBy(progressIncrement);

      // and return
      return handler;
    }
  }

  /**
   * Return a handler that is finished with to the pool.  Note that the state of
   * this handler will be reset once it is relinquished, so if you have any
   * references to values in the handler that you subsequently use, you may
   * encounter NullPointerExceptions.
   *
   * @param handler the handler to relinquish
   */
  public void relinquishIDFHandler(IDFHandler handler) {
//    // reset references
//    handler.setObjectBag(null);
//    handler.setInvestigation(null);
//    handler.setData(null);
//    // unset progress indicies
//    handler.setTaskIndex(-1);
//    handler.increasesProgressBy(0);
//
//    // return to the store
//    if (idfHandlerStore.containsKey(handler.getClass())) {
//      idfHandlerStore.get(handler.getClass()).add(handler);
//    }
//    else {
//      List<IDFHandler> list = new ArrayList<IDFHandler>();
//      list.add(handler);
//      idfHandlerStore.put(handler.getClass(), list);
//    }
  }

  /**
   * Request an SDRF Handler, acquiring one from the pool if possible or
   * otherwise lazily instantiating a new one.  The handler will be
   * preconfigured with the passed variables, and is therefore ready to call
   * handler.handle() on immediately.  Once handling has completed, you should
   * return this handler by calling HandlerPool.relinquishHandler().
   *
   * @param tag               the tag (i.e. the IDF header) for the data that
   *                          the new handler should be able to handle
   * @param headerData        the headers for the block of data being handled
   * @param rowData           the raw data in the section of the row being
   *                          handled
   * @param investigation     the investigation this handler will parse its data
   *                          into
   * @param taskNumber        the index this handler should use when updating
   *                          the progress of the investigation
   * @param progressIncrement the total value this handler can increment the
   *                          progress by upon completion
   * @return a new fully configured handler, or null if there was no handler for
   *         this tag
   */
  public SDRFHandler getSDRFHandler(String tag,
                                    String[] headerData,
                                    String[] rowData,
                                    MAGETABInvestigation investigation,
                                    int taskNumber,
                                    double progressIncrement) {
    SDRFHandler handler = null;

    synchronized (sdrfHandlerMap) {
      String strippedTag = tag.contains("[")
          ? tag.substring(0, tag.indexOf("[") + 1).concat(
          tag.substring(tag.indexOf("]"), tag.length()))
          : tag;
      if (sdrfHandlerMap.containsKey(strippedTag)) {
        log.debug("Stripped tag for handler lookup - type-free tag is " +
            strippedTag);

        Class<? extends SDRFHandler> handlerClass =
            sdrfHandlerMap.get(strippedTag);
        synchronized (sdrfHandlerStore) {
          List<? extends SDRFHandler> handlers =
              sdrfHandlerStore.get(handlerClass);

          if (handlers != null && handlers.size() > 0) {
            log.debug("Found pre-instantiated handler for " + strippedTag +
                ", reusing it");

            handler = handlers.get(0);
            handlers.remove(handler);
          }
          else {
            try {
              log.debug("Instantiating new handler for " + tag +
                  ", new handler of known type needed");
              handler = handlerClass.newInstance();
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }

          // must remove from handler store before returning,
          // otherwise could be used more than once
          sdrfHandlerStore.get(handlerClass).remove(handler);
        }
      }
      else {
        synchronized (sdrfHandlerClasses) {
          List<Class<? extends SDRFHandler>> handlerClasses =
              new ArrayList<Class<? extends SDRFHandler>>();
          handlerClasses.addAll(sdrfHandlerClasses);

          log.debug(strippedTag +
              " hasn't been seen before, looking for new handlers");

          // no reusable handler, so instantiate next handler and check
          for (Class<? extends SDRFHandler> handlerClass : handlerClasses) {
            try {
              // keep instantiating handlers till we get the right one!
              log.debug("Instantiating new handler for " + tag +
                  ", never before seen");
              handler = handlerClass.newInstance();

              // setup the handler properly and add to the map
              sdrfHandlerMap.put(handler.handlesTag(), handler.getClass());
              // also add a key to the store, if there isn't one already
              if (!sdrfHandlerStore.containsKey(handler.getClass())) {
                sdrfHandlerStore
                    .put(handler.getClass(), new ArrayList<SDRFHandler>());
              }

              if (handler.canHandle(tag)) {
                // we have the right handler, so break
                break;
              }
              else {
                // this isn't the right sort of handler, so store it
                synchronized (sdrfHandlerStore) {
                  sdrfHandlerStore.get(handler.getClass()).add(handler);
                }

                // set handler to null, because this one was wrong
                handler = null;
              }
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }
        }
      }

      if (handler == null) {
        log.debug(
            "No handler for tag '" + tag + "', so configuring an" +
                " IgnoredHeaderHandler as a placeholder");
        handler =
            new uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.IgnoredHeaderHandler();
      }

      // calibrate the handler with specified params
      // set references
      handler.setInvestigation(investigation);
      handler.setData(headerData, rowData);
      // set progress indices
      handler.setTaskIndex(taskNumber);
      handler.increasesProgressBy(progressIncrement);


      log.debug("Returning handler for " + tag + ", " +
          "type: " + handler.getClass().getSimpleName());

      // and return
      return handler;
    }
  }

  /**
   * Return a handler that is finished with to the pool.  Note that the state of
   * this handler will be reset once it is relinquished, so if you have any
   * references to values in the handler that you subsequently use, you may
   * encounter NullPointerExceptions.
   *
   * @param handler the handler to relinquish
   */
  public void relinquishSDRFHandler(SDRFHandler handler) {
    // reset references
//    handler.setObjectBag(null);
//    handler.setInvestigation(null);
//    handler.setData(null, null);
//    // set progres indicies
//    handler.setTaskIndex(-1);
//    handler.increasesProgressBy(0);
//
//    // return to the store
//    if (sdrfHandlerStore.containsKey(handler.getClass())) {
//      sdrfHandlerStore.get(handler.getClass()).add(handler);
//    }
//    else {
//      List<SDRFHandler> list = new ArrayList<SDRFHandler>();
//      list.add(handler);
//      sdrfHandlerStore.put(handler.getClass(), list);
//    }
  }

  /**
   * Request an IDF Handler, acquiring one from the pool if possible or
   * otherwise lazily instantiating a new one.  The handler will be
   * preconfigured with the passed variables, and is therefore ready to call
   * handler.handle() on immediately.  Once handling has completed, you should
   * return this handler by calling HandlerPool.relinquishHandler().
   * <p/>
   * Requesting a handler for a tag that cannot be handled (because there is no
   * handler configured) results in a {@link uk.ac.ebi.arrayexpress2.magetab.exception.ParseException}
   * being thrown.  This can be safely ignored: the file can still be read, but
   * the current tag cannot and will not be read
   *
   * @param tag               the tag (i.e. the IDF header) for the data that
   *                          the new handler should be able to handle
   * @param lineData          the actual data this handler will handle
   * @param arrayDesign       the investigation this handler will parse its data
   *                          into
   * @param taskNumber        the index this handler should use when updating
   *                          the progress of the investigation
   * @param progressIncrement the total value this handler can increment the
   *                          progress by upon completion
   * @return a new fully configured handler, or null if there was no handler for
   *         this tag
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if a tag is encountered that cannot be handled.
   */
  public ADFHeaderHandler getADFHeaderHandler(String tag,
                                              String lineData,
                                              MAGETABArrayDesign arrayDesign,
                                              int taskNumber,
                                              double progressIncrement)
      throws ParseException {
    ADFHeaderHandler handler = null;
    log.debug("Requesting handler for '" + tag + "', task index " + taskNumber);

    synchronized (adfHeaderHandlerMap) {
      String strippedTag = tag.contains("[")
          ? tag.substring(0, tag.indexOf("[") + 1).concat(
          tag.substring(tag.indexOf("]"), tag.length()))
          : tag;
      if (tag.equalsIgnoreCase("comment[arrayexpressaccession]")) {
        strippedTag = tag;
      }

      if (adfHeaderHandlerMap.containsKey(strippedTag)) {
        Class<? extends ADFHeaderHandler> handlerClass =
            adfHeaderHandlerMap.get(strippedTag);
        synchronized (adfHeaderHandlerStore) {
          List<? extends ADFHeaderHandler> handlers =
              adfHeaderHandlerStore.get(handlerClass);

          if (handlers != null && handlers.size() > 0) {
            for (ADFHeaderHandler next : handlers) {
              if (next.canHandle(tag)) {
                log.debug("Using pre-instantiated handler, " + next.toString());
                handler = next;
              }
            }
          }
          else {
            try {
              log.debug("Instantiating new handler for " + tag +
                  ", new handler of known type needed");
              handler = handlerClass.newInstance();
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }

          // must remove from handler store before returning,
          // otherwise could be used more than once
          adfHeaderHandlerStore.get(handlerClass).remove(handler);
        }
      }
      else {
        synchronized (adfHeaderHandlerClasses) {
          List<Class<? extends ADFHeaderHandler>> handlerClasses =
              new ArrayList<Class<? extends ADFHeaderHandler>>();
          handlerClasses.addAll(adfHeaderHandlerClasses);

          // no reusable handler, so instantiate next handler and check
          for (Class<? extends ADFHeaderHandler> handlerClass : handlerClasses) {
            try {
              // keep instantiating handlers till we get the right one!
              log.debug("Instantiating new handler for " + tag +
                  ", never before seen");
              handler = handlerClass.newInstance();

              // setup the handler properly and add to the map
              adfHeaderHandlerMap.put(handler.handlesTag(), handler.getClass());
              // also add a key to the store, if there isn't one already
              if (!adfHeaderHandlerStore.containsKey(handler.getClass())) {
                adfHeaderHandlerStore
                    .put(handler.getClass(), new ArrayList<ADFHeaderHandler>());
              }

              // remove from the class list - this doesn't need instantiating again
              adfHeaderHandlerClasses.remove(handler.getClass());

              if (handler.canHandle(tag)) {
                // we have the right handler, so break
                break;
              }
              else {
                // this isn't the right sort of handler, so store it
                synchronized (adfHeaderHandlerStore) {
                  adfHeaderHandlerStore.get(handler.getClass()).add(handler);
                }

                // set handler to null, because this one was wrong
                handler = null;
              }
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }
        }
      }

      // we should have a handler, unless no classes match - in which case we can't handle this tag
      if (handler == null) {
        String message = "Ignoring tag '" + tag + "', " +
            "this header cannot be read in the current configuration";

        // log a warning - we'll ignore this tag
        log.warn(message);

        // update the status of this task so we don't wait for it
        arrayDesign.ADF.increaseProgressBy(progressIncrement);
        arrayDesign.ADF.updateTaskList(taskNumber, Status.COMPLETE);

        // throw non-critical exception
        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        throw new ParseException(error, false);
      }
      // calibrate the handler with specified params
      log.debug("Calibrating handler with required parameters...");
      // set references
      handler.setArrayDesign(arrayDesign);
      log.debug(
          "Adding data (" + lineData + ") to handler [" + handler.toString() +
              "]");
      handler.setData(lineData);
      // set progress indicies
      handler.setTaskIndex(taskNumber);
      handler.increasesProgressBy(progressIncrement);

      // and return
      return handler;
    }
  }

  /**
   * Return a handler that is finished with to the pool.  Note that the state of
   * this handler will be reset once it is relinquished, so if you have any
   * references to values in the handler that you subsequently use, you may
   * encounter NullPointerExceptions.
   *
   * @param handler the handler to relinquish
   */
  public void relinquishADFHeaderHandler(ADFHeaderHandler handler) {
  }

  /**
   * Request an ADFGraph Handler, acquiring one from the pool if possible or
   * otherwise lazily instantiating a new one.  The handler will be
   * preconfigured with the passed variables, and is therefore ready to call
   * handler.handle() on immediately.  Once handling has completed, you should
   * return this handler by calling HandlerPool.relinquishHandler().
   *
   * @param tag               the tag (i.e. the IDF header) for the data that
   *                          the new handler should be able to handle
   * @param headerData        the headers for the block of data being handled
   * @param rowData           the raw data in the section of the row being
   *                          handled
   * @param arrayDesign       the investigation this handler will parse its data
   *                          into
   * @param taskNumber        the index this handler should use when updating
   *                          the progress of the investigation
   * @param progressIncrement the total value this handler can increment the
   *                          progress by upon completion
   * @return a new fully configured handler, or null if there was no handler for
   *         this tag
   */
  public ADFGraphHandler getADFGraphHandler(String tag,
                                            String[] headerData,
                                            String[] rowData,
                                            MAGETABArrayDesign arrayDesign,
                                            int taskNumber,
                                            double progressIncrement) {
    ADFGraphHandler handler = null;

    synchronized (adfGraphHandlerMap) {
      String strippedTag = tag.contains("[")
          ? tag.substring(0, tag.indexOf("[") + 1).concat(
          tag.substring(tag.indexOf("]"), tag.length()))
          : tag;
      if (adfGraphHandlerMap.containsKey(strippedTag)) {
        log.debug("Stripped tag for handler lookup - type-free tag is " +
            strippedTag);

        Class<? extends ADFGraphHandler> handlerClass =
            adfGraphHandlerMap.get(strippedTag);
        synchronized (adfGraphHandlerStore) {
          List<? extends ADFGraphHandler> handlers =
              adfGraphHandlerStore.get(handlerClass);

          if (handlers != null && handlers.size() > 0) {
            log.debug("Found pre-instantiated handler for " + strippedTag +
                ", reusing it");

            handler = handlers.get(0);
            handlers.remove(handler);
          }
          else {
            try {
              log.debug("Instantiating new handler for " + tag +
                  ", new handler of known type needed");
              handler = handlerClass.newInstance();
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }

          // must remove from handler store before returning,
          // otherwise could be used more than once
          adfGraphHandlerStore.get(handlerClass).remove(handler);
        }
      }
      else {
        synchronized (adfGraphHandlerClasses) {
          List<Class<? extends ADFGraphHandler>> handlerClasses =
              new ArrayList<Class<? extends ADFGraphHandler>>();
          handlerClasses.addAll(adfGraphHandlerClasses);

          log.debug(strippedTag +
              " hasn't been seen before, looking for new handlers");

          // no reusable handler, so instantiate next handler and check
          for (Class<? extends ADFGraphHandler> handlerClass : handlerClasses) {
            try {
              // keep instantiating handlers till we get the right one!
              log.debug("Instantiating new handler for " + tag +
                  ", never before seen");
              handler = handlerClass.newInstance();

              // setup the handler properly and add to the map
              adfGraphHandlerMap.put(handler.handlesTag(), handler.getClass());
              // also add a key to the store, if there isn't one already
              if (!adfGraphHandlerStore.containsKey(handler.getClass())) {
                adfGraphHandlerStore
                    .put(handler.getClass(), new ArrayList<ADFGraphHandler>());
              }

              if (handler.canHandle(tag)) {
                // we have the right handler, so break
                break;
              }
              else {
                // this isn't the right sort of handler, so store it
                synchronized (adfGraphHandlerStore) {
                  adfGraphHandlerStore.get(handler.getClass()).add(handler);
                }

                // set handler to null, because this one was wrong
                handler = null;
              }
            }
            catch (IllegalAccessException e) {
              throw new NullPointerException(
                  "Could not open handler class " + handlerClass.getName());
            }
            catch (InstantiationException e) {
              throw new NullPointerException(
                  "Unable to instantiate handler class " +
                      handlerClass.getName() +
                      "; this may be due to a non-default constructor?");
            }
          }
        }
      }

      if (handler == null) {
        log.debug(
            "No handler for tag '" + tag + "', so configuring an" +
                " IgnoredHeaderHandler as a placeholder");
        handler =
            new uk.ac.ebi.arrayexpress2.magetab.handler.adf.IgnoredHeaderHandler();
      }

      // calibrate the handler with specified params
      // set references
      handler.setArrayDesign(arrayDesign);
      handler.setData(headerData, rowData);
      // set progress indices
      handler.setTaskIndex(taskNumber);
      handler.increasesProgressBy(progressIncrement);


      log.debug("Returning handler for " + tag + ", " +
          "type: " + handler.getClass().getSimpleName());

      // and return
      return handler;
    }
  }


  private void addHandlerClasses(Set<Class<? extends Handler>> handlerClasses) {
    for (Class<? extends Handler> handlerClass : handlerClasses) {
      if (IDFHandler.class.isAssignableFrom(handlerClass)) {
        idfHandlerClasses.add((Class<? extends IDFHandler>) handlerClass);
      }
      else if (SDRFHandler.class.isAssignableFrom(handlerClass)) {
        sdrfHandlerClasses.add((Class<? extends SDRFHandler>) handlerClass);
      }
      else if (ADFHeaderHandler.class.isAssignableFrom(handlerClass)) {
        adfHeaderHandlerClasses.add(
            (Class<? extends ADFHeaderHandler>) handlerClass);
      }
      else if (ADFGraphHandler.class.isAssignableFrom(handlerClass)) {
        adfGraphHandlerClasses.add(
            (Class<? extends ADFGraphHandler>) handlerClass);
      }
      else {
        throw new IllegalArgumentException("Handler classes must be either " +
            "IDF or SDRF handler implementations");
      }
    }
  }
}
