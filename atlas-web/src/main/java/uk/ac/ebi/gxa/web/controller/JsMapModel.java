/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.controller;

import org.springframework.ui.Model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An experimental code. It gathers JSON specific attributes into single map to easy convert in JSON in JSP.
 * E.g. Use single operation ${u:toJson(jsMap)} instead of multiple '${u:escapeJS('attr1')}', '${u:escapeJS('attr2')}' ...
 *
 * @author Olga Melnichuk
 *         Date: 04/05/2011
 */
public class JsMapModel implements Model {

    private final Model model;
    private final Map<String, Object> jsMap = new HashMap<String, Object>();

    private JsMapModel(Model model) {
        this.model = model;
        this.model.addAttribute("jsMap", jsMap);
    }

    public JsMapModel addJsAttribute(String attributeName, Object attributeValue) {
        jsMap.put(attributeName, attributeValue);
        return this;
    }

    @Override
    public JsMapModel addAttribute(String attributeName, Object attributeValue) {
        model.addAttribute(attributeName, attributeValue);
        return this;
    }

    @Override
    public JsMapModel addAttribute(Object attributeValue) {
        model.addAttribute(attributeValue);
        return this;
    }

    @Override
    public JsMapModel addAllAttributes(Collection<?> attributeValues) {
        model.addAllAttributes(attributeValues);
        return this;
    }

    @Override
    public JsMapModel addAllAttributes(Map<String, ?> attributes) {
        model.addAllAttributes(attributes);
        return this;
    }

    @Override
    public JsMapModel mergeAttributes(Map<String, ?> attributes) {
        model.mergeAttributes(attributes);
        return this;
    }

    @Override
    public boolean containsAttribute(String attributeName) {
        return model.containsAttribute(attributeName);
    }

    @Override
    public Map<String, Object> asMap() {
        return model.asMap();
    }

    public static JsMapModel wrap(Model model) {
        return new JsMapModel(model);
    }
}
