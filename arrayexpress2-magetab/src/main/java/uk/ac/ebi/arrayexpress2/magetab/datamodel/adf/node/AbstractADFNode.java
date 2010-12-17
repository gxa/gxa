package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.AbstractNode;

/**
 * An abstract top-level implementation of an ADFNode.  This provides no
 * additional functionality and exists as a marker abstract class, designating
 * nodes that extend it as belonging to the ADF graph.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public abstract class AbstractADFNode
    extends AbstractNode
    implements ADFNode {
}
