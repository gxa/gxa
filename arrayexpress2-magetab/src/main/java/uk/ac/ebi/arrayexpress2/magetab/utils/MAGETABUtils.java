package uk.ac.ebi.arrayexpress2.magetab.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some common utilities for working with MAGE-TAB files.
 *
 * @author Tony Burdett
 * @date 30-Jan-2009
 */
public class MAGETABUtils {
  private static final Log log = LogFactory.getLog(MAGETABUtils.class);

  /**
   * Determine whether the string supplied ends with an escaped newline
   * character.  If the line does not end with a newline character, it is
   * assumed to have been previously removed, and therefore this will determine
   * if the line ends "within" a quote.
   *
   * @param line the line to inspect
   * @return true if the string ends "within" a quote, false otherwise
   */
  public static boolean endsWithEscapedNewline(String line) {
    // split line into cells
    String[] cells = line.split("\t");

    // counting back from last cell...
    int i = cells.length - 1;
    String nextCell = cells[i];

    while (i > -1) {
      if (nextCell.endsWith("\"")) {
        // if it ends with a quote, we're no longer escaped
        return false;
      }
      else if (nextCell.startsWith("\"")) {
        // if it starts with a quote, we're defo escaped
        return true;
      }
      else {
        // nextCell neither ends or starts with newline, so check previous cell for context
        nextCell = cells[i--];
      }
    }

    // if we got to here, it means we reached first cell and none are escaped
    return false;
  }

  /**
   * Compensate for a newline character that has been inserted into a single
   * 'logical' line in a MAGETAB file and then escaped.  This form of escaping
   * is legal in the MAGETAB specification, but obviously extremely unhelpful
   * for automated parsing, as one "logical" line in the file could potentially
   * be spread across several actual lines in the file, with quotes around the
   * newline character meant to indicate escaping.  This method takes two lines,
   * which have been judged to be separated by an escaped newline character (see
   * {@link #endsWithEscapedNewline(String)}) and then concatenates the two
   * strings.  Note that it will first check whether the first line does indeed
   * end in an escaped newline, and if not it will throw an exception. However,
   * there are situations when this will fail. When parsing a single line,
   * whether this line ends in an escaped newline character is judged by
   * checking each tab-delimited cell and if there are cells that start with a
   * quote and do NOT end in a quote before the end of the line, this line is
   * assumed to end in an escaped newline character.  If there are consecutive
   * lines ending in this way, however, every second line will look like this
   * yet will NOT end with an escaped newline.  Therefore, care must be taken to
   * remove all escaped newline characters in order, concatenating each string
   * as you parse, before calling this method.
   *
   * @param firstLine  the first line, i.e. the one ending with an escaped
   *                   newline character
   * @param secondLine the second line, i.e. the one that is a logical
   *                   continuation of the first
   * @return the result string, with the newline character removed
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the firstLine supplied does not end in an escaped newline
   *          character
   */
  public static String compensateForEscapedNewlines(String firstLine,
                                                    String secondLine)
      throws ParseException {
    if (endsWithEscapedNewline(firstLine)) {
      return firstLine.concat(secondLine);
    }
    else {
      // if the first line doesn't end in an escaped newline, you've done something daft
      String message = "A line was supplied that did not end in an escaped " +
          "newline character.  The line was: " + firstLine;

      ErrorItem error = ErrorItemFactory
          .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
          .generateErrorItem(
              message, 30, MAGETABUtils.class);

      throw new ParseException(error, true, message);
    }
  }

