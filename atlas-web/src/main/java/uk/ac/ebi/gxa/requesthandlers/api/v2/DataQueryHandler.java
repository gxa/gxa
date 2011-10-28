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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.TwoDFloatArray;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.util.*;

class DataQueryHandler implements QueryHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final BioEntityDAO bioEntityDAO;
    private final GeneSolrDAO geneSolrDAO;
    private final AtlasDataDAO atlasDataDAO;
    private final AtlasDAO atlasDAO;

    DataQueryHandler(BioEntityDAO bioEntityDAO, GeneSolrDAO geneSolrDAO, AtlasDataDAO atlasDataDAO, AtlasDAO atlasDAO) {
        this.bioEntityDAO = bioEntityDAO;
        this.geneSolrDAO = geneSolrDAO;
        this.atlasDataDAO = atlasDataDAO;
        this.atlasDAO = atlasDAO;
    }

    private static interface DataProvider {
        float[] getRow();
    }

    private static class SimpleDataProvider implements DataProvider {
        private final float[] row;

        SimpleDataProvider(float[] row) {
            this.row = row;
        }

        public float[] getRow() {
            return row;
        }
    }

    private static class TwoDDataProvider implements DataProvider {
        private final TwoDFloatArray array;
        private final int rowIndex;

        TwoDDataProvider(TwoDFloatArray array, int rowIndex) {
            this.array = array;
            this.rowIndex = rowIndex;
        }

        public float[] getRow() {
            return array.getRow(rowIndex);
        }
    }

    private static abstract class GeneDataDecorator {
        final String designElementAccession;
        final DataProvider provider;
        final Set<Integer> colIndices;

        GeneDataDecorator(String designElementAccession, DataProvider provider, Set<Integer> colIndices) {
            this.designElementAccession = designElementAccession;
            this.provider = provider;   
            this.colIndices = colIndices;
        }

        public String getDesignElementAccession() {
            return designElementAccession;
        }

        public float[] getExpressionLevels() {
            final float[] row = provider.getRow();
            if (row.length == colIndices.size()) {
                return row;
            }
            final float[] levels = new float[colIndices.size()];
            int index = 0;
            for (Integer i : colIndices) {
                levels[index++] = row[i];
            }
            return levels;
        }
    }

    private static class GeneDataDecoratorWithName extends GeneDataDecorator {
        final String geneName;

        GeneDataDecoratorWithName(String geneName, String designElementAccession, DataProvider provider, Set<Integer> colIndices) {
            super(designElementAccession, provider, colIndices);
            this.geneName = geneName;
        }

        public String getGeneName() {
            return geneName;
        }
    }

    private static class GeneDataDecoratorWithIdentifier extends GeneDataDecorator {
        final String geneIdentifier;

        GeneDataDecoratorWithIdentifier(String geneIdentifier, String designElementAccession, DataProvider provider, Set<Integer> colIndices) {
            super(designElementAccession, provider, colIndices);
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
            for (Object g : (List) value) {
                if (!(g instanceof String)) {
                    return new Error("All gene names (identifiers) must be a strings");
                }
            }
        }
        final List<String> genes = (value instanceof List) ? (List<String>) value : null;

        try {
            final Map<Long,AtlasGene> genesById;
            if (genes != null) {
                genesById = new TreeMap<Long,AtlasGene>();
                if (useGeneNames) {
                    for (String geneName : genes) {
                        for (AtlasGene gene : geneSolrDAO.getGenesByName(geneName)) {
                            genesById.put((long)gene.getGeneId(), gene);
                        }
                    }
                } else /* use gene identifiers */ {
                    for (AtlasGene gene : geneSolrDAO.getGenesByIdentifiers(genes)) {
                        genesById.put((long)gene.getGeneId(), gene);
                    }
                }
            } else {
                genesById = null;
            }
            final List<DataDecorator> data = new LinkedList<DataDecorator>();
            final Experiment experiment = atlasDAO.getExperimentByAccession(experimentAccession);
            final ExperimentWithData experimentWithData =
                atlasDataDAO.createExperimentWithData(experiment);
            for (ArrayDesign ad : experiment.getArrayDesigns()) {
                final Map<Integer, String> assayAccessionByIndex = new TreeMap<Integer, String>();
                int index = 0;
                for (Assay assay : experimentWithData.getAssays(ad)) {
                    if (assayAccessions.contains(assay.getAccession())) {
                        assayAccessionByIndex.put(index, assay.getAccession());
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
                final long[] proxyGenes = experimentWithData.getGenes(ad);
                final String[] proxyDEAccessions = experimentWithData.getDesignElementAccessions(ad);
                if (genesById == null) {
                    final TwoDFloatArray array = experimentWithData.getAllExpressionData(ad);
                    final TreeMap<Long,String> allGenesById = new TreeMap<Long,String>();
                    for (BioEntity g : bioEntityDAO.getAllGenesFast()) {
                        allGenesById.put(g.getId(), useGeneNames ? g.getName() : g.getIdentifier());
                    }
                    for (int i = 0; i < proxyGenes.length; ++i) {
                        String geneString = allGenesById.get(proxyGenes[i]);
                        if (geneString == null) {
                            geneString = "unknown gene";
                        }
                        final GeneDataDecorator geneInfo;
                        if (useGeneNames) {
                            d.genes.add(new GeneDataDecoratorWithName(
                                geneString,
                                proxyDEAccessions[i],
                                new TwoDDataProvider(array, i),
                                assayAccessionByIndex.keySet()
                            ));
                        } else {
                            d.genes.add(new GeneDataDecoratorWithIdentifier(
                                geneString,
                                proxyDEAccessions[i],
                                new TwoDDataProvider(array, i),
                                assayAccessionByIndex.keySet()
                            ));
                        }
                    }
                } else {
                    for (int i = 0; i < proxyGenes.length; ++i) {
                        final AtlasGene gene = genesById.get(proxyGenes[i]);
                        if (gene == null) {
                            continue;
                        }
                        final float[] levels = experimentWithData.getExpressionDataForDesignElementAtIndex(ad, i);
                        final GeneDataDecorator geneInfo;
                        if (useGeneNames) {
                            geneInfo = new GeneDataDecoratorWithName(
                                    gene.getGeneName(),
                                    proxyDEAccessions[i],
                                    new SimpleDataProvider(levels),
                                    assayAccessionByIndex.keySet()
                            );
                        } else {
                            geneInfo = new GeneDataDecoratorWithIdentifier(
                                    gene.getGeneIdentifier(),
                                    proxyDEAccessions[i],
                                    new SimpleDataProvider(levels),
                                    assayAccessionByIndex.keySet()
                            );
                        }
                        d.genes.add(geneInfo);
                    }
                }
            }
            return data;
        } catch (RecordNotFoundException e) {
            return new Error(e.getMessage());
        } catch (AtlasDataException e) {
            return new Error(e.toString());
        }
    }
}
