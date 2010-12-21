package uk.ac.ebi.arrayexpress2.magetab.utils;

import java.util.List;

public class AbstractFormat {
    protected static void checkAttributes(List<String> assignedHeaders, String[] nodeHeaders) {
        int matchPosition = 0;
        int nodePosition = 0;
        // check we haven't exhausted formatted headers yet
        while (matchPosition < assignedHeaders.size()) {
          String assignedHeader = assignedHeaders.get(matchPosition);

          if (nodePosition < nodeHeaders.length) {
            String candidateHeader = nodeHeaders[nodePosition];
            if (candidateHeader.equals(assignedHeader)) {
              // increment our write position and node position
              matchPosition++;
              nodePosition++;
            }
            else {
              // the current column doesn't match
              // is this an "anchored" attribute - i.e. Term Source REF, Unit, or Comment?
              if (candidateHeader.equals("Term Source REF") ||
                  candidateHeader.startsWith("Unit") ||
                  candidateHeader.startsWith("Comment")) {
                // if so, we can't move this header away from it's prior header
                // so insert into the next assign header slot
                matchPosition++;
                assignedHeaders.add(matchPosition, candidateHeader);

                // increment
                matchPosition++;
                nodePosition++;
              }
              else {
                // not an anchored type, so could be...
                //   a gap (i.e. this node has missing attributes) or
                //   a new attribute that hasn't been assigned yet

                // has it already been assigned further along?
                boolean leaveGap = false;
                int gapEndPosition = matchPosition + 1;
                while (gapEndPosition < assignedHeaders.size()) {
                  assignedHeader = assignedHeaders.get(gapEndPosition);

                  if (candidateHeader.equals(assignedHeader)) {
                    // jump the write position forward to this mark
                    leaveGap = true;
                    matchPosition = gapEndPosition;
                    break;
                  }
                  else {
                    gapEndPosition++;
                  }
                }

                if (!leaveGap) {
                  // this is a new attribute, rather than simply a gap

                  // insert at the current match position
                  assignedHeaders.add(matchPosition, candidateHeader);

                  // increment
                  matchPosition++;
                  nodePosition++;

                  // and take any anchored attributes with this extra attribute
                  while (nodePosition < nodeHeaders.length && (
                      nodeHeaders[nodePosition].equals("Term Source REF") ||
                          nodeHeaders[nodePosition].startsWith("Unit") ||
                          nodeHeaders[nodePosition].startsWith("Comment"))) {
                    assignedHeaders.add(matchPosition,
                                        nodeHeaders[nodePosition]);
                    matchPosition++;
                    nodePosition++;
                  }
                }
                else {
                  // we just need to leave a gap, as we can match later on
                }
              }
            }
          }
          else {
            // we've run out of nodes, so just exit
            break;
          }
        }

        // we've iterated over all our assigned headers - have we done all node headers?
        while (nodePosition < nodeHeaders.length) {
          // here, add any extra headers from this node
          String candidateHeader = nodeHeaders[nodePosition];

          // insert at the very end
          assignedHeaders.add(candidateHeader);

          // increment node position, but not match position cos we've run out of assigned headers
          nodePosition++;
        }
    }
}
