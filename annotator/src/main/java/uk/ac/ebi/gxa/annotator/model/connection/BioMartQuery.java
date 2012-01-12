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

package uk.ac.ebi.gxa.annotator.model.connection;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * http://gmod.org/wiki/BioMart_Tutorial#6._Querying_a_BioMart_server_via_REST_API
 *
 * @author Olga Melnichuk
 */
public class BioMartQuery {
    private final List<String> attributes = new ArrayList<String>();
    private final String virtualSchemaName;
    private final String dataSetName;

    public BioMartQuery(@Nonnull String virtualSchemaName, @Nonnull String dataSetName) {
        this.virtualSchemaName = virtualSchemaName;
        this.dataSetName = dataSetName;
    }

    public BioMartQuery addAttributes(Collection<String> attrs) {
        attributes.addAll(attrs);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<!DOCTYPE Query>")
                .append("<Query  virtualSchemaName = \"").append(virtualSchemaName).append("\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" >")
                .append("<Dataset name = \"").append(dataSetName).append("\" interface = \"default\" >");

        for (String attr : attributes) {
            sb.append("<Attribute name = \"").append(attr).append("\" />");
        }
        return sb.append("</Dataset></Query>").toString();
    }
}
