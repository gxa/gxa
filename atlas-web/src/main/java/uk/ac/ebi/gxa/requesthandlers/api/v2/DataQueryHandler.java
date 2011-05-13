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

class DataQueryHandler implements QueryHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final GeneSolrDAO geneSolrDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;

    DataQueryHandler(GeneSolrDAO geneSolrDAO, AtlasNetCDFDAO atlasNetCDFDAO) {
        this.geneSolrDAO = geneSolrDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    private static abstract class GeneDataDecorator {
        final String designElementAccession;
        final float[] expressionLevels;

        GeneDataDecorator(String designElementAccession, float[] expressionLevels) {
            this.designElementAccession = designElementAccession;
            this.expressionLevels = expressionLevels;
        }

        public String getDesignElementAccession() {
            return designElementAccession;
        }

        public float[] getExpressionLevels() {
            return expressionLevels;
        }
    }

    private static class GeneDataDecoratorWithName extends GeneDataDecorator {
        final String geneName;

        GeneDataDecoratorWithName(String geneName, String designElementAccession, float[] expressionLevels) {
            super(designElementAccession, expressionLevels);
            this.geneName = geneName;
        }

        public String getGeneName() {
            return geneName;
        }
    }

    private static class GeneDataDecoratorWithIdentifier extends GeneDataDecorator {
        final String geneIdentifier;

        GeneDataDecoratorWithIdentifier(String geneIdentifier, String designElementAccession, float[] expressionLevels) {
            super(designElementAccession, expressionLevels);
            this.geneIdentifier = geneIdentifier;
        }

        public String getGeneIdentifier() {
            return geneIdentifier;
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
        final String experimentAccession = (String)value;

        value = query.get("assayAccessions");
        if (value == null) {
            return new Error("Assay accessions list is not specified");
        } else if (!(value instanceof List)) {
            return new Error("Assay accessions must be a list");
        }
        for (Object aa : (List)value) {
            if (!(aa instanceof String)) {
                return new Error("All assay accessions must be a strings");
            }
        }
        final List<String> assayAccessions = (List<String>)value;

        final Object geneNames = query.get("geneNames");
        final Object geneIdentifiers = query.get("geneIdentifiers");
        if (geneNames == null && geneIdentifiers == null) {
            return new Error("Gene set is not specified; you must specify geneNames or geneIdentifiers field");
        } else if (geneNames != null && geneIdentifiers != null) {
            return new Error("You cannot specify geneNames and geneIdentifiers in the same request");
        }
        final boolean useGeneNames = geneNames != null;
        value = useGeneNames ? geneNames : geneIdentifiers;
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
        final List<String> genes = (value instanceof List) ? (List<String>)value : null;

        try {
            final List<String> proxyIds = new LinkedList<String>();
            for (NetCDFDescriptor descriptor :
                atlasNetCDFDAO.getNetCDFProxiesForExperiment(experimentAccession)) {
                proxyIds.add(descriptor.getProxyId());
            }
            final Map<Long,AtlasGene> genesById;
            if (genes != null) {
                genesById = new TreeMap<Long,AtlasGene>();
                if (useGeneNames) {
                    for (String geneName : genes) {
                        for (AtlasGene gene : geneSolrDAO.getGenesByName(geneName)) {
                            genesById.put(gene.getGeneId(), gene);
                        }
                    }
                } else /* use gene identifiers */ {
                    for (AtlasGene gene : geneSolrDAO.getGenesByIdentifiers(genes)) {
                        genesById.put(gene.getGeneId(), gene);
                    }
                }
            } else {
                genesById = null;
            }
            final List<DataDecorator> data = new LinkedList<DataDecorator>();
            for (String pId : proxyIds) {
                final NetCDFProxy proxy = atlasNetCDFDAO.getNetCDFProxy(experimentAccession, pId);
                final Map<Integer,String> assayAccessionByIndex = new TreeMap<Integer,String>();
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
                final long[] proxyGenes = proxy.getGenes();
                final String[] proxyDEAccessions = proxy.getDesignElementAccessions();
                if (genesById == null) {
                    final float[][] array = proxy.getAllExpressionData();
                    final TreeMap<Long,AtlasGene> allGenesById = new TreeMap<Long,AtlasGene>();
                    for (AtlasGene g : geneSolrDAO.getAllGenes()) {
                        allGenesById.put(g.getGeneId(), g);
                    }
                    for (int i = 0; i < proxyGenes.length; ++i) {
                        final AtlasGene gene = allGenesById.get(proxyGenes[i]);
                        final GeneDataDecorator geneInfo;
                        if (useGeneNames) {
                            final String geneName =
                                gene != null ? gene.getGeneName() : "unknown gene";
                            geneInfo = new GeneDataDecoratorWithName(
                                geneName,
                                proxyDEAccessions[i],
                                new float[assayAccessionByIndex.size()]
                            );
                        } else {
                            final String geneIdentifier =
                                gene != null ? gene.getGeneIdentifier() : "unknown gene";
                            geneInfo = new GeneDataDecoratorWithIdentifier(
                                geneIdentifier,
                                proxyDEAccessions[i],
                                new float[assayAccessionByIndex.size()]
                            );
                        }
                        d.genes.add(geneInfo);
                        index = 0;
                        for (int j : assayAccessionByIndex.keySet()) {
                            geneInfo.expressionLevels[index++] = array[i][j];
                        }
                    }
                } else {
                    for (int i = 0; i < proxyGenes.length; ++i) {
                        final AtlasGene gene = genesById.get(proxyGenes[i]);
                        if (gene == null) {
                            continue;
                        }
                        final float[] levels = proxy.getExpressionDataForDesignElementAtIndex(i);
                        final GeneDataDecorator geneInfo;
                        if (useGeneNames) {
                            geneInfo = new GeneDataDecoratorWithName(
                                gene.getGeneName(),
                                proxyDEAccessions[i],
                                new float[assayAccessionByIndex.size()]
                            );
                        } else {
                            geneInfo = new GeneDataDecoratorWithIdentifier(
                                gene.getGeneIdentifier(),
                                proxyDEAccessions[i],
                                new float[assayAccessionByIndex.size()]
                            );
                        }
                        d.genes.add(geneInfo);
                        index = 0;
                        for (int j : assayAccessionByIndex.keySet()) {
                            geneInfo.expressionLevels[index++] = levels[j];
                        }
                    }
                }
            }
            return data;
        } catch (IOException e) {
            return new Error(e.toString());
        }
    }
}
