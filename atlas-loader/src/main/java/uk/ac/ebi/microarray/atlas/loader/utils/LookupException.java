package uk.ac.ebi.microarray.atlas.loader.utils;

/**
 * An exception that is thrown whenever an attempt to retrieve an object from
 * the cache of objects fails unexpectedly.  This should <b>NOT</b> be thrown if
 * the object is not yet in the cache, rather it should be thrown if somehting
 * goes critically wrong whilst trying to query for an object that is in the
 * cache already.
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class LookupException extends Exception {
  public LookupException() {
    super();
  }

  public LookupException(String s) {
    super(s);
  }

  public LookupException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public LookupException(Throwable throwable) {
    super(throwable);
  }
}
