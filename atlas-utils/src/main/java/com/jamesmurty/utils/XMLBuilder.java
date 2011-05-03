package com.jamesmurty.utils;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import java.io.IOException;
import java.io.Writer;

/**
 * XML Builder is a utility that creates simple XML documents using relatively
 * sparse Java code. It is intended to allow for quick and painless creation of
 * XML documents where you might otherwise be tempted to use concatenated
 * strings, rather than face the tedium and verbosity of coding with
 * JAXP (http://jaxp.dev.java.net/).
 * <p>
 * Internally, XML Builder uses JAXP to build a standard W3C
 * {@link org.w3c.dom.Document} model (DOM) that you can easily export as a
 * string, or access and manipulate further if you have special requirements.
 * </p>
 * <p>
 * The XMLBuilder class serves as a wrapper of {@link org.w3c.dom.Element} nodes,
 * and provides a number of utility methods that make it simple to
 * manipulate the underlying element and the document to which it belongs.
 * In essence, this class performs dual roles: it represents a specific XML
 * node, and also allows manipulation of the entire underlying XML document.
 * The platform's default {@link DocumentBuilderFactory} and
 * {@link DocumentBuilder} classes are used to build the document.
 * </p>
 *
 * @author James Murty
 */
public class XMLBuilder {
    /**
     * A DOM Document that stores the underlying XML document operated on by
     * XMLBuilder instances. This document object belongs to the root node
     * of a document, and is shared by this node with all other XMLBuilder
     * instances via the {@link #getDocument()} method.
     * This instance variable must only be created once, by the root node for
     * any given document.
     */
    private Document xmlDocument = null;

    /**
     * The underlying element represented by this builder node.
     */
    private Element xmlElement = null;

    /**
     * Construct a new builder object that wraps the given XML document.
     * This constructor is for internal use only.
     *
     * @param xmlDocument
     * an XML document that the builder will manage and manipulate.
     */
    protected XMLBuilder(Document xmlDocument) {
        this.xmlDocument = xmlDocument;
        this.xmlElement = xmlDocument.getDocumentElement();
    }

    /**
     * Construct a new builder object that wraps the given XML document
     * and element element.
     * This constructor is for internal use only.
     *
     * @param myElement
     * the XML element that this builder node will wrap. This element may
     * be part of the XML document, or it may be a new element that is to be
     * added to the document.
     * @param parentElement
     * If not null, the given myElement will be appended as child node of the
     * parentElement node.
     */
    protected XMLBuilder(Element myElement, Element parentElement) {
        this.xmlElement = myElement;
        this.xmlDocument = myElement.getOwnerDocument();
        if (parentElement != null) {
        	parentElement.appendChild(myElement);
        }
    }

    /**
     * Construct a builder for new XML document. The document will be created
     * with the given root element, and the builder returned by this method
     * will serve as the starting-point for any further document additions.
     *
     * @param name
     * the name of the document's root element.
     * @return
     * a builder node that can be used to add more nodes to the XML document.
     *
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     */
    public static XMLBuilder create(String name)
        throws ParserConfigurationException, FactoryConfigurationError
    {
        // Init DOM builder and Document.
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Element rootElement = document.createElement(name);
        document.appendChild(rootElement);
        return new XMLBuilder(document);
    }

    /**
     * @return
     * true if the XML Document and Element objects wrapped by this
     * builder are equal to the other's wrapped objects.
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof XMLBuilder)) {
            return false;
        }
        XMLBuilder other = (XMLBuilder) obj;
        return this.xmlDocument.equals(other.getDocument())
            && this.xmlElement.equals(other.getElement());
    }

    @Override
    public int hashCode() {
        int result = xmlDocument != null ? xmlDocument.hashCode() : 0;
        result = 31 * result + (xmlElement != null ? xmlElement.hashCode() : 0);
        return result;
    }

    /**
     * @return
     * the XML element that this builder node will manipulate.
     */
    public Element getElement() {
        return this.xmlElement;
    }

    /**
     * @return
     * the builder node representing the root element of the XML document.
     * In other words, the same builder node returned by the initial
     * {@link #create(String)}  method.
     */
    public XMLBuilder root() {
        return new XMLBuilder(getDocument());
    }

    /**
     * @return
     * the XML document constructed by all builder nodes.
     */
    public Document getDocument() {
    	return this.xmlDocument;
    }

