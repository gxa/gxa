package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;

/**
 * @author pashky
 */
public class PropertiesIndexBuilderService extends IndexBuilderService {

    public PropertiesIndexBuilderService(AtlasDAO atlasDAO, SolrServer solrServer) {
        super(atlasDAO, solrServer);
    }

    protected void createIndexDocs(boolean pendingOnly) throws IndexBuilderException {
        try {
            getLog().info("Fetching all properties");
            for(Property property : getAtlasDAO().getAllProperties()) {
                SolrInputDocument solrInputDoc = new SolrInputDocument();
                solrInputDoc.addField("property_id", property.getPropertyId());
                solrInputDoc.addField("value_id", property.getPropertyValueId());
                solrInputDoc.addField("property", property.getName());
                solrInputDoc.addField("value", property.getValue());
                solrInputDoc.addField("pvalue_" + EscapeUtil.encode(property.getName()), property.getValue());
                solrInputDoc.addField("is_fv", property.isFactorValue());
                getLog().debug("Adding property " + property.getName() + " : " + property.getValue());
                getSolrServer().add(solrInputDoc);
            }
            getLog().info("Fetching all properties - done");
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        }
    }
    
    public static class Factory implements IndexBuilderService.Factory {
        public IndexBuilderService create(AtlasDAO atlasDAO, CoreContainer coreContainer) {
            return new PropertiesIndexBuilderService(atlasDAO, new EmbeddedSolrServer(coreContainer, "properties"));
        }

        public String getName() {
            return "properties";
        }

        public String[] getConfigFiles() {
            return getBasicConfigFilesForCore("properties");
        }
    }
}
