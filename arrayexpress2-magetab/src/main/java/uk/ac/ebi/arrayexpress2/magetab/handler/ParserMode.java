package uk.ac.ebi.arrayexpress2.magetab.handler;

/**
 * Modes that {@link uk.ac.ebi.arrayexpress2.magetab.parser.Parser}s or {@link
 * Handler}s belonging to a parser can operate in.  READ_AND_WRITE should
 * normally be the default, whereas READ_ONLY and WRITE_ONLY designate only
 * these operations.
 *
 * @author Tony Burdett
 * @date 19-Feb-2009
 */
public enum ParserMode {
  /**
   * Indicates that a handler should run in read and write mode
   */
  READ_AND_WRITE,
  /**
   * Indicates that a handler should read from a file only
   */
  READ_ONLY,
  /**
   * Indicates that a handler should only write objects that have been
   * previously read
   */
  WRITE_ONLY
}