    /**
     * Add a named XML element to the document as a child of this builder node,
     * and return the builder node representing the new child.
     *
     * @param name
     * the name of the XML element.
     *
     * @return
     * a builder node representing the new child.
     *
     * @throws IllegalStateException
     * if you attempt to add a child element to an XML node that already
     * contains a text node value.
     */
    public XMLBuilder e(String name) {
        // Ensure we don't create sub-elements in Elements that already have text node values.
        Node textNode = null;
        NodeList childNodes = xmlElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (Element.TEXT_NODE == childNodes.item(i).getNodeType()) {
                textNode = childNodes.item(i);
                break;
            }
        }
        if (textNode != null) {
            throw new IllegalStateException("Cannot add sub-element <" +
                name + "> to element <" + xmlElement.getNodeName()
                + "> that already contains the Text node: " + textNode);
        }

        return new XMLBuilder(getDocument().createElement(name), this.xmlElement);
    }

    /**
     * Add a named attribute value to the element represented by this builder
     * node, and return the node representing the element to which the
     * attribute was added (<strong>not</strong> the new attribute node).
     *
     * @param name
     * the attribute's name.
     * @param value
     * the attribute's value.
     *
     * @return
     * the builder node representing the element to which the attribute was
     * added.
     */
    public XMLBuilder a(String name, String value) {
        xmlElement.setAttribute(name, value);
        return this;
    }

    /**
     * Add a text value to the element represented by this builder node, and
     * return the node representing the element to which the text
     * was added (<strong>not</strong> the new text node).
     *
     * @param value
     * the text value to add to the element.
     *
     * @return
     * the builder node representing the element to which the text was added.
     */
    public XMLBuilder t(String value) {
        xmlElement.appendChild(getDocument().createTextNode(value));
        return this;
    }

    public XMLBuilder t(Object value) {
        return t(value.toString());
    }

    /**
     * Add a comment to the element represented by this builder node, and
     * return the node representing the element to which the comment
     * was added (<strong>not</strong> the new comment node).
     *
     * @param comment
     * the comment to add to the element.
     *
     * @return
     * the builder node representing the element to which the comment was added.
     */
    public XMLBuilder c(String comment) {
        xmlElement.appendChild(getDocument().createComment(comment));
        return this;
    }

    /**
     * Add an instruction to the element represented by this builder node, and
     * return the node representing the element to which the instruction
     * was added (<strong>not</strong> the new instruction node).
     *
     * @param target
     * the target value for the instruction.
     * @param data
     * the data value for the instruction
     *
     * @return
     * the builder node representing the element to which the instruction was
     * added.
     */
    public XMLBuilder i(String target, String data) {
        xmlElement.appendChild(getDocument().createProcessingInstruction(target, data));
        return this;
    }

    /**
     * Add a reference to the element represented by this builder node, and
     * return the node representing the element to which the reference
     * was added (<strong>not</strong> the new reference node).
     *
     * @param name
     * the name value for the reference.
     *
     * @return
     * the builder node representing the element to which the reference was
     * added.
     */
    public XMLBuilder r(String name) {
        xmlElement.appendChild(getDocument().createEntityReference(name));
        return this;
    }

    /**
     * Return the builder node representing the n<em>th</em> ancestor element
     * of this node, or the root node if n exceeds the document's depth.
     *
     * @param steps
     * the number of parent elements to step over while navigating up the chain
     * of node ancestors. A steps value of 1 will find a node's parent, 2 will
     * find its grandparent etc.
     *
     * @return
     * the n<em>th</em> ancestor of this node, or the root node if this is
     * reached before the n<em>th</em> parent is found.
     */
    public XMLBuilder up(int steps) {
    	Node currNode = this.xmlElement;
        int stepCount = 0;
        while (currNode.getParentNode() != null && stepCount < steps) {
        	currNode = currNode.getParentNode();
            stepCount++;
        }
        return new XMLBuilder((Element) currNode, null);
    }

    /**
     * Return the builder node representing the parent of the current node.
     *
     * @return
     * the parent of this node, or the root node if this method is called on the
     * root node.
     */
    public XMLBuilder up() {
        return up(1);
    }

    /**
     * Serialize the XML document to a string. If output options are
     * provided, these options are provided to the {@link Transformer}
     * serializer.
     *
     * @param indent indent or not
     * @param indentAmount how much
     * @param where appendable to write to 
     * @throws IOException if can't write
     */
    public void write(final Appendable where, boolean indent, int indentAmount)
        throws IOException
    {
        OutputFormat of = new OutputFormat("XML","utf-8",true);
        of.setIndent(indentAmount);
        of.setIndenting(indent);
        XMLSerializer serializer = new XMLSerializer(new Writer() {
            public void write(char[] cbuf, int off, int len) throws IOException {
                where.append(java.nio.CharBuffer.wrap(cbuf, off, len));
            }
            public void flush() throws IOException { }
            public void close() throws IOException { }
        }, of);
        serializer.asDOMSerializer();
        serializer.serialize(getDocument());
    }

}
