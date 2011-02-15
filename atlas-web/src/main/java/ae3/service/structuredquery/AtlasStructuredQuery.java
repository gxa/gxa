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

package ae3.service.structuredquery;

import ae3.util.HtmlHelper;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Atlas structured query container class for parsed parameters
 *
 * @author pashky
 */
public class AtlasStructuredQuery {

    private Collection<String> species = new ArrayList<String>();
    private Collection<ExpFactorQueryCondition> conditions = new ArrayList<ExpFactorQueryCondition>();
    private Collection<GeneQueryCondition> geneConditions = new ArrayList<GeneQueryCondition>();
    private int start = 0;
    private int rowsPerPage = 100;
    private int expsPerGene;
    private Set<String> expandColumns = new HashSet<String>();
    private ViewType viewType = ViewType.HEATMAP;
    private boolean fullHeatmap = false;

    public boolean isFullHeatmap() {
        return fullHeatmap;
    }

    public void setFullHeatmap(boolean fullHeatmap) {
        this.fullHeatmap = fullHeatmap;
    }

    /**
     * sets lists of gene queries represented by each row added to the query
     *
     * @param geneConditions conditions on genes
     */
    public void setGeneConditions(Collection<GeneQueryCondition> geneConditions) {
        this.geneConditions = geneConditions;
    }

    /**
     * Returns gene queries for the current query. Includes for each query (query, query operator and gene property)
     *
     * @return geneQueries
     */
    public Collection<GeneQueryCondition> getGeneConditions() {
        return geneConditions;
    }

    /**
     * Returns list of species
     *
     * @return list of species
     */
    public Collection<String> getSpecies() {
        return species;
    }

    /**
     * Sets list of species
     *
     * @param species list of species
     */
    public void setSpecies(Collection<String> species) {
        this.species = species;
    }

    /**
     * Returns Collection of all conditions
     *
     * @return Collection of all conditions
     * @see ExpFactorQueryCondition
     */
    public Collection<ExpFactorQueryCondition> getConditions() {
        return conditions;
    }

    /**
     * Sets list of EFV conditions
     *
     * @param conditions list of EFV conditions
     * @see QueryCondition
     */
    public void setConditions(Collection<ExpFactorQueryCondition> conditions) {
        this.conditions = conditions;
    }

    /**
     * Returns start position
     *
     * @return start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets required start position in paging
     *
     * @param start position in paging
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns required number of rows in page
     *
     * @return number of rows in page
     */
    public int getRowsPerPage() {
        return rowsPerPage;
    }

    /**
     * Sets required number of rows in page
     *
     * @param rowsPerPage number of rows in page
     */
    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    /**
     * Checks if query is "simple" query (just one condition)
     *
     * @return true or false
     */
    public boolean isSimple() {
        Iterator<ExpFactorQueryCondition> efi = conditions.iterator();
        if (efi.hasNext()) {
            ExpFactorQueryCondition efc = efi.next();
            if (efi.hasNext() || !"".equals(efc.getFactor()) || efc.getMinExperiments() > 1)
                return false;
        }

        Iterator<GeneQueryCondition> gqi = geneConditions.iterator();
        if (gqi.hasNext()) {
            GeneQueryCondition gqc = gqi.next();
            if (gqi.hasNext() || gqc.isNegated())
                return false;
        }

        Iterator<String> spi = species.iterator();
        return (!spi.hasNext() || (spi.next() != null && !spi.hasNext()));
    }

    /**
     * Checks if there's not enough parameters entered to query for something
     *
     * @return true or false
     */
    public boolean isNone() {
        return conditions.isEmpty() && geneConditions.isEmpty();
    }

    /**
     * Returns set of required expanded columns
     *
     * @return set of expanded columns
     */
    public Set<String> getExpandColumns() {
        return expandColumns;
    }

    /**
     * Sets required expanded columns
     *
     * @param expandColumns set of expanded columns
     */
    public void setExpandColumns(Set<String> expandColumns) {
        this.expandColumns = expandColumns;
    }

    /**
     * Sets requested view for the output results
     *
     * @param viewType the view to output
     */
    public void setViewType(ViewType viewType) {
        this.viewType = viewType;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        boolean hasValues = false;

        for (GeneQueryCondition c : geneConditions) {
            if (hasValues)
                sb.append(" and ");
            sb.append(c.getJointFactorValues()).append(" ");
            hasValues = true;
        }
        if (!hasValues)
            sb.append("(All genes) ");

        for (ExpFactorQueryCondition c : conditions) {
            sb.append(" and ");

            sb.append(c.getExpression().getDescription());
            if (c.getMinExperiments() > 1)
                sb.append("(min.").append(c.getMinExperiments()).append(" exps)");
            sb.append(" in ");
            if (c.isAnything())
                sb.append("(all conditions)");
            else
                sb.append(c.getJointFactorValues());
        }
        return sb.toString();
    }

    private String camelcase(String s) {
        return s.length() > 1 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : s.toUpperCase();
    }

    public String getApiUrl() {
        StringBuilder sb = new StringBuilder();

        for (GeneQueryCondition c : geneConditions)
            if (!c.isAnything()) {
                if (sb.length() > 0)
                    sb.append("&");
                sb.append("gene").append(HtmlHelper.escapeURL(camelcase(c.getFactor()))).append("Is");
                if (c.isNegated())
                    sb.append("Not");
                sb.append("=");
                sb.append(HtmlHelper.escapeURL(c.getJointFactorValues()));
            }

        for (String s : species) {
            if (sb.length() > 0)
                sb.append("&");
            sb.append("species=").append(HtmlHelper.escapeURL(s));
        }

        for (ExpFactorQueryCondition c : conditions)
            if (!c.isAnything()) {
                if (sb.length() > 0)
                    sb.append("&");

                sb.append(c.getExpression().toString().toLowerCase().replaceAll("[^a-z]", ""));
                if (c.getMinExperiments() > 1)
                    sb.append(c.getMinExperiments());
                sb.append("In").append(HtmlHelper.escapeURL(camelcase(c.getFactor())))
                        .append("=").append(HtmlHelper.escapeURL(c.getJointFactorValues()));
            }

        if (sb.length() > 0)
            sb.append("&");
        sb.append("rows=").append(rowsPerPage).append("&start=").append(start);
        return sb.toString();
    }

    /**
     * Retrieves number of experiments to retrieve for each gene
     *
     * @return number of experiments set for each gene
     */
    public int getExpsPerGene() {
        return expsPerGene;
    }

    /**
     * Sets number of experiments to retrieve for each gene
     *
     * @param expsPerGene maximal number of experiments to retrieve per gene
     */
    public void setExpsPerGene(int expsPerGene) {
        this.expsPerGene = expsPerGene;
    }

    /**
     * Checks whether user restricted the search with any condition
     *
     * @return true if search contains at least one condition; false otherwise
     */
    public boolean isRestricted() {
        return !Collections2.filter(conditions,
                new Predicate<ExpFactorQueryCondition>() {
                    public boolean apply(@Nullable ExpFactorQueryCondition input) {
                        return input != null && !input.isAnything();
                    }
                }).isEmpty();
    }
}
