package uk.ac.ebi.arrayexpress2.magetab.utils;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.ADF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Writes ADF format output to a Writer.
 *
 * @author Tony Burdett
 * @date 17-May-2010
 */
public class ADFWriter {
  private RenderingTimer timer;
  private boolean printStdout = false;

  /**
   * Writes out the ADF to the given writer, without printing any stdout.  This
   * is equivalent to <code>writeADF(adf, writer, false)</code>
   *
   * @param adf    the ADF to write
   * @param writer the writer to write this ADF to
   * @throws IOException if the writer refused to accept bytes, or the ADF was
   *                     formatted in such a way as to make writing impossible
   */
  public void writeADF(ADF adf, Writer writer) throws IOException {
    writeADF(adf, writer, false);
  }

  /**
   * Writes out the ADF to the given writer, specifying whether to write
   * information about progress to standard out.
   *
   * @param adf         the ADF to write
   * @param writer      the writer to write this ADF to
   * @param printStdout whether or not to print progress information to standard
   *                    out
   * @throws IOException if the writer refused to accept bytes, or the ADF was
   *                     formatted in such a way as to make writing impossible
   */
  public void writeADF(ADF adf, Writer writer, boolean printStdout)
      throws IOException {
    try {
      this.printStdout = printStdout;

      // determine the ADF format to write out
      ADFFormat format = new ADFFormat(adf);

      // now, use the acquired format to write each line

      // get source nodes
      List<ADFNode> sourceNodes = new ArrayList<ADFNode>();
      sourceNodes.addAll(adf.lookupRootNodes());
      // do alphabetical name sorting
      Collections.sort(sourceNodes, new Comparator<ADFNode>() {
        public int compare(ADFNode node1, ADFNode node2) {
          return node1.getNodeName().compareTo(node2.getNodeName());
        }
      });

      // generate the headers
      for (String s : format.getHeaderFormat()) {
        writer.append(s).append("\t");
      }
      writer.append("\n");

      // now for each node, generate the strings for each line by walking the graph
      timer = new RenderingTimer(format.getLineCount()).start();
      if (printStdout) {
        System.out.println("Rendering...");
      }
      for (ADFNode node : sourceNodes) {
        walkGraph(writer, node, format, "");
      }
      if (printStdout) {
        System.out.println();
        System.out.println("...done!");
      }
    }
    catch (NullPointerException e) {
      e.printStackTrace();
      throw new RuntimeException(
          "Cannot generate ADF due to uninferrable graph layout");
    }
  }

  private void walkGraph(Writer adfWriter,
                         ADFNode node,
                         ADFFormat format,
                         final String currentLine) throws IOException {
    // append node to currentLine based on format
    String[] headers = node.headers();
    String[] values = node.values();

    String[] formattedHeaders = format.getColumnsForNode(node);

    String lineNow;
    // we need to rearrange values based on where the format puts them
    // for each header, only write a value if it matches the formatted header
    lineNow = currentLine;
    int writePosition = 0;
    int nodePosition = 0;

    // check we're writing on the correct layer
    int columnIndex = lineNow.split("\t", -1).length;
    while (columnIndex <= format.getNodeStartingColumn(node)) {
      // write a tab, as this probably represents a silent column or a missing attribute at the end of a column
      lineNow = lineNow.concat("\t");
      columnIndex++;
    }

    // check we haven't exhausted formatted headers yet
    while (writePosition < formattedHeaders.length) {
      if (nodePosition < headers.length) {
        if (headers[nodePosition].equals(formattedHeaders[writePosition])) {
          // match, write the values
          if (nodePosition < values.length) {
            lineNow = lineNow.concat(values[nodePosition]);
          }
          lineNow = lineNow.concat("\t");

          // increment our write position and node position
          writePosition++;
          nodePosition++;
        }
        else {
          // no match, write a blank tab
          lineNow = lineNow.concat("\t");

          // increment our write position only
          writePosition++;
        }
      }
      else {
        // there are more formatted headers than this node know about, so write extra blanks
        lineNow = lineNow.concat("\t");

        // and increment our write position
        writePosition++;
      }
    }

    // now, recurse if possible
    if (node.getChildNodes().size() > 0) {
      for (Node childNode : node.getChildNodes()) {
        if (childNode instanceof ADFNode) {
          ADFNode adfChildNode = (ADFNode) childNode;
          walkGraph(adfWriter, adfChildNode, format, lineNow);
        }
        else {
          // we've got rogue UnresolvedPlaceholderNodes, just flush the line
          adfWriter.append(lineNow).append("\n");
          timer.completed();

          if (printStdout) {
            String estimate = new DecimalFormat("#,###").format(
                ((float) timer.getCurrentEstimate()) / 1000);
            System.out.print("\rEstimated time remaining : " + estimate + "s.");
          }
        }
      }
    }
    else {
      // no child nodes present, so print this line
      adfWriter.append(lineNow).append("\n");
      timer.completed();

      if (printStdout) {
        String estimate = new DecimalFormat("#,###").format(
            ((float) timer.getCurrentEstimate()) / 1000);
        System.out.print("\rEstimated time remaining : " + estimate + "s.");
      }
    }
  }

