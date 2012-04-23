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

package uk.ac.ebi.gxa.requesthandlers.helper;

import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.AutoCompleteResult;
import ae3.service.structuredquery.AutoCompleter;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RequestWrapper;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author pashky
 */
public class FactorValuesRequestHandler extends AbstractRestRequestHandler {

    @RestOut(xmlItemName = "completion")
    public static class ACList extends ArrayList<AutoCompleteItem> {
    }

    @RestOut(xmlItemName = "query", xmlAttr = "id")
    public static class ACMap extends HashMap<String, List<AutoCompleteItem>> {
    }

    private Map<String, List<AutoCompleter>> autoCompleters;

    public void setAutoCompleters(Map<String, List<AutoCompleter>> autoCompleters) {
        this.autoCompleters = autoCompleters;
    }

    public Object process(HttpServletRequest request0) {
        RequestWrapper request = new RequestWrapper(request0);

        String type = request.getStr("type");
        List<AutoCompleter> listers = autoCompleters.get(type);


        Map<String, Object> result = new HashMap<String, Object>();

        String factor = request.getStr("factor");
        result.put("factor", factor);

        int nlimit = request.getInt("limit", 100, 1, 1000);
        String[] queries = request.getStrArray("q");

        Map<String, List<AutoCompleteItem>> values = new ACMap();
        result.put("completions", values);

        for (String query : queries) {
            String q = query != null ? query : "";
            if (q.startsWith("\"")) {
                q = q.substring(1);
            }
            if (q.endsWith("\"")) {
                q = q.substring(0, q.length() - 1);
            }

            Map<String, String> filters = new HashMap<String, String>();
            for (String filter : request.getStrArray("f")) {
                filters.put(filter, request.getStr(filter));
            }

            AutoCompleteResult autoCompleteResult = new AutoCompleteResult();
            if (listers != null) {
                for (AutoCompleter lister : listers) {
                    for (AutoCompleteItem item : lister.autoCompleteValues(factor, q, nlimit, filters))
                        autoCompleteResult.add(item);
                }
            }
            List<AutoCompleteItem> res = autoCompleteResult.getResults(type);
            Collections.sort(res);

            List<AutoCompleteItem> resultList = new ACList();
            resultList.addAll(res.subList(0, Math.min(nlimit, res.size())));
            values.put(q != null ? q : "", resultList);
        }

        return result;
    }
}
