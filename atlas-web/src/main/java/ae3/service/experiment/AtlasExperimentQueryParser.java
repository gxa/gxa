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

package ae3.service.experiment;

import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.Map;

import static uk.ac.ebi.gxa.utils.EscapeUtil.optionalParseList;
import static uk.ac.ebi.gxa.utils.EscapeUtil.parseNumber;


/**
 * API experiment search query parser class. Has just one static method
 *
 * @author pashky
 */
public class AtlasExperimentQueryParser {

    private final AtlasProperties atlasProperties;

    private final Iterable<String> factors;

    public AtlasExperimentQueryParser(AtlasProperties atlasProperties, Iterable<String> factors) {
        this.atlasProperties = atlasProperties;
        this.factors = factors;
    }

    /**
     * Creates an instance of {@link AtlasExperimentQuery} from a parameter map.
     *
     * @param parameters parameter map to parse
     * @return AtlasExperimentQuery object, can be empty (check with isEmpty() method) but never null
     */
    public AtlasExperimentQuery parse(Map<String, String[]> parameters) {
        AtlasExperimentQuery.Builder qb = new AtlasExperimentQuery.Builder();
        qb.setRows(atlasProperties.getQueryDefaultPageSize());

        for (Map.Entry<String, String[]> e : parameters.entrySet()) {
            final String name = e.getKey();
            for (String v : e.getValue()) {
                if (name.matches("^experiment(Text|Id|Accession)?$")) {
                    if (v.equalsIgnoreCase("listAll"))
                        qb.setListAll();
                    else
                        qb.withExperimentKeywords(optionalParseList(v));
                } else if (name.matches("^experimentHasFactor$")) {
                    qb.withFactors(optionalParseList(v));
                } else if (name.matches("^experimentHas.*$")) {
                    String factName = name.substring("experimentHas".length()).toLowerCase();
                    if (factName.startsWith("any")) {
                        qb.withAnyFactorValues(optionalParseList(v));
                    } else if (factName.length() > 0) {
                        for (String p : factors) {
                            if (p.equalsIgnoreCase(factName)) {
                                qb.withFactorValues(p, optionalParseList(v));
                            }
                        }
                    }
                } else if (name.equalsIgnoreCase("geneIs")) {
                    qb.withGeneIdentifiers(optionalParseList(v));
                } else if (name.equalsIgnoreCase("rows")) {
                    qb.setRows(parseNumber(v, atlasProperties.getQueryDefaultPageSize(), 1, atlasProperties.getAPIQueryMaximumPageSize()));
                } else if (name.equalsIgnoreCase("start")) {
                    qb.setStart(parseNumber(v, 0, 0, Integer.MAX_VALUE));
                }
            }
        }
        return qb.toQuery();
    }
}
