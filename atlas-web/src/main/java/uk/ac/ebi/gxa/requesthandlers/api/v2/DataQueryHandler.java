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

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class DataQueryHandler implements QueryHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final GeneSolrDAO geneSolrDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;

    DataQueryHandler(GeneSolrDAO geneSolrDAO, AtlasNetCDFDAO atlasNetCDFDAO) {
        this.geneSolrDAO = geneSolrDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    private static class GeneDataDecorator {
        final String geneName;
        final String designElementAccession;
        final float[] expressionLevels;

        GeneDataDecorator(String geneName, String designElementAccession, float[] expressionLevels) {
            this.geneName = geneName;
            this.designElementAccession = designElementAccession;
            this.expressionLevels = expressionLevels;
        }

        public String getGeneName() {
            return geneName;
        }

        public String getDesignElementAccession() {
            return designElementAccession;
        }

        public float[] getExpressionLevels() {
            return expressionLevels;
        }
    }

    private static class DataDecorator {
        String[] assayAccessions;
        final List<GeneDataDecorator> genes = new LinkedList<GeneDataDecorator>();

        public String[] getAssayAccessions() {
            return assayAccessions;
        }

        public List<GeneDataDecorator> getGenes() {
            return genes;
        }
    }

    public Object getResponse(Map query) {
        Object value = query.get("experimentAccession");
        if (value == null) {
            return new Error("Experiment accession is not specified");
        } else if (!(value instanceof String)) {
            return new Error("Experiment accession must be a string");
        }
        final String experimentAccession = (String) value;

        value = query.get("assayAccessions");
        if (value == null) {
            return new Error("Assay accessions list is not specified");
        } else if (!(value instanceof List)) {
            return new Error("Assay accessions must be a list");
        }
        for (Object aa : (List) value) {
            if (!(aa instanceof String)) {
                return new Error("All assay accessions must be a strings");
            }
        }
        final List<String> assayAccessions = (List<String>) value;

        value = query.get("geneNames");
        if (value == null) {
            return new Error("Gene set is not specified");
        } else if (!(value instanceof List) && !"*".equals(value)) {
            return new Error("Gene set must be a list or \"*\" pattern");
        }
        if (value instanceof List) {
            for (Object g : (List) value) {
                if (!(g instanceof String)) {
                    return new Error("All assay accessions must be a strings");
                }
            }
        }
        // TODO: if !(value instanceof List), we silently ignore an error
        final List<String> genes = (value instanceof List) ? (List<String>) value : null;

        try {
            final Map<Integer, AtlasGene> genesById;
            if (genes == null) {
                genesById = null;
            } else {
                genesById = new TreeMap<Integer, AtlasGene>();
                for (String geneName : genes) {
                    for (AtlasGene gene : geneSolrDAO.getGenesByName(geneName)) {
                        genesById.put(gene.getGeneId(), gene);
                    }
                }
            }
            final List<DataDecorator> data = new LinkedList<DataDecorator>();
            for (NetCDFDescriptor ncdf : atlasNetCDFDAO.getNetCDFProxiesForExperiment(experimentAccession)) {
                NetCDFProxy proxy = null;
                try {
                    proxy = ncdf.createProxy();
                    final Map<Integer, String> assayAccessionByIndex = new TreeMap<Integer, String>();
                    int index = 0;
                    for (String aa : proxy.getAssayAccessions()) {
                        if (assayAccessions.contains(aa)) {
                            assayAccessionByIndex.put(index, aa);
                        }
                        ++index;
                    }
                    final DataDecorator d = new DataDecorator();
                    data.add(d);
                    d.assayAccessions = new String[assayAccessionByIndex.size()];
                    index = 0;
                    for (String aa : assayAccessionByIndex.values()) {
                        d.assayAccessions[index++] = aa;
                    }
                    int deIndex = 0;
                    final long[] proxyGenes = proxy.getGenes();
                    final String[] proxyDEAccessions = proxy.getDesignElementAccessions();
                    for (int i = 0; i < proxyGenes.length; ++i) {
                        final int geneId = (int) proxyGenes[i];
                        if (genesById == null || genesById.keySet().contains(geneId)) {
                            // TODO: 4geometer: NPE here!
                            final AtlasGene gene = genesById.get(geneId);
                            final GeneDataDecorator geneInfo = new GeneDataDecorator(
                                    gene.getGeneName(),
                                    proxyDEAccessions[i],
                                    new float[assayAccessionByIndex.size()]
                            );
                            d.genes.add(geneInfo);
                            float[] levels = proxy.getExpressionDataForDesignElementAtIndex(deIndex);
                            index = 0;
                            for (int j : assayAccessionByIndex.keySet()) {
                                geneInfo.expressionLevels[index++] = levels[j];
                            }
                        }
                        ++deIndex;
                    }
                } finally {
                    Closeables.closeQuietly(proxy);
                }
            }
            return data;
        } catch (IOException e) {
            return new Error(e.toString());
        }
    }
}
