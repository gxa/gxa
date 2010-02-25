package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.util.List;

/**
 * @author pashky
 */
public class PropertiesIndexBuilderService extends IndexBuilderService {

    protected void createIndexDocs(ProgressUpdater progressUpdater) throws IndexBuilderException {
        try {
            getLog().info("Fetching all properties");

            List<Property> properties = getAtlasDAO().getAllProperties();
            int total = properties.size();
            int num = 0;
            for(Property property : properties) {
                SolrInputDocument solrInputDoc = new SolrInputDocument();
                solrInputDoc.addField("property_id", property.getPropertyId());
                solrInputDoc.addField("value_id", property.getPropertyValueId());
                solrInputDoc.addField("property", property.getName());
                solrInputDoc.addField("value", property.getValue());
                solrInputDoc.addField("pvalue_" + EscapeUtil.encode(property.getName()), property.getValue());
                solrInputDoc.addField("is_fv", property.isFactorValue());
                getLog().debug("Adding property " + property.getName() + " : " + property.getValue());
                getSolrServer().add(solrInputDoc);
                ++num;
                progressUpdater.update(num + "/" + total);
            }
            getLog().info("Properties index builder finished");
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        }
    }

    public String getName() {
        return "properties";
    }

}