  /**
   * Simply removes any escaping present in the given line.  In MAGETAB,
   * quotation marks are used to surround some values, and this signifies that
   * they should be used as part of the value supplied; this includes tabs,
   * newlines and other such characters.  This method removes these characters,
   * replacing tabs and other whitespace with a single space.  Note that this
   * method will not work with lines that end in an escaped newline character,
   * and you should explicitly remove these characters first using the {@link
   * #endsWithEscapedNewline(String)} and {@link #compensateForEscapedNewlines(String,
   * String)} methods first.
   *
   * @param escapedLine the preprocessed line, that may contain segments escaped
   *                    with quotation marks
   * @return an unescaped line, with tab characters replaced with single spaces
   *         and quotes removed
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the escapedLine supplied ends in an escaped newline character,
   *          as we don't have the rest of the "logical" line to read
   */
  public static String unescapeLine(String escapedLine) throws ParseException {
    try {
      // this line should not end in an escaped newline, but could contain other whitespace chars
      if (!endsWithEscapedNewline(escapedLine)) {
        // regex to find quoted substrings
        Pattern p = Pattern.compile("\"[^\"\\r\\n]*\"");

        Matcher m = p.matcher(escapedLine);
        while (m.find()) {
          MatchResult mr = m.toMatchResult();
          // replace tabs in this segment with spaces
          String origSegment = mr.group();
          String newSegment = origSegment.replaceAll("\t", " ");
          // now replace the matching segment of the escapedLine with the new bit
          escapedLine = escapedLine.replaceFirst(origSegment, newSegment);
        }
        // finally, remove all quotes
        return escapedLine.replaceAll("\"", "");
      }
      else {
        // if the idf wasn't found, throw and exception
        String message = "A line was supplied that ended in an escaped " +
            "newline character, you should fix this before attempting to " +
            "unescape other characters.";

        ErrorItem error = ErrorItemFactory
            .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
            .generateErrorItem(
                message, 30, MAGETABUtils.class);

        throw new ParseException(error, true, message);
      }
    }
    catch (Exception e) {
      // maybe PatternSyntaxException, probably due to string munging issues with HTML encoding
      String message = "An unexpected error occurred whilst attempting to " +
          "parse a line in this document, possibly due to irregular " +
          "HTML encoding.";

      ErrorItem error = ErrorItemFactory
          .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
          .generateErrorItem(
              message, 30, MAGETABUtils.class);

      throw new ParseException(error, true, message, e);
    }
  }

  public static String[] splitLine(String line, boolean ignoreEscaping)
      throws ParseException {
    if (!ignoreEscaping) {
      // split the line into cells
      String[] cells = line.split("\t", -1);
      List<String> logicalCells = new ArrayList<String>();

      StringBuffer sb = null;
      for (String cell : cells) {
        if (cell.startsWith("\"") && !cell.endsWith("\"")) {
          // if the cell starts with a quote but doesn't end with one, this is a new logical cell
          sb = new StringBuffer();
          sb.append(cell).append("\t");
        }
        else if (!cell.startsWith("\"") && cell.endsWith("\"")) {
          // if the cell ends with a quote but doesn't start with one, then append this to current logical cell
          if (sb != null) {
            sb.append(cell);
            String logicalCell = sb.toString();
            logicalCells.add(logicalCell);
          }
          else {
            String message = "Bad escaping - found a cell ending in a " +
                "quote without a prior cell starting with a quote";

            ErrorItem error = ErrorItemFactory
                .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
                .generateErrorItem(
                    message, 30, MAGETABUtils.class);

            throw new ParseException(error, true, message);
          }
        }
        else if (cell.startsWith("\"") && cell.endsWith("\"")) {
          logicalCells.add(cell.substring(1, cell.length() - 1));
        }
        else {
          // this cell both starts and ends with or without quotes
          logicalCells.add(cell);
        }
      }

      return logicalCells.toArray(new String[logicalCells.size()]);
    }
    else {
      return line.split("\t", -1);
    }
  }

  /**
   * Remove any unescaped or unparenthesised whitespace, and convert to lower
   * case.
   *
   * @param header the header to digest
   * @return the digested string
   */
  public static String digestHeader(String header) {
    if (header == null) {
      return "";
    }
    else {
      String main;
      String quals;

      // take the header and ignore anything after the first '['
      if (header.contains("[")) {
        // the main part is everything up to [
        main = header.substring(0, header.indexOf('['));
        // the qualifier is everything after [
        quals = header.substring(header.indexOf("["), header.indexOf("]") + 1);
      }
      else if (header.contains("(")) {
        // the main part is everything up to ( - there shouldn't be cases of this?
        main = header.substring(0, header.indexOf('('));
        // the qualifier is everything after (
        quals = header.substring(header.indexOf("("), header.indexOf(")") + 1);
      }
      else {
        main = header;
        quals = "";
      }

      // remove any trailing whitespace
      main = main.trim();
      // remove internal whitespace
      main = main.replaceAll("\\s", "");
      // remove quotes
      main = main.replaceAll("\"", "");
      // lowercase everything
      main = main.toLowerCase();
      // add any [] or () qualifiers
      header = main + quals;

      return header;
    }
  }

