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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BioMart v 0.7 service query.
 *
 * @author Olga Melnichuk
 */
public class MartQuery {
    private final List<String> attributes = new ArrayList<String>();
    private final String virtualSchema;
    private final String dataset;
    private boolean count;

    /**
     * Constructs mart query object by giving mart location params.
     *
     * @param virtualSchema a name of a virtualSchema (mart) to query data from
     * @param dataset       a name of a dataset within a virtualSchema (mart) to query data from
     */
    public MartQuery(@Nonnull String virtualSchema, @Nonnull String dataset) {
        this.virtualSchema = virtualSchema;
        this.dataset = dataset;
    }

    /**
     * Adds attributes to the query; attributes specify the data columns returned in the query response.
     *
     * @param attributes a collection of attributes
     * @return the original mart query object
     */
    public MartQuery addAttributes(Collection<String> attributes) {
        this.attributes.addAll(attributes);
        return this;
    }

    /**
     * Sets "count" flag; if it's "true" query response is a total number of results; otherwise all requested
     * results are returned.
     *
     * @param count a boolean flag to force query return just a total number of results
     * @return the original mart query object
     */
    public MartQuery setCount(boolean count) {
        this.count = count;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<!DOCTYPE Query>")
                .append("<Query virtualSchemaName = \"").append(virtualSchema).append("\"")
                .append(" formatter = \"TSV\"")
                .append(" header = \"1\"")
                .append(" uniqueRows = \"1\"")
                .append(" count = \"").append(count ? "1" : "0").append("\" >")
                .append("<Dataset name = \"").append(dataset).append("\" interface = \"default\" >");

        for (String attr : attributes) {
            sb.append("<Attribute name = \"").append(attr).append("\" />");
        }
        return sb.append("</Dataset></Query>").toString();
    }
}
