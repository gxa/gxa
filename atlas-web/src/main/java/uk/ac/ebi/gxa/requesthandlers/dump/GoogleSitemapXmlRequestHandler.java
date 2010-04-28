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
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;


/**
 * Prepares for and allows downloading of Google Sitemap XML files
 */
public class GoogleSitemapXmlRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, DisposableBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private CoreContainer coreContainer;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;
    
    private File sitemapIndexFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "geneSitemapIndex.xml");

    public CoreContainer getCoreContainer() {
        return coreContainer;
    }

    public void setCoreContainer(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
    }

    public File getSitemapIndexFile() {
        return sitemapIndexFile;
    }

    public void setSitemapIndexFile(File sitemapIndexFile) {
        this.sitemapIndexFile = sitemapIndexFile;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasProperties(uk.ac.ebi.gxa.properties.AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene sitemap download request");
        if(!sitemapIndexFile.exists())
            writeGeneSitemap();
        if(httpServletRequest.getPathInfo().contains("Index"))
            FileDownloadServer.processRequest(sitemapIndexFile, "text/xml", httpServletRequest, httpServletResponse);
        else {
            String name = httpServletRequest.getPathInfo();
            File file = new File(sitemapIndexFile.getParentFile(), name);
            FileDownloadServer.processRequest(file, "application/x-gzip", httpServletRequest, httpServletResponse);
        }
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        if(sitemapIndexFile.exists()) {
            sitemapIndexFile.delete();
            int i = 0;
            while(true) {
                File file = new File(sitemapIndexFile.getParentFile(), "geneSitemap" + (i++) + ".xml.gz");
                if(!file.exists())
                    break;
                file.delete();
            }
        }
        if(atlasProperties.isGeneListAfterIndexAutogenerate())
            writeGeneSitemap();
    }

    public void onIndexBuildStart(IndexBuilder builder) {

    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     *
     */
    void writeGeneSitemap() {
        SolrCore core = null;
        try {
            core = getCoreContainer().getCore("atlas");

            RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            IndexReader r = searcher.get().getIndexReader();

            log.info("Generating gene sitemap, index in " + sitemapIndexFile);
            BufferedOutputStream bfind = new BufferedOutputStream(new FileOutputStream(sitemapIndexFile));

            bfind.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
            bfind.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n".getBytes("UTF-8"));

            List<String> geneids = atlasProperties.getDumpGeneIdFields();

            TermEnum terms = r.terms();

            int c = 0;
            GZIPOutputStream gzout = null;
            while (terms.next()) {
                if (0 == c % 50000) {
                    if (null != gzout) {
                        gzout.write("</urlset>".getBytes("UTF-8"));
                        gzout.close();
                        gzout = null;
                        log.info("Generating gene sitemap, geneSitemap" + (c/50000) + ".xml.gz has been written");
                        if (c > 0) {
                            bfind.write(
                                    ("<sitemap><loc>http://www.ebi.ac.uk/gxa/sitemap/geneSitemap" + (c / 50000 - 1) +
                                            ".xml.gz</loc></sitemap>\n").getBytes("UTF-8"));
                        }
                    }
                }

                Term t = terms.term();
                String f = t.field();

                if (f.startsWith("property_f_") && geneids.contains(f.substring("property_f_".length()))) {
                    if(gzout == null) {
                        gzout = new GZIPOutputStream(
                                new BufferedOutputStream(
                                        new FileOutputStream(
                                                new File(sitemapIndexFile.getParentFile(), "geneSitemap" + c / 50000 + ".xml.gz")
                                        )
                                )
                        );
                        gzout.write(
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n".getBytes(
                                        "UTF-8"));
                    }
                    gzout.write(("<url><loc>http://www.ebi.ac.uk/gxa/gene/" + t.text() + "</loc></url>\n").getBytes(
                            "UTF-8"));
                    c++;
                }
            }

            if (gzout != null) {
                gzout.write("</urlset>".getBytes("UTF-8"));
                gzout.close();
                bfind.write(("<sitemap><loc>http://www.ebi.ac.uk/gxa/sitemap/geneSitemap" + (c / 50000) +
                        ".xml.gz</loc></sitemap>\n").getBytes("UTF-8"));
            }

            bfind.write("</sitemapindex>".getBytes("UTF-8"));

            searcher.decref();
            bfind.close();
            log.info("Generating gene sitemap, index in " + sitemapIndexFile + " - done");
        }
        catch (IOException e) {
            log.error("Failed to create gene sitemap from index", e);
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
