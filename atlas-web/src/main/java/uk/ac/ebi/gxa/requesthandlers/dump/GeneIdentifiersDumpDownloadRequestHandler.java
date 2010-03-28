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

import ae3.util.FileDownloadServer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneIdentifiersDumpDownloadRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, InitializingBean, DisposableBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private File dumpGeneIdsFile;
    private CoreContainer coreContainer;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    private static final String PROPERTY = "property_f_";

    public CoreContainer getCoreContainer() {
        return coreContainer;
    }

    public void setCoreContainer(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
    }

    public File getDumpGeneIdsFile() {
        return dumpGeneIdsFile;
    }

    public void setDumpGeneIdsFile(File dumpGeneIdsFile) {
        this.dumpGeneIdsFile = dumpGeneIdsFile;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasProperties(uk.ac.ebi.gxa.properties.AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void afterPropertiesSet() throws Exception {
        if(dumpGeneIdsFile == null)
            dumpGeneIdsFile = new File(System.getProperty("java.io.tmpdir") + File.separator + atlasProperties.getDumpGeneIdentifiersFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene identifiers dump download request");
        if(!dumpGeneIdsFile.exists())
            dumpGeneIdentifiers();
        FileDownloadServer.processRequest(dumpGeneIdsFile, "text/plain", httpServletRequest, httpServletResponse);
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
    void dumpGeneIdentifiers() {
        SolrCore core = null;
        try {
            core = getCoreContainer().getCore("atlas");

            RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            IndexReader r = searcher.get().getIndexReader();

            log.info("Writing gene ids file from index to " + dumpGeneIdsFile);

            BufferedWriter out = new BufferedWriter(new FileWriter(dumpGeneIdsFile));

            List<String> geneids = atlasProperties.getDumpGeneIdFields();

            TermEnum terms = r.terms();

            while (terms.next()) {
                Term t = terms.term();
                String f = t.field();

                if (f.startsWith(PROPERTY) && geneids.contains(f.substring(PROPERTY.length()))) {
                    out.write(f.substring(PROPERTY.length()) + "\t" + t.text());
                    out.newLine();
                }
            }

            searcher.decref();
            out.close();

            log.info("Writing gene ids file from index to " + dumpGeneIdsFile + " - done");
        }
        catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
        finally {
            if(core != null)
                core.close();
        }
    }

    public void destroy() throws Exception {
        if(indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