  private static class ADFFormat extends AbstractFormat {
    private String[] headerFormat;
    private int lineCount;

    private Map<ADFNode, Integer> layerByNode;
    private Map<Integer, Integer> layerStartIndex;
    private Map<Integer, List<String>> columnsByLayer;

    public ADFFormat(ADF adf) {
      lineCount = 0;
      evaluateADFFormat(adf);
    }

    public int getLineCount() {
      return lineCount;
    }

    public String[] getHeaderFormat() {
      return headerFormat;
    }

    public int getNodeStartingColumn(ADFNode node) {
      if (layerByNode == null || columnsByLayer == null) {
        throw new RuntimeException(
            "Must evaluate ADF first before format can be determined");
      }
      else {
        int layer = layerByNode.get(node);
        return layerStartIndex.get(layer);
      }
    }

    public String[] getColumnsForNode(ADFNode node) {
      if (layerByNode == null || columnsByLayer == null) {
        throw new RuntimeException(
            "Must evaluate ADF first before format can be determined");
      }
      else {
        int layer = layerByNode.get(node);
        List<String> cols = columnsByLayer.get(layer);
        return cols.toArray(new String[cols.size()]);
      }
    }

    public void evaluateADFFormat(ADF adf) {
      // first, naively assign a layer to each node based on tree size
      layerByNode = new HashMap<ADFNode, Integer>();
      for (ADFNode node : adf.lookupRootNodes()) {
        assignLayers(layerByNode, node, 0);
      }

      // now, we need to reorganise layers so there is one type per layer
      Map<Integer, String> typeByLayer = new HashMap<Integer, String>();
      for (ADFNode node : adf.lookupRootNodes()) {
        rearrangeLayers(layerByNode, typeByLayer, node);
      }

      // now assign column order to attributes
      columnsByLayer = new HashMap<Integer, List<String>>();
      assignColumnNames(layerByNode, columnsByLayer);

      // now we should have our headers arranged nicely
      // max number of nodes
      Integer max = typeByLayer.keySet().size() == 0
          ? -1
          : Collections.max(typeByLayer.keySet());
      // list all strings that make up headers, in order
      List<String> headers = new ArrayList<String>();
      // assign each layer a starting column index
      layerStartIndex = new HashMap<Integer, Integer>();
      // iterate over nodes and attributes
      for (int i = 0; i <= max; i++) {
        // i = layer, headers.size() = the number of header columns
        // (and therefore where this layer starts)
        layerStartIndex.put(i, headers.size());
        for (String column : columnsByLayer.get(i)) {
          // now add all columns to the header
          headers.add(column);
        }
      }
      // now, drop into headerFormat string array
      headerFormat = headers.toArray(new String[headers.size()]);
    }

    private void assignLayers(Map<ADFNode, Integer> layersMap,
                              ADFNode node,
                              int currentLayer) {
      // assign the node a layer, if it doesn't already have one
      if (!layersMap.containsKey(node)) {
        layersMap.put(node, currentLayer);

        // increment the current layer
        currentLayer++;
      }
      else {
        // this node has already been assigned a layer, so recover it
        currentLayer = layersMap.get(node);
      }

      // and now assign layers to all children
      for (Node nextNode : node.getChildNodes()) {
        if (nextNode instanceof ADFNode) {
          assignLayers(layersMap, (ADFNode) nextNode, currentLayer);
        }
        else {
          lineCount++;
        }
      }

      if (node.getChildNodes().size() == 0) {
        // this is a leaf node, increment line count
        lineCount++;
      }
    }

