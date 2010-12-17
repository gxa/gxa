package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

/**
 * The interface for all handlers that can handle aspects of the ADF graph
 * structure.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public interface ADFGraphHandler extends ADFHandler {
  /**
   * The "name" describing the first node or attribute that this handler will
   * handle.  Once the handler has been initialized by calling {@link
   * #setData(String[], String[])}, this becomes the same as the values[0]
   * passed to this method.  Note that prior to this happening, this handler can
   * actually handle any node name.
   *
   * @return the tag for entries this handler will handle
   */
  String handlesName();

  /**
   * Configure this handler with the headers and the values being read.  These
   * string arrays must be the same length, else a parse exception may occur
   * when reading.  If the ordering changes from the file, then incorrect data
   * may be stored.
   *
   * @param headers the headers to read from
   * @param values  the values to read
   */
  void setData(String[] headers, String[] values);

  /**
   * A special type of handle-type method that will cause this handler to return
   * the index of the last header it can handle in the current set of headers.
   * This can be used to determine the start and finish indices that a handler
   * can read from/to.
   *
   * @return the last column index that can be read
   */
  int assess();
}
