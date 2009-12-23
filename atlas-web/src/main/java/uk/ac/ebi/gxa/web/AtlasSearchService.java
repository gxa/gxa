package uk.ac.ebi.gxa.web;

import ae3.dao.AtlasDao;
import ae3.service.AtlasDownloadService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;
import uk.ac.ebi.microarray.atlas.model.Species;
import uk.ac.ebi.ae3.indexbuilder.efo.Efo;

import java.io.File;
import java.util.*;

/**
 * A service that is used to perform various Atlas searches.  This needs to be wired with several data access classes,
 * as searches are performed against the Atlas database and the Atlas SOLR index.  There are also several nested
 * services that can be acquired from this class for doing download, compute, and query operations.
 * <p/>
 *
 * @author Misha Kapushesky (original)
 * @author Tony Burdett
 * @date 12-Feb-2008
 */
public class AtlasSearchService implements InitializingBean {
    private CoreContainer multiCore;

    private AtlasComputeService atlasComputeService;
    private AtlasDownloadService atlasDownloadService;
    private AtlasStructuredQueryService atlasQueryService;

    private File atlasIndex;
    private AtlasDAO atlasDatabaseDAO;
    private AtlasDao atlasSolrDAO;

    private AtlasStatistics stats;

    // logging
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public CoreContainer getMultiCore() {
        return multiCore;
    }

    public void setMultiCore(CoreContainer multiCore) {
        this.multiCore = multiCore;
    }

    public AtlasComputeService getAtlasComputeService() {
        return atlasComputeService;
    }

    public void setAtlasComputeService(AtlasComputeService atlasComputeService) {
        this.atlasComputeService = atlasComputeService;
    }

    public AtlasDownloadService getAtlasDownloadService() {
        return atlasDownloadService;
    }

    public void setAtlasDownloadService(AtlasDownloadService atlasDownloadService) {
        this.atlasDownloadService = atlasDownloadService;
    }

    public AtlasStructuredQueryService getAtlasQueryService() {
        return atlasQueryService;
    }

    public void setAtlasQueryService(AtlasStructuredQueryService atlasQueryService) {
        this.atlasQueryService = atlasQueryService;
    }

    public File getAtlasIndex() {
        return atlasIndex;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public AtlasDAO getAtlasDatabaseDAO() {
        return atlasDatabaseDAO;
    }

    public void setAtlasDatabaseDAO(AtlasDAO atlasDatabaseDAO) {
        this.atlasDatabaseDAO = atlasDatabaseDAO;
    }

    public AtlasDao getAtlasSolrDAO() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDAO(AtlasDao atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void afterPropertiesSet() throws Exception {
        initialize();
    }

    public void initialize() {
        try {
            System.out.println("Initializing AtlasSearchService");

            // fixme: this shuld be configured low-level, e.g. in a DAO module and injected
            // startup SOLR server
            multiCore = new CoreContainer(atlasIndex.getAbsolutePath(), new File(atlasIndex, "solr.xml"));

            // create a SOLR DAO
            atlasSolrDAO = new AtlasDao(multiCore);

            // do required additional setup for SOLR things that weren't set by spring
            atlasQueryService = new AtlasStructuredQueryService(multiCore);
            atlasDownloadService.setAtlasQueryService(atlasQueryService);

            // do some precanned queries the interface requires
            try {
                String dataRelease = AtlasProperties.getProperty("atlas.data.release");
                stats = getAtlasDatabaseDAO().getAtlasStatisticsByDataRelease(dataRelease);
            }
            catch (Exception e) {
                log.error("Statistics failed", e);
            }

            new Thread() {
                public void run() {
                    atlasQueryService.getEfvListHelper().preloadData();
                }
            }.start();
            new Thread() {
                public void run() {
                    atlasQueryService.getEfoListHelper().preloadData();
                }
            }.start();
            new Thread() {
                public void run() {
                    atlasQueryService.getGeneListHelper().preloadData();
                }
            }.start();
        }
        catch (Exception e) {
            throw new RuntimeException("Fatal initialization error", e);
        }
    }

    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down AtlasSearchService.");

        atlasComputeService.shutdown();
        atlasComputeService = null;

        atlasDownloadService.shutdown();
        atlasQueryService = null;

        Efo.getEfo().close();

        log.info("Shutting down DB connections and indexes");
        try {
            if (multiCore != null) {
                multiCore.shutdown();
                multiCore = null;
            }
        }
        catch (Exception e) {
            log.error("Error shutting down AtlasSearchService!", e);
        }
    }

    public AtlasStatistics getStats() {
        return stats;
    }

    /**
     * Gets the Solr core from the main core container. Don't forget to close when finished with it.
     *
     * @param coreName core to return
     * @return SolrCore
     */
    public SolrCore getSolrCore(final String coreName) {
        return multiCore.getCore(coreName);
    }


}
