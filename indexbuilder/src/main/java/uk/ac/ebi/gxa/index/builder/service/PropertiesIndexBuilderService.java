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
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.io.IOException;
import java.util.List;

/**
 * @author pashky
 */
public class PropertiesIndexBuilderService extends IndexBuilderService {
    private PropertyDAO pdao;

    public PropertiesIndexBuilderService(PropertyDAO pdao) {
        this.pdao = pdao;
    }

    @Override
    public void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        getAtlasDAO().startSession();
        try {
            getLog().info("Fetching all properties");

            List<Property> properties = pdao.getAll();
            int total = properties.size();
            int num = 0;
            for (Property property : properties) {
                for (PropertyValue value : property.getValues()) {
                    addProperty(value);
                }
                ++num;
                progressUpdater.update(num + "/" + total);
            }
            getLog().info("Properties index builder finished");
        } catch (IOException e) {
            throw new IndexBuilderException(e);
        } catch (SolrServerException e) {
            throw new IndexBuilderException(e);
        } finally {
            getAtlasDAO().finishSession();
        }
    }

    private void addProperty(PropertyValue value) throws SolrServerException, IOException {
        Property property = value.getDefinition();

        SolrInputDocument solrInputDoc = new SolrInputDocument();
        solrInputDoc.addField("property_id", property.getId());
        solrInputDoc.addField("value_id", value.getId());
        solrInputDoc.addField("property", property.getName());
        solrInputDoc.addField("value", value.getValue());
        solrInputDoc.addField("pvalue_" + EscapeUtil.encode(property.getName()), value.getValue());
        getLog().debug("Adding property " + value);
        getSolrServer().add(solrInputDoc);
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, ProgressUpdater progressUpdater) throws IndexBuilderException {
        processCommand(new IndexAllCommand(), progressUpdater);
    }


    public String getName() {
        return "properties";
    }
}