  /**
   * Given a directory that represents a MAGE-TAB document, locate the IDF file
   * it contains
   *
   * @param mageTabFile the directory representing the mage-tab document
   * @return the IDF file
   * @throws ParseException if the file could not be read, or if there was no
   *                        IDF file
   */
  public static File locateIDF(File mageTabFile) throws ParseException {
    // hand off children to idf and sdrf parsers
    for (File f : mageTabFile.listFiles()) {
      if (f.getName().endsWith(".idf") ||
          f.getName().endsWith(".idf.txt")) {
        return f;
      }
    }

    // if the idf wasn't found, throw and exception
    String message = "Could not determine the location of an IDF file " +
        " (*.idf, *.idf.txt) in directory " + mageTabFile.getName();

    ErrorItem error = ErrorItemFactory
        .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
        .generateErrorItem(
            message, ErrorCode.BAD_URI, MAGETABUtils.class);

    throw new ParseException(error, true, message);
  }

  /**
   * Given a directory that represents a MAGE-TAB document, locate the SDRF file
   * it contains
   *
   * @param mageTabFile the directory representing the mage-tab document
   * @return the SDRF file
   * @throws ParseException if the file could not be read, or if there was no
   *                        SDRF file
   */
  public static File locateSDRF(File mageTabFile) throws ParseException {
    for (File f : mageTabFile.listFiles()) {
      if (f.getName().endsWith(".sdrf") ||
          f.getName().endsWith(".sdrf.txt")) {
        return f;
      }
    }

    // if the sdrf wasn't found, throw and exception
    String message = "Could not determine the location of an SDRF file " +
        " (*.sdrf, *.sdrf.txt) in directory " + mageTabFile.getName();

    ErrorItem error =
        ErrorItemFactory
            .getErrorItemFactory(MAGETABUtils.class.getClassLoader())
            .generateErrorItem(
                message, ErrorCode.UNREADABLE_SDRF_FILE, MAGETABUtils.class);

    throw new ParseException(error, false, message);
  }

  /**
   * Extract a subset of an array from a larger array.  Elements from
   * <code>startIndex</code> to <code>endIndex</code> inclusive will be
   * extracted and returned as a new array.  The type of the array will be
   * preserved.  The start and index values are zero indexed and are inclusive:
   * in other words, the maximum range that can be extracted is from 0 to
   * array.length-1.  If the endIndex value is greater than this, the result
   * would be the same as using the {@link #extractRange(Object[], int)} form of
   * this method - it will extract to the end and no further.
   *
   * @param array      the array to extract a subset from
   * @param startIndex the first element to extract, zero indexed, inclusive
   * @param endIndex   the last element to extract, zero indexed, inclusive - if
   *                   this is greater than the length of the array this will
   *                   extract to the end of the array
   * @param <T>        the type of the array
   * @return an array comprising a subset of the elements in the original array
   */
  public static <T> T[] extractRange(T[] array, int startIndex, int endIndex) {
    if (endIndex >= array.length) {
      return extractRange(array, startIndex);
    }
    else {
      T[] response = (T[]) Array.newInstance(
          array.getClass().getComponentType(),
          endIndex - startIndex + 1);

      System.arraycopy(array, startIndex, response, 0, response.length);
      return response;
    }
  }

  /**
   * Extract a subset of an array from a larger array.  Elements from
   * <code>startIndex</code> to the end of the array, inclusive will be
   * extracted and returned as a new array.  The type of the array will be
   * preserved.
   *
   * @param array      the array to extract a subset from
   * @param startIndex the first element to extract, zero indexed, inclusive
   * @param <T>        the type of the array
   * @return an array comprising a subset of the elements in the original array
   */
  public static <T> T[] extractRange(T[] array, int startIndex) {
    if (startIndex > array.length) {
      return (T[]) Array.newInstance(array.getClass().getComponentType(), 0);
    }
    else {
      T[] response = (T[]) Array.newInstance(
          array.getClass().getComponentType(),
          array.length - startIndex);

      System.arraycopy(array, startIndex, response, 0, response.length);
      return response;
    }
  }

  public static String generateImplicitSDRFNodeID(SDRF sdrf, SDRFNode node) {
    String nodeName = "Inferred_" + node.getClass().getSimpleName() + "_";
    int count = 1;
    for (SDRFNode nextNode : sdrf.lookupNodes(node.getClass())) {
      if (nextNode.getNodeName().equals(nodeName + count)) {
        // this node was already created, try next one up
        count++;
      }
      else {
        // no node with this name, break
        break;
      }
    }

    return nodeName + count;
  }
}
