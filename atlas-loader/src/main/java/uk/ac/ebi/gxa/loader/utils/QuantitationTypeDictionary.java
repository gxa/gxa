package uk.ac.ebi.gxa.loader.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A singleton class representing the dictionary of quantitation types.  You
 * should use the static {@link #getQTDictionary(ClassLoader)} factory method to
 * return the class that can be used to look up quantitation types in the
 * dictionary. Dictionaries can be calibrated by using the default set of terms,
 * or by adding your own set of quantitation types into a file on the current
 * classpath called "qttypes.properties".  This file should contain a listing of
 * the type name as the key, and the pipeline type as the value.
 * <p/>
 * Once loaded, terms can be looked up using the {@link #listQTTypes()} method,
 * which returns a list of every known quantitation type in the dictionary.
 * <p/>
 * This class should be used when parsing a MAGE-TAB format processed data file,
 * in order to determine those
 *
 * @author Tony Burdett
 * @date 24-Jul-2009
 */
public class QuantitationTypeDictionary {
  private static final Map<ClassLoader, QuantitationTypeDictionary> factories =
      new HashMap<ClassLoader, QuantitationTypeDictionary>();

  /**
   * Get the {@link QuantitationTypeDictionary} that can be used to lookup
   * quantitation type terms, or obtain a full list. Note that, because
   * qttypes.properties resources may be spread across different classloaders,
   * you are required to supply one here.  Normally <code>getClass().getClassLoader()</code>
   * will suffice to allow this factory to discover required resources, unless
   * you want to load errorcodes explicitly from e.g. an online resource.
   * <p/>
   * Always use this form of the method if there is a chance that you have an
   * extension component that uses a different classloader to this factory
   * class.  Otherwise, not all extension resources are guaranteed to be
   * discovered
   *
   * @param loader the ClassLoader that this factory uses to search for
   *               resources
   * @return the ErrorItemFactory that can find error codes using the given
   *         loader
   */
  public static QuantitationTypeDictionary getQTDictionary(
      ClassLoader loader) {
    if (factories.containsKey(loader)) {
      // got a factory for this classloader already
      return factories.get(loader);
    }
    else {
      // create a new factory for this loader
      QuantitationTypeDictionary factory =
          new QuantitationTypeDictionary(loader);
      factories.put(loader, factory);
      return factory;
    }
  }

  /**
   * Get the {@link QuantitationTypeDictionary} that can be used to lookup
   * quantitation type terms, or obtain a full list. This form of the method
   * uses any resources accessible from the classloader that loaded this class.
   * Note that qttypes.properties resources may be spread across different
   * classloaders, so if you are unsure whether resources are accessible from
   * the classloader that loaded this class use the parameterised version {@link
   * #getQTDictionary(ClassLoader)}.  Normally calling that form of the method
   * with <code>getClass().getClassLoader()</code> will suffice to allow this
   * factory to discover required resources, unless you want to load a
   * dictionary of quantitation types explicitly from e.g. an online resource.
   * <p/>
   * Be advised that you should <b>ONLY</b> use this form if you are sure that
   * the classloader that loaded this factory has access to all the resources
   * you require.  This will normally be true if you only require the core
   * dictionary of terms and have provided no extensions, and assuming that your
   * environment will not have resulted in this class being dynamically loaded.
   *
   * @return the ErrorItemFactory that can find error codes using the
   *         classloader for this class
   */
  public static QuantitationTypeDictionary getQTDictionary() {
    // get the loader for this class
    ClassLoader loader = QuantitationTypeDictionary.class.getClassLoader();

    // check set and reuse if possible
    synchronized (factories) {
      if (factories.containsKey(loader)) {
        // got a factory for this classloader already
        return factories.get(loader);
      }
      else {
        // create a new factory for this loader
        QuantitationTypeDictionary factory =
            new QuantitationTypeDictionary(loader);
        factories.put(loader, factory);
        return factory;
      }
    }
  }

  private Map<String, String> qtTypes;

  private QuantitationTypeDictionary(ClassLoader loader) {
    // initialize map
    qtTypes = new HashMap<String, String>();

    // load qtTypes from all property files on the classpath
    Log log = LogFactory.getLog(this.getClass());
    try {
      Enumeration<URL> resources = loader.getResources(
          "META-INF/magetab/qttypes.properties");
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        log.info(
            "Loading dictionary of Quantitation Types from " + url.toString());
        Properties props = new Properties();
        props.load(url.openStream());

        for (Object key : props.keySet()) {
          String qtType = MAGETABUtils.digestHeader(key.toString());
          String message = props.get(key).toString();

          if (!qtTypes.containsKey(qtType)) {
            log.debug("Adding custom error code " + qtType + " to known list");
            qtTypes.put(qtType, message);
          }
        }
      }
    }
    catch (IOException e) {
      log.error("Unable to access classpath resource to load " +
          "extension error codes, these will be ignored");
    }
  }

  /**
   * Returns a string array that contains every term in this dictionary.  This
   * array is sorted alphabetically and is guaranteed to contain no duplicates
   *
   * @return the full list of terms in the dictionary.
   */
  public String[] listQTTypes() {
    // keyset
    Set<String> types = qtTypes.keySet();
    List<String> list = new ArrayList<String>();
    list.addAll(types);

    // now order list
    Collections.sort(list);

    String[] sortedList = new String[list.size()];

    return list.toArray(sortedList);
  }

  /**
   * Does a lookup on the supplied term to determine whether it is present in
   * the dictionary or not
   *
   * @param term the term to lookup
   * @return true if this term is in the dictionary, false otherwise
   */
  public boolean lookupTerm(String term) {
    return qtTypes.containsKey(term);
  }
}
