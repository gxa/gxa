/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.index;

import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;

/**
 * Spring-friendly factory class, allowing to create CoreContainer's configured by index path represented as java.io.File
 * @author pashky
 */
public class SolrContainerFactory {

    final private Logger log = LoggerFactory.getLogger(getClass());

    private final static String[] CONF_FILES = new String[]{
            "solrconfig.xml",
            "stopwords.txt",
            "protwords.txt",
            "synonyms.txt",
            "schema.xml"
    };

    private File atlasIndex;
    private String templatePath;
    
    public SolrContainerFactory() {

    }

    public File getAtlasIndex() {
        return atlasIndex;
    }

    public void setAtlasIndex(File indexLocation) {
        this.atlasIndex = indexLocation;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public CoreContainer createContainer() throws IOException, ParserConfigurationException, SAXException {
        File solr = new File(atlasIndex, "solr.xml");
        if (!solr.exists())
        {
            if(getTemplatePath() != null)
                deployIndex();
            else
                throw new IOException("SOLR index not found and don't have template to create a new one");
        }
        return new CoreContainer(atlasIndex.getAbsolutePath(), new File(atlasIndex, "solr.xml"));
    }

    /**
     * This method bootstraps an empty atlas index when starting an indexbuilder from scratch.  Use this is the index
     * could not be found, and you should get a ready-to-build index with all required config files
     *
     * @throws IOException if the resources could not be written
     */
    private void deployIndex() throws IOException {
        // check for the presence of the index
        log.debug("No existing index - unpacking config files to " +
                atlasIndex.getAbsolutePath());
        // no prior index, check the directory is empty?
        if (atlasIndex.exists() && atlasIndex.listFiles().length > 0) {
            String message = "Unable to unpack solr configuration files - " +
                    atlasIndex.getAbsolutePath() + " is not empty. " +
                    "Please choose an empty directory to create the index";
            log.error(message);
            throw new RuntimeException(message);
        }
        else {
            // unpack configuration files
            writeResourceToFile(getTemplatePath() + "/solr.xml", new File(atlasIndex, "solr.xml"));

            try {
                javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        getTemplatePath() + "/solr.xml"));
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                NodeList cores = (NodeList)xpath.evaluate("//cores/core", doc, XPathConstants.NODESET);
                for(int i = 0; i < cores.getLength(); ++i) {
                    Node node = cores.item(i);
                    String path = node.getAttributes().getNamedItem("instanceDir").getTextContent();
                    for(String file : CONF_FILES) {
                        String filePath = path + "/conf/" + file;
                        writeResourceToFile(getTemplatePath() + "/" + filePath,
                                new File(atlasIndex, filePath.replaceAll("/", File.separator)));
                    }

                }

            } catch (ParserConfigurationException e) {

            } catch (SAXException e) {

            } catch (XPathExpressionException e) {

            }
        }
    }


    /**
     * Writes a classpath resource to a file in the specified location.  You should not use this to overwrite files - if
     * you attempt this, an IOException will be thrown.  Also note that an IOException is thrown if the file you specify
     * is in a new directory and the parent directories required could not be created.
     *
     * @param resourceName the name of the classpath resource to copy
     * @param file         the file to write the classpath resource to
     * @throws IOException if the resource could not properly be written out, or if the file already exists
     */
    private void writeResourceToFile(String resourceName, File file)
            throws IOException {
        // make all parent dirs necessary if they don't exist
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Unable to make index directory " +
                        file.getParentFile() + ", do you have permission to write here?");
            }
        }

        // check the resource we're attempting to write doesn't exist
        if (file.exists()) {
            throw new IOException("The file " + file + " already exists - you " +
                    "should not attempt to overwrite an existing index.  If you wish " +
                    "to replace this index, please backup or delete the old one first");
        }

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(
                                resourceName)));
        BufferedWriter writer =
                new BufferedWriter(new FileWriter(file));
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line + "\n");
        }
        reader.close();
        writer.close();
    }

}
