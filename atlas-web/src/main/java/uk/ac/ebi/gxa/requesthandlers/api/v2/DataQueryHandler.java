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

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.util.*;
import java.io.IOException;

class DataQueryHandler implements QueryHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final AtlasSolrDAO atlasSolrDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;

    DataQueryHandler(AtlasSolrDAO atlasSolrDAO, AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
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
        for (final Object aa : (List)value) {
            if (!(aa instanceof String)) {
                return new Error("All assay accessions must be a strings");
            }
        }
        final List<String> assayAccessions = (List<String>)value;

        value = query.get("genes");
        if (value == null) {
            return new Error("Gene set is not specified");
        } else if (!(value instanceof List) && !"*".equals(value)) {
            return new Error("Gene set must be a list or \"*\" pattern");
        }
        if (value instanceof List) {
            for (final Object g : (List)value) {
                if (!(g instanceof String)) {
                    return new Error("All assay accessions must be a strings");
                }
            }
        }
        final List<String> genes = (value instanceof List) ? (List<String>)value : null;

        try {
            final List<String> proxyIds = new LinkedList<String>();
            for (final NetCDFDescriptor descriptor :
                atlasNetCDFDAO.getNetCDFProxiesForExperiment(experimentAccession)) {
                proxyIds.add(descriptor.getProxyId());
            }
            final Map<Long,String> geneNamesByIds;
            if (genes != null) {
                geneNamesByIds = new TreeMap<Long,String>();
                for (final String geneName : genes) {
                    for (final AtlasGene gene : atlasSolrDAO.getGenesByName(geneName)) {
                        geneNamesByIds.put(gene.getGeneId(), geneName);
                    }
                }
            } else {
                geneNamesByIds = null;
            }
            for (final String pId : proxyIds) {
                final NetCDFProxy proxy = atlasNetCDFDAO.getNetCDFProxy(experimentAccession, pId);
                final Map<Integer,String> assayAccessionByIndex = new TreeMap<Integer,String>();
                int index = 0;
                for (final String aa : proxy.getAssayAccessions()) {
                    if (assayAccessions.contains(aa)) {
                        assayAccessionByIndex.put(index, aa);
                    }
                    ++index;
                }
                int deIndex = 0;
                for (Long geneId : proxy.getGenes()) {
                    if (geneNamesByIds == null || geneNamesByIds.keySet().contains(geneId)) {
                        log.info("gene: " + geneNamesByIds.get(geneId));
                        float[] data = proxy.getExpressionDataForDesignElementAtIndex(deIndex);
                        for (Integer i : assayAccessionByIndex.keySet()) {
                            log.info("assay " + assayAccessionByIndex.get(i) + ": " + data[i]);
                        }
                    }
                    ++deIndex;
                }
            }
        } catch (IOException e) {
            return new Error(e.toString());
        }

        return new Error("unsupported request");
    }
}
