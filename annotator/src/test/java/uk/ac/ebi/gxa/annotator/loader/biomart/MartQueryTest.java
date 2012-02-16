/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Olga Melnichuk
 */
public class MartQueryTest {

    @Test
    public void testQuerySyntax() throws ParserConfigurationException, IOException, SAXException {
        final String virtualSchemaName = "_virtualSchemaName_";
        final String dataSetName = "_dataSetName_";
        final List<String> attrs = asList("attr1", "attr2");
        final boolean count = true;

        MartQuery query = new MartQuery(virtualSchemaName, dataSetName)
                .addAttributes(attrs)
                .setCount(count);

        validateXml(query.toString(),
                virtualSchemaName,
                dataSetName,
                attrs,
                count ? "1" : "0");
    }

    private void validateXml(String xmlString,
                             String virtualSchemaName,
                             String dataSetName,
                             List<String> attrs,
                             String count) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(new ByteArrayInputStream(xmlString.getBytes()));
        
        assertEquals("1.0", doc.getXmlVersion());
        assertEquals("UTF-8", doc.getXmlEncoding());
        
        DocumentType docType = doc.getDoctype();
        assertEquals("Query", docType.getName());
        
        Element queryElm = doc.getDocumentElement();
        assertEquals("Query", queryElm.getTagName());

        assertEquals(virtualSchemaName, queryElm.getAttribute("virtualSchemaName"));
        assertEquals("TSV", queryElm.getAttribute("formatter"));
        assertEquals("1", queryElm.getAttribute("header"));
        assertEquals("1", queryElm.getAttribute("uniqueRows"));
        assertEquals(count, queryElm.getAttribute("count"));

        NodeList nlist = queryElm.getElementsByTagName("Dataset");
        assertEquals(1, nlist.getLength());
        
        Element datasetElm = (Element) nlist.item(0);
        assertEquals(dataSetName, datasetElm.getAttribute("name"));
        assertEquals("default", datasetElm.getAttribute("interface"));
        
        nlist = queryElm.getElementsByTagName("Attribute");
        assertEquals(attrs.size(), nlist.getLength());

        List<String> attrNames = new ArrayList<String>();
        for(int i =0; i<nlist.getLength(); i++) {
                Element attrElm = (Element) nlist.item(i);
            attrNames.add(attrElm.getAttribute("name"));
        }
        Collections.sort(attrNames);
        
        List<String> copy = new ArrayList<String>(attrs);
        Collections.sort(copy);
        
        assertArrayEquals(copy.toArray(new String[copy.size()]),
                attrNames.toArray(new String[attrNames.size()]));
    }
}
