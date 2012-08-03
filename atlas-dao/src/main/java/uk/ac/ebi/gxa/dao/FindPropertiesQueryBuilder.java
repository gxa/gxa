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

package uk.ac.ebi.gxa.dao;

public class FindPropertiesQueryBuilder {

    static final String PROPERTY_NAME_SELECTOR = "p.propertyValue.property.name";
    static final String PROPERTY_VALUE_SELECTOR = "p.propertyValue.value";

    private String parentEntityName;

    private boolean exactMatch = true;

    private boolean caseInsensitive = true;


    public FindPropertiesQueryBuilder(){

    }


    public FindPropertiesQueryBuilder setParentEntityName(String parentEntityName) {
        this.parentEntityName = parentEntityName;
        return this;
    }


    public String getQueryThatSelectsPropertiesByValue(){
        return "select p from " + parentEntityName + " t left join t.properties p where "
                        + getMatcherCondition(PROPERTY_VALUE_SELECTOR, exactMatch, caseInsensitive);

    }


    public String getQueryThatSelectsPropertiesByNameAndValue(){

        return getQueryThatSelectsPropertiesByValue()
                        + " and " + getMatcherCondition(PROPERTY_NAME_SELECTOR, true , caseInsensitive);

    }



    String getMatcherCondition(String selectorExpression, boolean exactMatch, boolean caseInsensitive) {

        if (caseInsensitive) {

            selectorExpression = "upper(" + selectorExpression + ")" ;

        }

        if (exactMatch) {

            return selectorExpression +  " = ?";

        }

        return selectorExpression + " like ?";
    }


    public String addHqlLikeSymbols(String value) {

        if (value.contains("*")){

            return org.apache.commons.lang.StringUtils.replace(value, "*", "%");

        }
        return "%" + value + "%";

    }


    public boolean isExactMatch() {

        return exactMatch;

    }

    public FindPropertiesQueryBuilder setExactMatch(boolean exactMatch) {

        this.exactMatch = exactMatch;
        return this;

    }

    public boolean isCaseInsensitive() {

        return caseInsensitive;

    }

    public FindPropertiesQueryBuilder setCaseInsensitive(boolean caseInsensitive) {

        this.caseInsensitive = caseInsensitive;
        return this;

    }

    public String getParentEntityName(){
        return parentEntityName;
    }

}
