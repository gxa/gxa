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

package uk.ac.ebi.gxa.model.impl;

import java.util.*;

/**
 * @author pashky
 */
public class GeneProperties {
    public static final String GENE_PROPERTY_NAME = "gene";

    public static enum PropType {
        NAME(5), ID(3), DESC(-1), IGNORE(0);

        public int limit;
        private PropType(int limit) {
            this.limit = limit;
        }
    }

    public static class Prop {
        public Prop(String id, String facetField, String searchField, PropType type, boolean drilldown) {
            this.id = id;
            this.facetField = facetField;
            this.searchField = searchField;
            this.type = type;
            this.drilldown = drilldown;
        }

        public String id;
        public String facetField;
        public String searchField;
        public PropType type;
        public boolean drilldown;
    }

    private static final Prop[] GENE_PROPS = {
        new Prop("dbxref",          "property_DBXREF",              "property_DBXREF", PropType.ID, false),
        new Prop("embl",            "property_EMBL",                "property_EMBL", PropType.ID, false),
        new Prop("ensfamily",       "property_ENSFAMILY",           "property_ENSFAMILY", PropType.ID, false),
        new Prop("ensgene",         "property_ENSGENE",             "property_ensgene", PropType.ID, false),
        new Prop("ensprotein",      "property_ENSPROTEIN",          "property_ENSPROTEIN", PropType.ID, false),
        new Prop("enstranscript",   "property_ENSTRANSCRIPT",       "property_ENSTRANSCRIPT", PropType.ID, false),
        new Prop("goid",            "property_GOID",                "property_GOID", PropType.ID, false),
        new Prop("image",           "property_IMAGE",               "property_IMAGE", PropType.ID, false),
        new Prop("interproid",      "property_INTERPROID",          "property_INTERPROID", PropType.ID, false),
        new Prop("locuslink",       "property_LOCUSLINK",           "property_LOCUSLINK", PropType.ID, false),
        new Prop("omimid",          "property_OMIMID",              "property_OMIMID", PropType.ID, false),
        new Prop("orf",             "property_ORF",                 "property_ORF", PropType.ID, false),
        new Prop("ortholog",        "property_ORTHOLOG",            "property_ORTHOLOG", PropType.ID, false),
        new Prop("refseq",          "property_REFSEQ",              "property_REFSEQ", PropType.ID, false),
        new Prop("unigene",         "property_UNIGENE",             "property_UNIGENE", PropType.ID, false),
        new Prop("uniprot",         "property_UNIPROT",             "property_UNIPROT", PropType.ID, false),
        new Prop("hmdb",            "property_HMDB",                "property_HMDB", PropType.ID, false),
        new Prop("chebi",           "property_CHEBI",               "property_CHEBI", PropType.ID, false),
        new Prop("cas",             "property_CAS",                 "property_CAS", PropType.ID, false),
        new Prop("uniprotmetenz",   "property_UNIPROTMETENZ",       "property_UNIPROTMETENZ", PropType.ID, false),

        new Prop("name",            "name_f",                       "name_f", PropType.NAME, false),
        new Prop("synonym",         "property_SYNONYM",             "property_SYNONYM", PropType.NAME, false),
        new Prop("identifier",      "identifier",                   "identifier", PropType.NAME, false),

        new Prop("disease",         "property_f_DISEASE",           "property_DISEASE", PropType.DESC, true),
        new Prop("goterm",          "property_f_GOTERM",            "property_GOTERM", PropType.DESC, true),
        new Prop("interproterm",    "property_f_INTERPROTERM",      "property_INTERPROTERM", PropType.DESC, true),
        new Prop("keyword",         "property_f_KEYWORD",           "property_keyword", PropType.DESC, true),
        new Prop("protein",         "property_f_PROTEIN",           "property_protein", PropType.DESC, true),
        new Prop("species",         "species",                      "species", PropType.IGNORE, false),
    };

    public static String convertPropertyToFacetField(String id)
    {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.facetField;
        return null;
    }

    public static String convertPropertyToSearchField(String id)
    {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.searchField;
        return null;
    }

    public static Prop[] allProperties()
    {
        return GENE_PROPS;
    }

    public static Iterable<Prop> allDrillDowns()
    {
        List<Prop> s = new ArrayList<Prop>();
        for(Prop p : GENE_PROPS)
            if(p.drilldown)
                s.add(p);
        return s;
    }

    public static Collection<String> allPropertyIds()
    {
        Collection<String> s = new ArrayList<String>();
        for(Prop p : GENE_PROPS)
            if(p.type != PropType.NAME)
                s.add(p.id);
        s.add(GENE_PROPERTY_NAME);
        return s;
    }

    public static Iterable<String> optionPropertyIds()
    {
        Collection<String> s = new ArrayList<String>();
        s.add(GENE_PROPERTY_NAME);
        for(Prop p : GENE_PROPS)
            if(p.type != PropType.NAME && p.type != PropType.IGNORE)
                s.add(p.id);
        return s;
    }

    public static Prop findPropByFacetField(String field)
    {
        for(Prop p : GeneProperties.allProperties())
            if(p.facetField.equals(field))
                return p;
        return null;
    }

    public static Prop findPropBySearchField(String field)
    {
        for(Prop p : GeneProperties.allProperties())
            if(p.searchField.equals(field))
                return p;
        return null;
    }

    public static boolean isNameProperty(String id) {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.type == PropType.NAME;
        return false;
    }
}
