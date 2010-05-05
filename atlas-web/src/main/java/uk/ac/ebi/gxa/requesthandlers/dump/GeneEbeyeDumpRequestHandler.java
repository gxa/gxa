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

package uk.ac.ebi.gxa.requesthandlers.dump;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.model.AtlasGeneDescription;
import ae3.util.FileDownloadServer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, InitializingBean, DisposableBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private File ebeyeDumpFile;
    private AtlasSolrDAO atlasSolrDAO;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    public AtlasSolrDAO getDao() {
        return atlasSolrDAO;
    }

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void afterPropertiesSet() throws Exception {
        if(ebeyeDumpFile == null)
            ebeyeDumpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + atlasProperties.getDumpEbeyeFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene ebeye dump download request");
        if(!ebeyeDumpFile.exists())
            dumpEbeyeData();
        FileDownloadServer.processRequest(ebeyeDumpFile, "application/x-gzip", httpServletRequest, httpServletResponse);
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        ebeyeDumpFile.delete();
        if(atlasProperties.isGeneListAfterIndexAutogenerate())
            dumpEbeyeData();
    }

    public void onIndexBuildStart(IndexBuilder builder) {

    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     */
    public void dumpEbeyeData() {
        XMLOutputFactory output = null;
        XMLStreamWriter writer = null;

        try {
            log.info("Writing ebeye file from index to " + ebeyeDumpFile);

            output = XMLOutputFactory.newInstance();
            writer = output.createXMLStreamWriter(
                    new GZIPOutputStream(
                      new BufferedOutputStream(
                        new FileOutputStream(ebeyeDumpFile), 2048)));

            writer.writeStartDocument();
            writer.writeStartElement("database");

            writer.writeStartElement("name");
            writer.writeCharacters("Gene Expression Atlas");
            writer.writeEndElement();

            writer.writeStartElement("description");
            writer.writeCharacters("Large scale meta-analysis of public transcriptomics data");
            writer.writeEndElement();

            writer.writeStartElement("release");
            writer.writeCharacters(atlasProperties.getDataRelease());
            writer.writeEndElement();

            writer.writeStartElement("release_date");
            writer.writeCharacters(new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
            writer.writeEndElement();

            writer.writeStartElement("entry_count");
            writer.writeCharacters(String.valueOf(atlasSolrDAO.getGeneCount()));
            writer.writeEndElement();

            writer.writeStartElement("entries");

            int i = 0;
            for (AtlasGene gene : atlasSolrDAO.getAllGenes()) {
                Map<String, Collection<String>> geneprops = gene.getGeneProperties();

                writer.writeStartElement("entry");
                writer.writeAttribute("id", gene.getGeneIdentifier());
                writer.writeAttribute("acc", gene.getGeneIdentifier());

                writer.writeStartElement("name");
                writer.writeCharacters(gene.getGeneName());
                writer.writeEndElement();

                writer.writeStartElement("description");
                writer.writeCharacters(new AtlasGeneDescription(atlasProperties, gene).toString());
                writer.writeEndElement();

                writer.writeStartElement("keywords");
                writer.writeCharacters(gene.getPropertyValue("keyword"));
                writer.writeEndElement();

                writer.writeStartElement("dates");

                writer.writeStartElement("date");
                writer.writeAttribute("type","creation");
                writer.writeAttribute("value",new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writer.writeEndElement();

                writer.writeStartElement("date");
                writer.writeAttribute("type","last_modification");
                writer.writeAttribute("value",new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writer.writeEndElement();

                writer.writeEndElement(); // dates

                writer.writeStartElement("cross_references");
                for (String geneIdField : atlasProperties.getDumpGeneIdFields()) {
                    Collection<String> genepropvals = geneprops.get(geneIdField);
                    if(null != genepropvals && genepropvals.size() > 0) {
                        for (String propval : genepropvals) {
                            writer.writeStartElement("ref");
                            writer.writeAttribute("dbname",geneIdField);
                            writer.writeAttribute("dbkey",propval);
                            writer.writeEndElement();
                        }
                    }
                }
                writer.writeEndElement(); // xrefs

                writer.writeStartElement("additional_fields");
                for (Map.Entry<String, Collection<String>> geneprop : geneprops.entrySet()) {
                    if(!atlasProperties.getDumpGeneIdFields().contains(geneprop.getKey())) {
                        writer.writeStartElement("field");
                        writer.writeAttribute("name",geneprop.getKey());
                        writer.writeCharacters(StringUtils.join(geneprop.getValue(), ", "));
                        writer.writeEndElement();
                    }

                    /* TODO: this blows up the dump to gigabytes in size, need to rethink/redo
                    List<ListResultRow> data = gene.getHeatMapRows(atlasProperties.getOptionsIgnoredEfs());
                    for (ListResultRow row : data) {
                        writer.writeStartElement("field");
                        writer.writeAttribute("name",row.getEf());
                        writer.writeCharacters(row.getFv());
                        writer.writeEndElement();
                    }
                    */
                }
                writer.writeEndElement(); // add'l fields

                writer.writeEndElement(); // entry

                if (0 == (++i % 100)) {
                    writer.flush();
                    log.debug("Wrote " + i + " genes");
                }
            }

            writer.writeEndElement(); // entries
            writer.writeEndDocument();
            writer.flush();

        } catch (XMLStreamException e) {
            log.error("Failed to dump gene identifiers from index", e);
        } catch (IOException e) {
            log.error("Couldn't write to " + ebeyeDumpFile.getAbsolutePath(), e);
        } finally {
            try {
                if(null != writer) writer.close();
            } catch (XMLStreamException x) {
                log.error("Failed to close XMLStreamWriter", x);
            }

            log.info("Writing ebeye file from index to " + ebeyeDumpFile + " - done");
        }
    }

    public void destroy() throws Exception {
        if(indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
