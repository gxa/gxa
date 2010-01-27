package uk.ac.ebi.gxa.loader.utils;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A class filled with handy convenience methods for performing writing tasks common to lots of SDRF graph nodes.  This
 * class contains methods that hepl with writing {@link Property} objects out given some nodes in the SDRF graph.
 *
 * @author Tony Burdett
 * @date 28-Aug-2009
 */
public class SDRFWritingUtils {
    private static Logger log = LoggerFactory.getLogger(SDRFWritingUtils.class);

    /**
     * Write out the properties associated with a {@link Sample} in the SDRF graph.  These properties are obtained by
     * looking at the "characteristic" column in the SDRF graph, extracting the type and linking this type (the
     * property) to the name of the {@link SourceNode} provided (the property value).
     *
     * @param investigation the investigation being loaded
     * @param sample        the sample you want to attach properties to
     * @param sourceNode    the sourceNode being read
     * @throws ObjectConversionException if there is a problem creating the property object
     */
    public static void writeSampleProperties(
            MAGETABInvestigation investigation,
            Sample sample,
            SourceNode sourceNode)
            throws ObjectConversionException {
        // fetch characteristics of this sourceNode
        for (CharacteristicsAttribute characteristicsAttribute : sourceNode.characteristics) {
            // create Property for this attribute
            if (characteristicsAttribute.type.contains("||") || characteristicsAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                String message = "Characteristics and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database";

                ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                        .generateErrorItem(message, 999, SDRFWritingUtils.class);

                throw new ObjectConversionException(error, true);
            }

            // does this sample already contain this property/property value pair?
            boolean existing = false;
            if (sample.getProperties() != null) {
                for (Property sp : sample.getProperties()) {
                    if (sp.getName().equals(characteristicsAttribute.type)) {
                        if (sp.getValue().equals(characteristicsAttribute.getNodeName())) {
                            existing = true;
                            break;
                        }
                        else {
                            // generate error item and throw exception
                            String message = "Inconsistent characteristic values for sample " + sample.getAccession() +
                                    ": property " + sp.getName() + " has values " + sp.getValue() + " and " +
                                    characteristicsAttribute.getNodeName() + " in different rows. Second value (" +
                                    characteristicsAttribute + ") will be ignored";

                            ErrorItem error =
                                    ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                            .generateErrorItem(message, 40, SDRFWritingUtils.class);

                            throw new ObjectConversionException(error, false);

                        }
                    }
                }
            }

            if (!existing) {
                Property p = new Property();
                p.setAccession(AtlasLoaderUtils.getNodeAccession(
                        investigation, characteristicsAttribute));
                p.setName(characteristicsAttribute.type);
                p.setValue(characteristicsAttribute.getNodeName());
                p.setFactorValue(false);

                sample.addProperty(p);

                // todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
            }
        }
    }

