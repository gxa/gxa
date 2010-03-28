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
import ae3.service.XML4dbDumps;
import ae3.util.FileDownloadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, InitializingBean, DisposableBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private File dumpGeneIdsFile;
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
        if(dumpGeneIdsFile == null)
            dumpGeneIdsFile = new File(System.getProperty("java.io.tmpdir") + File.separator + atlasProperties.getDumpEbeyeFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene ebeye dump download request");
        if(!dumpGeneIdsFile.exists())
            dumpGeneIdentifiers();
        FileDownloadServer.processRequest(dumpGeneIdsFile, "text/xml", httpServletRequest, httpServletResponse);
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        dumpGeneIdsFile.delete();
        dumpGeneIdentifiers();
    }

    public void onIndexBuildStart(IndexBuilder builder) {

    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     */
    public void dumpGeneIdentifiers() {
        try {
            List<AtlasGene> genes = atlasSolrDAO.getGenes();

            XML4dbDumps.Document d1 = new XML4dbDumps.Document();
            d1.setName("Gene Expression Atlas"); //db_name
            d1.setDescription("Impressive Gene Expression Atlas");
            d1.setRelease("1.0");
            d1.setReleaseDate("29-AUG-2006");

            for (AtlasGene g : genes) {
                XML4dbDumps.Document.Entry e1 = new XML4dbDumps.Document.Entry();
                d1.getEntries().add(e1);

                e1.setId(g.getGeneIdentifier());
                e1.setAccessionNumber(g.getGeneName());
                e1.setName(g.getGeneName());
                e1.setDateCreated(atlasProperties.getDataRelease());
                e1.setDateModified(atlasProperties.getDataRelease());

                e1.setDescription("");
                e1.setAuthors("");
                e1.setKeywords("");

                XML4dbDumps.Document.Entry.Reference ref1 = new XML4dbDumps.Document.Entry.Reference();
                ref1.setDbName("db2");
                ref1.setDbKey("abc123");
                e1.getReferences().add(ref1);

                XML4dbDumps.Document.Entry.Reference ref2 = new XML4dbDumps.Document.Entry.Reference();
                ref2.setDbName("db3");
                ref2.setDbKey("abcdef");
                e1.getReferences().add(ref2);

                XML4dbDumps.Document.Entry.AdditionalField f1 = new XML4dbDumps.Document.Entry.AdditionalField();
                f1.setName("namefield1");
                f1.setValue("value1");
                e1.getAdditionalFields().add(f1);

                XML4dbDumps.Document.Entry.AdditionalField f2 = new XML4dbDumps.Document.Entry.AdditionalField();
                f2.setName("namefield2");
                f2.setValue("value2");
                e1.getAdditionalFields().add(f2);
            }

            //File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);
            //RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            //IndexReader r = searcher.get().getReader();

            log.info("Writing ebeye file from index to " + dumpGeneIdsFile);

            //BufferedWriter out = new BufferedWriter(new FileWriter(dumpGeneIdsFile));
            //List<String> geneids = Arrays.asList(StringUtils.split(AtlasProperties.getProperty("atlas.dump.geneidentifiers"), ','));
            //ByteArrayOutputStream ostream = new ByteArrayOutputStream();

            FileOutputStream ostream = new FileOutputStream(dumpGeneIdsFile);

            try {
                ostream.write(XML4dbDumps.Serialize(d1).getBytes());
            }
            catch (Exception Ex) {
                log.error("Failed to dump gene identifiers from index", Ex.getMessage());
            }
            finally {
                ostream.close();
                log.info("Writing ebeye file from index to " + dumpGeneIdsFile + " - done");
            }

        }
        catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
    }

    public void destroy() throws Exception {
        if(indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
