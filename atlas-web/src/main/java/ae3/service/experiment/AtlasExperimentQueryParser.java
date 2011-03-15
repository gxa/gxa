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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static uk.ac.ebi.gxa.utils.EscapeUtil.parseNumber;


/**
 * API experiment search query parser class. Has just one static method
 * @author pashky
 */
public class AtlasExperimentQueryParser {
    /**
     * Parse HTTP request into AtlasExperimentQuery class
     * @param request HTTP Servlet request to parse
     * @param factors a list of all factors
     * @return AtlasExperimentQuery object, can be empty (check with isEmpty() method) but never null
     */
    public static AtlasExperimentQuery parse(HttpServletRequest request, Iterable<String> factors) {
        AtlasExperimentQuery query = new AtlasExperimentQuery();

        for(Map.Entry<String,String[]> e : request.getParameterMap().entrySet()) {
            final String name = e.getKey();
            for(String v : e.getValue()) {
                if(name.matches("^experiment(Text|Id|Accession)?$")) {
                    if(v.equalsIgnoreCase("listAll"))
                        query.listAll();
                    else
                        query.andText(v);
                } else if(name.matches("^experimentHasFactor$")) {
                    query.andHasFactor(v);
                } else if(name.matches("^experimentHas.*$")) {
                    String factName = name.substring("experimentHas".length()).toLowerCase();
                    if(factName.startsWith("any"))
                        factName = "";
                    else if(factName.length() > 0)
                        for(String p : factors)
                            if(p.equalsIgnoreCase(factName))
                                factName = p;

                    query.andHasFactorValue(factName, v);
                } else if(name.equalsIgnoreCase("rows")) {
                    query.rows(parseNumber(v, 10, 1, 200));
                } else if(name.equalsIgnoreCase("start")) {
                    query.start(parseNumber(v, 0, 0, Integer.MAX_VALUE));
                } else if(name.equalsIgnoreCase("dateReleaseFrom")){
                    query.addDateReleaseFrom(v);
                } else if(name.equalsIgnoreCase("dateReleaseTo")){
                    query.addDateReleaseTo(v);
                } else if(name.equalsIgnoreCase("dateLoadFrom")){
                    query.addDateLoadFrom(v);
                } else if(name.equalsIgnoreCase("dateLoadTo")){
                    query.addDateLoadTo(v);
                }
            }
        }

        return query;
    }
}