    /**
     * Write out the properties associated with an {@link Assay} in the SDRF graph.  These properties are obtained by
     * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
     * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode} provided (the property
     * value).
     *
     * @param investigation the investigation being loaded
     * @param assay         the assay you want to attach properties to
     * @param assayNode     the assayNode being read
     * @throws ObjectConversionException if there is a problem creating the property object
     */
    public static void writeAssayProperties(
            MAGETABInvestigation investigation,
            Assay assay,
            AssayNode assayNode)
            throws ObjectConversionException {
        // fetch factor values of this assayNode
        for (FactorValueAttribute factorValueAttribute : assayNode.factorValues) {
            // create Property for this attribute
            if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                String message = "Factors and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database";

                ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                        .generateErrorItem(message, 999, SDRFWritingUtils.class);

                throw new ObjectConversionException(error, true);
            }

            // does this assay already contain this property/property value pair?
            boolean existing = false;
            if (assay.getProperties() != null) {
                for (Property ap : assay.getProperties()) {
                    if (ap.getName().equals(factorValueAttribute.type)) {
                        if (ap.getValue().equals(factorValueAttribute.getNodeName())) {
                            existing = true;
                            break;
                        }
                        else {
                            // generate error item and throw exception
                            String message = "Inconsistent characteristic values for assay " + assay.getAccession() +
                                    ": property " + ap.getName() + " has values " + ap.getValue() + " and " +
                                    factorValueAttribute.getNodeName() + " in different rows. Second value (" +
                                    factorValueAttribute + ") will be ignored";

                            ErrorItem error =
                                    ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                            .generateErrorItem(message, 40, SDRFWritingUtils.class);

                            throw new ObjectConversionException(error, false);

                        }
                    }
                }
            }

            if (!existing) {
                Property p = new Property();
                p.setAccession(AtlasLoaderUtils.getNodeAccession(
                        investigation, factorValueAttribute));
                p.setName(factorValueAttribute.type);
                p.setValue(factorValueAttribute.getNodeName());
                p.setFactorValue(false);

                assay.addProperty(p);

                // todo - factor values can have ontology entries, set these values
            }
        }
    }

    /**
     * Write out the properties associated with an {@link Assay} in the SDRF graph.  These properties are obtained by
     * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
     * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode} provided (the
     * property value).
     *
     * @param investigation     the investigation being loaded
     * @param assay             the assay you want to attach properties to
     * @param hybridizationNode the hybridizationNode being read
     * @throws ObjectConversionException if there is a problem creating the property object
     */
    public static void writeHybridizationProperties(MAGETABInvestigation investigation,
                                                    Assay assay,
                                                    HybridizationNode hybridizationNode)
            throws ObjectConversionException {
        // fetch factor values of this assayNode
        for (FactorValueAttribute factorValueAttribute : hybridizationNode.factorValues) {
            // create Property for this attribute
            if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                String message = "Factors and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database";

                ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                        .generateErrorItem(message, 999, SDRFWritingUtils.class);

                throw new ObjectConversionException(error, true);
            }

            // does this assay already contain this property/property value pair?
            boolean existing = false;
            if (assay.getProperties() != null) {
                for (Property ap : assay.getProperties()) {
                    if (ap.getName().equals(factorValueAttribute.type)) {
                        if (ap.getValue().equals(factorValueAttribute.getNodeName())) {
                            existing = true;
                            break;
                        }
                        else {
                            // generate error item and throw exception
                            String message = "Inconsistent characteristic values for assay " + assay.getAccession() +
                                    ": property " + ap.getName() + " has values " + ap.getValue() + " and " +
                                    factorValueAttribute.getNodeName() + " in different rows. Second value (" +
                                    factorValueAttribute + ") will be ignored";

                            ErrorItem error =
                                    ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                            .generateErrorItem(message, 40, SDRFWritingUtils.class);

                            throw new ObjectConversionException(error, false);

                        }
                    }
                }
            }

            if (!existing) {
                Property p = new Property();
                p.setAccession(AtlasLoaderUtils.getNodeAccession(
                        investigation, factorValueAttribute));
                p.setName(factorValueAttribute.type);
                p.setValue(factorValueAttribute.getNodeName());
                p.setFactorValue(false);

                assay.addProperty(p);

                // todo - factor values can have ontology entries, set these values
            }
        }
    }

    /**
     * Finds nodes upstream of the given node in the SDRF graph, searching for nodes of a particular type.  This returns
     * all nodes upstream of the given node, of the supplied type - this includes both those directly upstream or those
     * further hops away.  So, for example, if you have nodes a (type A), b1 (type B), b2 (type B) and c (type C) and
     * your graph looks like:
     * <pre>
     *     a -> b1 -> b2 -> c
     * </pre>
     * If you call <code>findUpstreamNodes(sdrf, c, B.class);</code> your answer will contain b1 and b2.
     *
     * @param sdrf             the SDRF graph to look in
     * @param currentNode      the current node - this method will locate all nodes upstream of this node
     * @param upstreamNodeType the type of nodes you are interested in locating
     * @return the collection of SDRF nodes, of the "upstreamNodeType" type, upstream of "currentNode"
     */
    public static <T extends SDRFNode> Collection<T> findUpstreamNodes(SDRF sdrf,
                                                                       SDRFNode currentNode,
                                                                       Class<T> upstreamNodeType) {
        Set<T> foundNodes = new HashSet<T>();

        for (T candidateNode : sdrf.lookupNodes(upstreamNodeType)) {
            // walk downstream, if there is a child matching this node then we want this
            if (isDirectlyDownstream(sdrf, candidateNode, currentNode)) {
                foundNodes.add(candidateNode);
            }
        }

        // return all the nodes we found
        return foundNodes;
    }

    /**
     * Finds nodes downstream of the given node in the SDRF graph, searching for nodes of a particular type.
     *
     * @param sdrf               the SDRF graph to look in
     * @param currentNode        the current node - this method will locate all nodes downstream of this node
     * @param downstreamNodeType the type of nodes you are interested in locating
     * @return the collection of SDRF nodes, of the "downstreamNodeType" type, downstream of "currentNode"
     */
    public static <T extends SDRFNode> Collection<T> findDownstreamNodes(SDRF sdrf,
                                                                         SDRFNode currentNode,
                                                                         Class<T> downstreamNodeType) {
        Set<T> foundNodes = new HashSet<T>();

        for (T candidateNode : sdrf.lookupNodes(downstreamNodeType)) {
            // walk downstream, if there is a child matching this node then we want this
            if (isDirectlyUpstream(sdrf, candidateNode, currentNode)) {
                foundNodes.add(candidateNode);
            }
        }

        // return all the nodes we found
        return foundNodes;
    }

    /**
     * Determine whether the query node, "maybeUpstreamNode", is upstream of the current node in the given SDRF graph.
     * This only takes into account nodes that are DIRECTLY upstream - if there is a node of the same type as the
     * maybeUpstreamNode between it and the currentNode, this method returns false.
     *
     * @param sdrf              the SDRF graph to inspect
     * @param currentNode       the target node: determine whether the query node is upstream of this
     * @param maybeUpstreamNode the node to assess
     * @return true if "maybeUpstreamNode" is upstream of currentNode, false otherwise
     */
    public static boolean isDirectlyUpstream(SDRF sdrf, SDRFNode currentNode, SDRFNode maybeUpstreamNode) {
        // check children of maybeUpstreamNode
        if (maybeUpstreamNode.getChildNodeValues().contains(currentNode.getNodeName())) {
            // does have the target as a child
            return true;
        }
        else {
            // no immediate child, but recurse as long as child node types aren't the same as currentNode type
            if (maybeUpstreamNode.getChildNodeType().equals(currentNode.getNodeType())) {
                return false;
            }
            else {
                // child node types are different, so recurse
                for (String nodeName : maybeUpstreamNode.getChildNodeValues()) {
                    SDRFNode nextNode = sdrf.lookupNode(nodeName, maybeUpstreamNode.getChildNodeType());
                    // if we found the child here, return true
                    if (nextNode != null && isDirectlyUpstream(sdrf, nextNode, currentNode)) {
                        return true;
                    }
                }
            }
        }

        // if we got to here, no children of node match, so return false
        return false;
    }

    /**
     * Determine whether the query node, "maybeDownstreamNode", is downstream of the current node in the given SDRF
     * graph. This only takes into account nodes that are DIRECTLY downstream - if there is a node of the same type as
     * the maybeDownstreamNode between it and the currentNode, this method returns false.
     *
     * @param sdrf                the SDRF graph to inspect
     * @param currentNode         the target node: determine whether the query node is downstream of this
     * @param maybeDownstreamNode the node to assess
     * @return true if "maybeDownstreamNode" is downstream of currentNode, false otherwise
     */
    public static boolean isDirectlyDownstream(SDRF sdrf, SDRFNode currentNode, SDRFNode maybeDownstreamNode) {
        // check for nulls - remember the graph might just not yet be complete,
        // but nodes upstream of the one we want will DEFINATELY be present
        if (currentNode.getChildNodeType() == null || currentNode.getChildNodeValues() == null) {
            return false;
        }

        // check children of currentNode
        if (currentNode.getChildNodeValues().contains(maybeDownstreamNode.getNodeName())) {
            // does have the maybeDownstreamNode as a child
            return true;
        }
        else {
            // no immediate child, but recurse as long as child node types aren't the same as maybeDownstreamNode type
            if (currentNode.getChildNodeType().equals(maybeDownstreamNode.getNodeType())) {
                return false;
            }
            else {
                // child node types are different, so recurse
                for (String nodeName : currentNode.getChildNodeValues()) {
                    SDRFNode nextNode = sdrf.lookupNode(nodeName, currentNode.getChildNodeType());
                    // if we found the child here, return true
                    if (nextNode != null && isDirectlyDownstream(sdrf, nextNode, maybeDownstreamNode)) {
                        return true;
                    }
                }
            }
        }

        // if we got to here, no children of node match, so return false
        return false;
    }
}
