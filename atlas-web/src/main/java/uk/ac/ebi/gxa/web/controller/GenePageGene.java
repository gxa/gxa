/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.controller;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.model.AtlasGeneDescription;
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.*;

/**
 * @author Olga Melnichuk
 */
public class GenePageGene {

    private final AtlasGene atlasGene;
    private final String geneDescription;
    private final Collection<String> synonyms;
    private final Collection<AtlasGene> orthologs;
    private final Collection<GeneField> geneFields;
    private final String baselineAtlasLink;

    private GenePageGene(AtlasGene atlasGene, String geneDescription, Collection<String> synonyms, Collection<AtlasGene> orthologs, Collection<GeneField> geneFields, String baselineAtlasLink) {
        this.atlasGene = atlasGene;
        this.geneDescription = geneDescription;
        this.synonyms = synonyms;
        this.orthologs = orthologs;
        this.geneFields = geneFields;
        this.baselineAtlasLink = baselineAtlasLink;
    }

    public static GenePageGene create(AtlasGene atlasGene, AtlasProperties atlasProperties, GeneSolrDAO geneSolrDAO, AtlasStatisticsQueryService atlasStatisticsQueryService) {
        Collection<String> synonyms = findSynonyms(atlasGene, atlasProperties);
        Collection<AtlasGene> orthologs = findOrthologs(atlasGene, geneSolrDAO);
        Collection<GeneField> geneFields = findGeneFields(atlasGene, atlasProperties);
        String geneDescription = new AtlasGeneDescription(atlasProperties, atlasGene, getEnsemblDescription(geneFields), atlasStatisticsQueryService).toString();
        String baselineAtlasLink = !Strings.isNullOrEmpty(atlasGene.getGeneSpecies()) ?
                atlasProperties.getBaselineAtlasLink(atlasGene.getGeneSpecies().toLowerCase().replaceAll(" ", "_")) : null;

        return new GenePageGene(
                atlasGene,
                geneDescription,
                synonyms,
                orthologs,
                geneFields,
                baselineAtlasLink);
    }

    public String getGeneName() {
        return atlasGene.getGeneName();
    }

    public String getGeneSpecies() {
        return atlasGene.getGeneSpecies();
    }


    public String getBaselineAtlasLink() {
        return baselineAtlasLink;
    }

    public String getGeneIdentifier() {
        return atlasGene.getGeneIdentifier();
    }

    public int getGeneId() {
        return atlasGene.getGeneId();
    }

    public String getGeneDescription() {
        return geneDescription;
    }

    public Collection<String> getSynonyms() {
        return Collections.unmodifiableCollection(synonyms);
    }

    public Collection<AtlasGene> getOrthologs() {
        return Collections.unmodifiableCollection(orthologs);
    }

    public Collection<GeneField> getGeneFields() {
        return geneFields;
    }

    private static Collection<GeneField> findGeneFields(AtlasGene atlasGene, AtlasProperties atlasProperties) {
        Map<String, String> curatedFieldNames = atlasProperties.getCuratedGeneProperties();
        List<String> defaultFields = atlasProperties.getGenePageDefaultFields();
        Map<String, String> fieldUrlTemplates = atlasProperties.getGenePropertyLinks();

        List<String> ignoreFields = new ArrayList<String>();
        ignoreFields.addAll(atlasProperties.getGeneAutocompleteNameFields());
        ignoreFields.addAll(atlasProperties.getGenePageIgnoreFields());

        Map<String, Collection<String>> geneProperties = atlasGene.getGeneProperties();

        List<GeneField> fields = new ArrayList<GeneField>();
        for (Iterator<String> iter = atlasGene.getGenePropertiesIterator(); iter.hasNext();) {
            String field = iter.next();
            if (ignoreFields.contains(field)) {
                continue;
            }
            fields.add(new GeneField(
                    field,
                    curatedFieldNames.get(field),
                    defaultFields.contains(field),
                    fieldUrlTemplates.get(field),
                    geneProperties.get(field)));
        }

        // add extra properties, for example emage_id
        final Map<String, String> extraGeneProperties = atlasProperties.getExtraGeneProperties();
        for (String extraProp : extraGeneProperties.keySet()) {
            fields.add(new GeneField(
                    extraProp,
                    curatedFieldNames.get(extraProp),
                    defaultFields.contains(extraProp),
                    fieldUrlTemplates.get(extraProp),
                    geneProperties.get(extraGeneProperties.get(extraProp))));
        }

        return fields;
    }

    private static String getEnsemblDescription(Collection<GeneField> fields) {
        String ensemblGeneDescription = null;
        for (GeneField field : fields) {
           if (field.getName().equalsIgnoreCase("description")) {
               ensemblGeneDescription = StringUtils.join(field.getValues(), ", ");
           }
        }
        return ensemblGeneDescription;
    }

    private static Collection<AtlasGene> findOrthologs(AtlasGene atlasGene, GeneSolrDAO geneSolrDAO) {
        return geneSolrDAO.getOrthoGenes(atlasGene);
    }

    private static Collection<String> findSynonyms(AtlasGene atlasGene, AtlasProperties atlasProperties) {
        List<String> synonyms = new ArrayList<String>();
        for (String prop : atlasProperties.getGeneAutocompleteNameFields()) {
            synonyms.addAll(atlasGene.getGeneProperties().get(prop));
        }
        return synonyms;
    }

    public static class GeneField {
        private final String name;
        private final boolean isDefault;
        private final String urlTemplate;
        private final List<String> values = new ArrayList<String>();

        public GeneField(String name, String curatedName, boolean isDefault, String valueUrlTemplate, Collection<String> values) {
            this.name = curatedName == null ? name : curatedName;
            this.isDefault = isDefault;
            this.urlTemplate = valueUrlTemplate;
            this.values.addAll(values);
        }

        public String getName() {
            return name;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public boolean isUrlAware() {
            return !Strings.isNullOrEmpty(urlTemplate);
        }

        public Collection<String> getValues() {
            return Collections.unmodifiableCollection(values);
        }

        public Collection<FieldValueWithUrl> getValuesWithUrl() {
            List<FieldValueWithUrl> list = new ArrayList<FieldValueWithUrl>();
            for (String v : values) {
                list.add(new FieldValueWithUrl(v, urlTemplate));
            }
            return list;
        }
    }

    public static class FieldValueWithUrl {
        private final String value;
        private final String url;

        public FieldValueWithUrl(String value, String urlTemplate) {
            this.value = value;
            this.url = urlTemplate.replace("$$", value);
        }

        public String getValue() {
            return value;
        }

        public String getUrl() {
            return url;
        }
    }
}