    private void rearrangeLayers(Map<ADFNode, Integer> layerByNode,
                                 Map<Integer, String> typeByLayer,
                                 ADFNode node) {
      // indexed by the layer, list of strings are the types that are conflicting
      boolean conflicts = true;

      // assign each node type a layer, if it doesn't already have one
      while (conflicts) {
        conflicts = false;
        int nodeLayer = layerByNode.get(node);

        if (!typeByLayer.containsKey(nodeLayer)) {
          // no type yet assigned to the layer for this node, we can add it
          typeByLayer.put(nodeLayer, node.getNodeType());
        }
        else {
          // this node has been assigned a layer - are they equal?
          String typeForNodeLayer = typeByLayer.get(nodeLayer);
          if (!typeForNodeLayer.equals(node.getNodeType())) {
            // there is a conflict between the layer this node is assigned to,
            // and the type previously taken by this layer

            // can we move the node to a higher layer?
            int typeLayer = nodeLayer + 1;
            boolean movedSuccess = false;
            while (typeByLayer.containsKey(typeLayer)) {
              String typeForNextLayer = typeByLayer.get(typeLayer);
              if (typeForNextLayer == null || typeForNextLayer.equals(node.getNodeType())) {
                // we can just increment the node layer to this one
                layerByNode.put(node, typeLayer);
                movedSuccess = true;
                break;
              }
              else {
                typeLayer++;
              }
            }

            // did we successfully rearrange the layer?
            if (!movedSuccess) {
              // we can't resolve
              // read backwards from the current layer to find a matching one
              int startShiftAt = 0;
              for (int i = nodeLayer; i >= 0; i--) {
                if (typeByLayer.get(i) != null &&
                    typeByLayer.get(i).equals(node.getNodeType())) {
                  // transpose all layer
                  startShiftAt = i;
                  movedSuccess = true;
                  break;
                }
              }

              if (movedSuccess) {
                // now increment the layer of every type at a layer greater than startShiftAt
                Map<Integer, String> newTypeByLayer =
                    new HashMap<Integer, String>();
                for (int layer : typeByLayer.keySet()) {
                  if (layer < startShiftAt) {
                    newTypeByLayer.put(layer, typeByLayer.get(layer));
                  }
                  else {
                    newTypeByLayer.put(layer + 1, typeByLayer.get(layer));
                  }
                }

                // copy back
                typeByLayer.clear();
                for (int newLayer : newTypeByLayer.keySet()) {
                  typeByLayer.put(newLayer, newTypeByLayer.get(newLayer));
                }
                conflicts = true;
              }
              else {
                throw new RuntimeException("Unable to serialize ADF - " +
                    "could not find a viable column location for " +
                    node.getNodeType() + " (aiming for " + nodeLayer + ")");
              }
            }
          }
        }

        // should now have assigned a layer for this node - do children!
        for (Node nextNode : node.getChildNodes()) {
          if (nextNode instanceof ADFNode) {
            rearrangeLayers(layerByNode,
                            typeByLayer,
                            (ADFNode) nextNode);
          }
        }
      }
    }

      private void assignColumnNames(Map<ADFNode, Integer> layerByNode,
                                     Map<Integer, List<String>> columnsByLayer) {
          for (ADFNode node : layerByNode.keySet()) {
              int layer = layerByNode.get(node);

              // assign the list of columns for this node to a layer
              if (!columnsByLayer.containsKey(layer)) {
                  List<String> headers = new ArrayList<String>();
                  Collections.addAll(headers, node.headers());
                  columnsByLayer.put(layer, headers);
              } else {
                  // we've already assigned attributes, check they all match
                  List<String> assignedHeaders = columnsByLayer.get(layer);
                  String[] nodeHeaders = node.headers();

                  checkAttributes(assignedHeaders, nodeHeaders);
              }
          }
      }
  }

  private static class RenderingTimer {
    private int completedCount;
    private int totalCount;
    private long startTime;
    private long lastEstimate;

    public RenderingTimer(int numberOfLines) {
      completedCount = 0;
      totalCount = numberOfLines;
    }

    public synchronized RenderingTimer start() {
      startTime = System.currentTimeMillis();
      return this;
    }

    public synchronized RenderingTimer completed() {
      completedCount++;

      // calculate estimate of time
      long timeWorking = System.currentTimeMillis() - startTime;
      lastEstimate = (timeWorking / completedCount) *
          (totalCount - completedCount);

      return this;
    }

    public synchronized long getCurrentEstimate() {
      return lastEstimate;
    }
  }
}