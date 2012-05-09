/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.service;

import com.jamesmurty.utils.XMLBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.export.ChEbiEntry;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 01/05/2012
 */
public class ChEbiXrefExportService {

    @Autowired
    private AtlasDAO atlasDAO;

    public String exportChEbiEntries() {
        final Collection<ChEbiEntry> chEbiEntries = atlasDAO.getChEbiEntries();
        try {
            XMLBuilder xmlBuilder = XMLBuilder.create("doc")
                    .e("database_name").t("Gene Expression Atlas Database").up()
                    .e("database_description").t("The Gene Expression Atlas is a semantically enriched database of meta-analysis based summary statistics over a curated subset of ArrayExpress Archive, servicing queries for condition-specific gene expression patterns as well as broader exploratory searches for biologically interesting genes/samples.").up()
                    .e("link_url").t("http://www.ebi.ac.uk/gxa/experiment/*").up()
                    .e("entities");

            for (ChEbiEntry chEbiEntry : chEbiEntries) {
                xmlBuilder = xmlBuilder.e("entity")
                        .e("chebi_id").t(chEbiEntry.getChebiAcc()).up()
                        .e("xrefs");

                for (ChEbiEntry.ExperimentInfo info : chEbiEntry.getExperimentInfos()) {
                   xmlBuilder =  xmlBuilder.e("xref")
                            .e("display_id").t(info.getAccession()).up()
                            .e("link_id").t(info.getAccession()).up()
                            .e("name").t(info.getDescription()).up().up();
                }
                xmlBuilder = xmlBuilder.up().up();
            }

            StringBuilder sb = new StringBuilder();
            xmlBuilder.write(sb, true, 2);

            return sb.toString().trim();

        } catch (ParserConfigurationException e) {
            throw LogUtil.createUnexpected("Problem when creating ChEbi export", e);
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Problem when creating ChEbi export", e);
        }
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }
}
