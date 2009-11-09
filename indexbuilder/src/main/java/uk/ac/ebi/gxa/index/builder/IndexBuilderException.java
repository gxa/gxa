package uk.ac.ebi.gxa.index.builder;

/**
 * An exception representing a failure to construct a SOLR index from the Atlas
 * 2 database. This may occur during building from scratch or updating, and can
 * wrap exceptions thrown either from the database access or from the SOLR
 * server.
 *
 * @author Tony Burdett
 * @date 22-Sept-2009
 */
public class IndexBuilderException extends Exception {

  public IndexBuilderException() {
    super();
  }

  public IndexBuilderException(String message, Throwable cause) {
    super(message, cause);
  }

  public IndexBuilderException(String message) {
    super(message);
  }

  public IndexBuilderException(Throwable cause) {
    super(cause);
  }

}
