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

package uk.ac.ebi.gxa.requesthandlers.api.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.util.*;
import java.io.IOException;

class GenesQueryHandler implements QueryHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final GeneSolrDAO geneSolrDAO;

    GenesQueryHandler(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    private static class GeneDecorator {
        final String name;
        final String identifier;

        GeneDecorator(AtlasGene gene) {
            this.name = gene.getGeneName();
            this.identifier = gene.getGeneIdentifier();
        }

        public String getName() {
            return name;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    public Object getResponse(Map query) {
        final Object geneNames = query.get("geneNames");
        final Object geneIdentifiers = query.get("geneIdentifiers");
        if (geneNames == null && geneIdentifiers == null) {
            return new Error("Gene set is not specified; you must specify geneNames or geneIdentifiers field");
        } else if (geneNames != null && geneIdentifiers != null) {
            return new Error("You cannot specify geneNames and geneIdentifiers in the same request");
        }
        final boolean useGeneNames = geneNames != null;
        Object value = useGeneNames ? geneNames : geneIdentifiers;
        if (!(value instanceof List) && !"*".equals(value)) {
            return new Error("Gene set must be a list or \"*\" pattern");
        }
        if (value instanceof List) {
            for (Object g : (List)value) {
                if (!(g instanceof String)) {
                    return new Error("All gene names (identifiers) must be a strings");
                }
            }
        }
        final List<String> geneStrings = (value instanceof List) ? (List<String>)value : null;

        final List<GeneDecorator> data = new LinkedList<GeneDecorator>();
        if (geneStrings != null) {
            if (useGeneNames) {
                for (String geneName : geneStrings) {
                    for (AtlasGene g : geneSolrDAO.getGenesByName(geneName)) {
                        data.add(new GeneDecorator(g));
                    }
                }
            } else /* use gene identifiers */ {
                for (AtlasGene g : geneSolrDAO.getGenesByIdentifiers(geneStrings)) {
                    data.add(new GeneDecorator(g));
                }
            }
        } else {
            for (AtlasGene g : geneSolrDAO.getAllGenes()) {
                data.add(new GeneDecorator(g));
            }
        }

        return data;
    }
}
