package uk.ac.ebi.microarray.atlas.loader.utils;

/**
 * todo: Javadocs go here!
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
