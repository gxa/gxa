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
import uk.ac.ebi.gxa.dao.PropertyValueDAO;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.io.IOException;
import java.util.Collection;

/**
 * @author pashky
 */
public class PropertiesIndexBuilderService extends IndexBuilderService {
    private PropertyValueDAO pvdao;

    public PropertiesIndexBuilderService(PropertyValueDAO pvdao) {
        this.pvdao = pvdao;
    }

    @Override
    public void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        try {
            getLog().info("Fetching all properties");

            Collection<PropertyValue> properties = pvdao.getAllPropertyValues();
            int total = properties.size();
            int num = 0;
            for (PropertyValue pv : properties) {
                SolrInputDocument solrInputDoc = new SolrInputDocument();
                solrInputDoc.addField("property_id", pv.getDefinition().getId());
                solrInputDoc.addField("value_id", pv.getId());
                solrInputDoc.addField("property", pv.getDefinition().getName());
                solrInputDoc.addField("value", pv.getValue());
                solrInputDoc.addField("pvalue_" + EscapeUtil.encode(pv.getDefinition().getName()), pv.getValue());
                getLog().debug("Adding property " + pv);
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

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, ProgressUpdater progressUpdater) throws IndexBuilderException {
        processCommand(new IndexAllCommand(), progressUpdater);
    }


    public String getName() {
        return "properties";
    }
}
