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

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasExperimentImpl;
import ae3.model.AtlasGene;
import ae3.model.AtlasGeneDescription;
import ae3.service.AtlasStatisticsQueryService;
import ae3.util.FileDownloadServer;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.ExperimentInfo;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.tempFile;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String UNDERSCORE = "_";
    // No alphanumeric characters may break Lucene indexing - the literal below will be used to replace
    // them with UNDERSCORE
    private static final String NON_ALPHANUMERIC_PATTERN = "[^a-zA-Z0-9]+";

    private File ebeyeDumpFile;
    private GeneSolrDAO geneSolrDAO;
    private ExperimentSolrDAO experimentSolrDAO;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    // Constant used for testing if a <gene name> == GENE_PREAMBLE + <gene id>; This seems to be a case
    // of a gene that is loaded from an experiment but cannot be mapped to any gene currently in A2_gene table.
    // In such cases we don't output gene name into the EB-eye gene dump - in an effort to avoid redundancy.
    private static final String GENE_PREAMBLE = "GENE:";
    private static final String PIPE = "|";

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void afterPropertiesSet() throws Exception {
        if (ebeyeDumpFile == null)
            ebeyeDumpFile = tempFile(atlasProperties.getDumpEbeyeFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene ebeye dump download request");
        synchronized (this) {
            if (!ebeyeDumpFile.exists())
                dumpEbeyeData();
        }
        FileDownloadServer.processRequest(ebeyeDumpFile, "application/x-zip", httpServletRequest, httpServletResponse);
    }

    public void onIndexBuildFinish() {
        if (!ebeyeDumpFile.delete())
            log.warn("Cannot delete " + ebeyeDumpFile.getAbsolutePath());
        if (atlasProperties.isGeneListAfterIndexAutogenerate())
            dumpEbeyeData();
    }

    public void onIndexBuildStart() {

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
            closeQuietly(outputStream);
        }
    }

    private Map<Long,Experiment> getidToExperimentMapping() {
        // Used LinkedHashMap to preserve order of insertion
        Map<Long,Experiment> idToExperiment = new LinkedHashMap<Long,Experiment>();
        Collection<ExperimentInfo> scoringExperiments = atlasStatisticsQueryService.getScoringExperiments(StatisticsType.UP_DOWN);
        Collection<Long> ids = transform(scoringExperiments, new Function<ExperimentInfo, Long>() {
            public Long apply(@Nonnull ExperimentInfo input) {
                return input.getExperimentId();
            }
        });
        List<AtlasExperimentImpl> experiments = experimentSolrDAO.getExperiments(ids);
        for (AtlasExperimentImpl exp : experiments) {
            idToExperiment.put(exp.getExperiment().getId(), exp.getExperiment());
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
     *
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
     *
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
            writer.writeCharacters(String.valueOf(geneSolrDAO.getGeneCount()));
            writeEndElement(writer);


            Map<Long,Experiment> idToExperiment = getidToExperimentMapping();

            writer.writeStartElement("entries");

            int i = 0;
            for (AtlasGene gene : geneSolrDAO.getAllGenes()) {
                Map<String, Collection<String>> geneprops = gene.getGeneProperties();

                writer.writeStartElement("entry");
                String geneName = gene.getGeneName();
                String geneId = gene.getGeneIdentifier();

                writer.writeAttribute("id", geneId);

                if (!geneName.equals(GENE_PREAMBLE + geneId)) {
                    // Don't output the gene name, as it is almost identical to its id
                    writer.writeStartElement("name");
                    writer.writeCharacters(geneName);
                    writeEndElement(writer);
                }

                AtlasGeneDescription geneDescription = new AtlasGeneDescription(atlasProperties, gene, atlasStatisticsQueryService);

                writer.writeStartElement("description");
                writer.writeCharacters(geneDescription.toStringExperimentCount());
                writeEndElement(writer);

                writer.writeStartElement("keywords");
                writer.writeCharacters(gene.getPropertyValue("keyword", PIPE));
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
                            writer.writeAttribute("dbname", geneIdField.replaceAll(NON_ALPHANUMERIC_PATTERN, UNDERSCORE));
                            writer.writeAttribute("dbkey", propval);
                            writeEndElement(writer);
                        }
                    }
                }
                // Cross-reference gene to experiments
                Set<Long> experimentIds = gene.getExperimentIds(atlasStatisticsQueryService);
                for (Long experimentId : experimentIds) {
                    writer.writeStartElement("ref");
                    writer.writeAttribute("dbname", "atlas");
                    writer.writeAttribute("dbkey", idToExperiment.get(experimentId).getAccession());
                    writeEndElement(writer);
                }

                writeEndElement(writer); // xrefs

                writer.writeStartElement("additional_fields");
                for (Map.Entry<String, Collection<String>> geneprop : geneprops.entrySet()) {
                    if (!atlasProperties.getDumpGeneIdFields().contains(geneprop.getKey()) && // The field has not already been output above
                            !atlasProperties.getDumpExcludeFields().contains(geneprop.getKey())) { // The field is not explicitly excluded
                        writer.writeStartElement("field");
                        writer.writeAttribute("name", geneprop.getKey().replaceAll(NON_ALPHANUMERIC_PATTERN, UNDERSCORE));
                        writer.writeCharacters(StringUtils.join(geneprop.getValue(), PIPE));
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
                writer.writeAttribute("name", "number_of_experiments");
                writer.writeCharacters(geneDescription.getTotalExperiments() + "");
                writeEndElement(writer);

                // Output descriptive text relating to each experimmental factor
                // in efToText.keySet() in a separate field
                Map<String, String> efToIndexedText = geneDescription.getEfToEbeyeDumpText();
                for (Map.Entry<String, String> entry : efToIndexedText.entrySet()) {
                    writer.writeStartElement("field");
                    writer.writeAttribute("name", entry.getKey());
                    writer.writeCharacters(entry.getValue());
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
            } catch (XMLStreamException x) {
                log.error("Failed to close XMLStreamWriter", x);
            }

            log.info("Writing ebeye file from index to " + ebeyeDumpFile + " - done");
        }
    }

    /**
     * Add experiments xml file to zip file:  outputStream
     *
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


            Map<Long,Experiment> idToExperiment = getidToExperimentMapping();

            writer = output.createXMLStreamWriter(outputStream);
            writeHeader(writer);

            writer.writeStartElement("entry_count");
            writer.writeCharacters(String.valueOf(idToExperiment.keySet().size()));
            writeEndElement(writer);

            writer.writeStartElement("entries");

            int i = 0;

            for (Map.Entry<Long,Experiment> entry : idToExperiment.entrySet()) {
                final Experiment experiment = entry.getValue();

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
                writer.writeAttribute("dbkey", experiment.getAccession());
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
            } catch (XMLStreamException x) {
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
