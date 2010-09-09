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
import ae3.model.AtlasExperiment;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, InitializingBean, DisposableBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String BR = "\n";
    private static final String UNDERSCORE = "_";
    // No alphanumeric characters may break Lucene indexing - the literal below will be used to replace
    // them with UNDERSCORE
    private static final String NON_ALPHANUMERIC_PATTERN ="[^a-zA-Z0-9]+";

    private File ebeyeDumpFile;
    private AtlasSolrDAO atlasSolrDAO;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    // Constant used for testing if a <gene name> == GENE_PREAMBLE + <gene id>; This seems to be a case
    // of a gene that is loaded from an experiment but cannot be mapped to any gene currently in A2_gene table.
    // In such cases we don't output gene name into the EB-eye gene dump - in an effort to avoid redundancy.
    private static final String GENE_PREAMBLE = "GENE:";

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
        if (ebeyeDumpFile == null)
            ebeyeDumpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + atlasProperties.getDumpEbeyeFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene ebeye dump download request");
        synchronized(this)
        {
            if (!ebeyeDumpFile.exists())
                dumpEbeyeData();
        }
        FileDownloadServer.processRequest(ebeyeDumpFile, "application/x-zip", httpServletRequest, httpServletResponse);
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        ebeyeDumpFile.delete();
        if (atlasProperties.isGeneListAfterIndexAutogenerate())
            dumpEbeyeData();
    }

    public void onIndexBuildStart(IndexBuilder builder) {

    }

    /**
     * Generates a zip file containing to be used in EB-eye. The zip file contains two xml files,
     * one containing genes, and one containing experiments data.
     */
    public void dumpEbeyeData() {
        ZipOutputStream outputStream = null;
        try {
            outputStream = new ZipOutputStream(new FileOutputStream(ebeyeDumpFile));
            dumpGenesForEbeye(outputStream);
            dumpExperimentsForEbeye(outputStream);
        } catch (IOException e) {
            log.error("Couldn't write to " + ebeyeDumpFile.getAbsolutePath(), e);
        } finally {
            try {
                if (null != outputStream)
                    outputStream.close();
            } catch (Exception e) {
                log.error("Failed to close outputStream", e);
            }
        }                       
    }

    private Map<Long, AtlasExperiment> getidToExperimentMapping() {
        // Used LinkedHashMap to preserve order of insertion
        Map<Long, AtlasExperiment> idToExperiment = new LinkedHashMap<Long, AtlasExperiment>();
        List<AtlasExperiment> experiments = atlasSolrDAO.getExperiments();
        for (AtlasExperiment exp : experiments) {
            idToExperiment.put(new Long(exp.getId()), exp);
        }
        return idToExperiment;
    }

    /**
     * Calls writeEndElement() writer, then writes a new line
     *
     * @param writer
     * @throws XMLStreamException
     */
    private void writeEndElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    /**
     * Write ebeye dump header information to writer
     * @param writer
     * @throws XMLStreamException
     */
    private void writeHeader(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument();
        writer.writeStartElement("database");

        writer.writeStartElement("name");
        writer.writeCharacters("Gene Expression Atlas");
        writeEndElement(writer);

        writer.writeStartElement("description");
        writer.writeCharacters("Large scale meta-analysis of public transcriptomics data");
        writeEndElement(writer);

        writer.writeStartElement("release");
        writer.writeCharacters(atlasProperties.getDataRelease());
        writeEndElement(writer);

        writer.writeStartElement("release_date");
        writer.writeCharacters(new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
        writeEndElement(writer);
    }

    /**
     * Add genes xml file to zip file:  outputStream
     * @param outputStream ZipOutputStream
     */
    private void dumpGenesForEbeye(ZipOutputStream outputStream) {
        XMLOutputFactory output = null;
        XMLStreamWriter writer = null;

        try {
            String genesDumpFileName = atlasProperties.getGenesDumpEbeyeFilename();
            log.info("Writing " + genesDumpFileName + " to " + ebeyeDumpFile);
            outputStream.putNextEntry(new ZipEntry(genesDumpFileName));

            output = XMLOutputFactory.newInstance();
            writer = output.createXMLStreamWriter(outputStream);
            writeHeader(writer);

            writer.writeStartElement("entry_count");
            writer.writeCharacters(String.valueOf(atlasSolrDAO.getGeneCount()));
            writeEndElement(writer);


            Map<Long, AtlasExperiment> idToExperiment = getidToExperimentMapping();

            writer.writeStartElement("entries");

            int i = 0;
            for (AtlasGene gene : atlasSolrDAO.getAllGenes()) {
                Map<String, Collection<String>> geneprops = gene.getGeneProperties();

                writer.writeStartElement("entry");
                String geneName =  gene.getGeneName();
                String geneId = gene.getGeneIdentifier();

                writer.writeAttribute("id", geneId );

                if (!geneName.equals(GENE_PREAMBLE + geneId)) {
                    // Don't output the gene name, as it is almost identical to its id
                    writer.writeStartElement("name");
                    writer.writeCharacters(geneName);
                    writeEndElement(writer);
                }

                AtlasGeneDescription geneDescription = new AtlasGeneDescription(atlasProperties, gene);

                writer.writeStartElement("description");
                writer.writeCharacters(geneDescription.toStringExperimentCount());
                writeEndElement(writer);

                writer.writeStartElement("keywords");
                writer.writeCharacters(gene.getPropertyValue("keyword"));
                writeEndElement(writer);

                writer.writeStartElement("dates");

                writer.writeStartElement("date");
                writer.writeAttribute("type", "creation");
                writer.writeAttribute("value", new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writeEndElement(writer);

                writer.writeStartElement("date");
                writer.writeAttribute("type", "last_modification");
                writer.writeAttribute("value", new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writeEndElement(writer);

                writeEndElement(writer); // dates

                writer.writeStartElement("cross_references");
                // Cross-reference gene to properties
                for (String geneIdField : atlasProperties.getDumpGeneIdFields()) {
                    Collection<String> genepropvals = geneprops.get(geneIdField);
                    if (null != genepropvals && genepropvals.size() > 0) {
                        for (String propval : genepropvals) {
                            writer.writeStartElement("ref");
                            writer.writeAttribute("dbname", geneIdField);
                            writer.writeAttribute("dbkey", propval);
                            writeEndElement(writer);
                        }
                    }
                }
                // Cross-reference gene to experiments
                Set<Long> experimentIds = gene.getExperimentIds();
                for (Long experimentId : experimentIds) {
                    writer.writeStartElement("ref");
                    writer.writeAttribute("dbname", "atlas");
                    writer.writeAttribute("dbkey", idToExperiment.get(experimentId).getAccession());
                    writeEndElement(writer);
                }

                writeEndElement(writer); // xrefs

                writer.writeStartElement("additional_fields");
                for (Map.Entry<String, Collection<String>> geneprop : geneprops.entrySet()) {
                    if (!atlasProperties.getDumpGeneIdFields().contains(geneprop.getKey()) &&
                            !"keyword".equals(geneprop.getKey())) {  // The keyword property value(s) have already been output before additional_fields
                        writer.writeStartElement("field");
                        writer.writeAttribute("name", geneprop.getKey().replaceAll(NON_ALPHANUMERIC_PATTERN, UNDERSCORE));
                        writer.writeCharacters(StringUtils.join(geneprop.getValue(), ", "));
                        writeEndElement(writer);
                    }

                    /* TODO: this blows up the dump to gigabytes in size, need to rethink/redo
                    List<ListResultRow> data = gene.getHeatMapRows(atlasProperties.getOptionsIgnoredEfs());
                    for (ListResultRow row : data) {
                        writer.writeStartElement("field");
                        writer.writeAttribute("name",row.getEf().replaceAll(NON_ALPHANUMERIC_PATTERN, UNDERSCORE));
                        writer.writeCharacters(row.getFv());
                        writeEndElement(writer);
                    }
                    */
                }

                // The score field, along with EB-eye Lucene 'relevance' measure,
                // will be used to determine the order of genes returned by the search.
                // According to the score below, the more experiments a given gene was differentially
                // expressed in, the more relevant it is (irrespective of the search criteria).
                writer.writeStartElement("field");
                writer.writeAttribute("name", "score");
                writer.writeCharacters(geneDescription.getTotalExperiments() + "");
                writeEndElement(writer);

                // Output descriptive text relating to each experimmental factor
                // in efToText.keySet() in a separate field
                // The indexed ef fields' names will end in '_indexed' e.g. 'celline_indexed'). These
                // indexed fields will contain all the efvs relevant to that ef, in order to
                // make searches more effective.
                Map<String, String> efToIndexedText = geneDescription.getEfToIndexedText();
                for (String efName : efToIndexedText.keySet()) {
                    writer.writeStartElement("field");
                    writer.writeAttribute("name", efName);
                    writer.writeCharacters(efToIndexedText.get(efName));
                    writeEndElement(writer);
                }
                 // The displayed ef fields' names will end in '_displayed e.g. 'organismpart_displayed').
                 // These indexed fields will contain nly the first two efvs relevant to that ef, in order to
                 // make search result screen less cluttered
                // (e.g. <organismpart_displayed>cerebellum, brainstem, ... (6 more);</organismpart_displayed>
                Map<String, String> efToDisplayedText = geneDescription.getEfToDisplayedText();
                for (String efName : efToDisplayedText.keySet()) {
                    writer.writeStartElement("field");
                    writer.writeAttribute("name", efName);
                    writer.writeCharacters(efToDisplayedText.get(efName));
                    writeEndElement(writer);
                }

                writeEndElement(writer); // add'l fields

                writeEndElement(writer); // entry

                if (0 == (++i % 100)) {
                    writer.flush();
                    log.debug("Wrote " + i + " genes");
                }

            }
            writeEndElement(writer); // entries
            writer.writeEndDocument();
            writer.writeCharacters("\n");
            writer.flush();
            outputStream.closeEntry(); // Close current Zip file entry

        } catch (XMLStreamException e) {
            log.error("Failed to dump gene identifiers from index", e);
        } catch (IOException e) {
            log.error("Couldn't write to " + ebeyeDumpFile.getAbsolutePath(), e);
        } finally {
            try {
                if (null != writer) writer.close();
            } catch (Exception x) {
                log.error("Failed to close XMLStreamWriter", x);
            }

            log.info("Writing ebeye file from index to " + ebeyeDumpFile + " - done");
        }
    }

   /**
     * Add experiments xml file to zip file:  outputStream
     * @param outputStream ZipOutputStream
     */
    private void dumpExperimentsForEbeye(ZipOutputStream outputStream) {
        XMLOutputFactory output = null;
        XMLStreamWriter writer = null;


        try {
            String experimentDumpFileName = atlasProperties.getExperimentsDumpEbeyeFilename();
            log.info("Writing " + experimentDumpFileName + " to " + ebeyeDumpFile);
            outputStream.putNextEntry(new ZipEntry(experimentDumpFileName));
            output = XMLOutputFactory.newInstance();


            Map<Long, AtlasExperiment> idToExperiment = getidToExperimentMapping();

            writer = output.createXMLStreamWriter(outputStream);
            writeHeader(writer);

            writer.writeStartElement("entry_count");
            writer.writeCharacters(String.valueOf(idToExperiment.keySet().size()));
            writeEndElement(writer);

            writer.writeStartElement("entries");

            int i = 0;

            for (Long experimentId : idToExperiment.keySet()) {
                AtlasExperiment experiment = idToExperiment.get(experimentId);

                writer.writeStartElement("entry");
                writer.writeAttribute("id", experiment.getAccession());

                writer.writeStartElement("name");
                writer.writeCharacters(experiment.getAccession());
                writeEndElement(writer);

                writer.writeStartElement("description");
                writer.writeCharacters(experiment.getDescription());
                writeEndElement(writer);

                writer.writeStartElement("dates");

                writer.writeStartElement("date");
                writer.writeAttribute("type", "creation");
                writer.writeAttribute("value", new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writeEndElement(writer);

                writer.writeStartElement("date");
                writer.writeAttribute("type", "last_modification");
                writer.writeAttribute("value", new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
                writeEndElement(writer);

                writeEndElement(writer); // dates

                writer.writeStartElement("cross_references");
                writer.writeStartElement("ref");
                writer.writeAttribute("dbname", "arrayexpress");
                writer.writeAttribute("dbkey", idToExperiment.get(experimentId).getAccession());
                writeEndElement(writer);
                writeEndElement(writer); // xrefs

                writeEndElement(writer); // entry

                if (0 == (++i % 100)) {
                    writer.flush();
                    log.debug("Wrote " + i + " experiments");
                }
            }

            writeEndElement(writer); // entries
            writer.writeEndDocument();
            writer.writeCharacters("\n");
            writer.flush();
            outputStream.closeEntry(); // Close current Zip file entry

        } catch (XMLStreamException e) {
            log.error("Failed to dump gene identifiers from index", e);
        } catch (IOException e) {
            log.error("Couldn't write to " + ebeyeDumpFile.getAbsolutePath(), e);
        } finally {
            try {
                if (null != writer) writer.close();
            } catch (Exception x) {
                log.error("Failed to close XMLStreamWriter", x);
            }

            log.info("Writing ebeye file from index to " + ebeyeDumpFile + " - done");
        }
    }

    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
