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

package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author pashky
 */
public class PropertiesIndexBuilderService extends IndexBuilderService {

    @Override
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

    /**
     * Generate/update only documents for a selection of id's.
     *
     * @param docIds          document id's to update
     * @param progressUpdater instance of {@link uk.ac.ebi.gxa.index.builder.service.IndexBuilderService.ProgressUpdater} to track progress
     * @throws uk.ac.ebi.gxa.index.builder.IndexBuilderException
     *          thrown if an error occurs
     */
    @Override
    protected void updateIndexDocs(Collection<Long> docIds,
                                   ProgressUpdater progressUpdater) throws IndexBuilderException {
        throw new RuntimeException("Not implemented");
    }

    public String getName() {
        return "properties";
    }

}
