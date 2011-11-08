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

package ae3.model;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class represents the data structure used for atlas list views
 *
 * @author iemam
 */
public class ListResultRow implements Comparable<ListResultRow> {
    private final String fv;
    private final String ef;
    private final int count_up;
    private final int count_dn;
    private final int count_no;
    private final float minPval_up;
    private final float minPval_dn;
    private AtlasGene gene;
    public final String designElementAccession;

    Collection<ListResultRowExperiment> exp_list = new ArrayList<ListResultRowExperiment>();

    public ListResultRow(String ef, String efv, int count_up, int count_dn, int count_no, float min_up, float min_dn, String designElementAccession) {
        this.ef = ef;
        this.fv = efv;
        this.count_dn = count_dn;
        this.count_up = count_up;
        this.count_no = count_no;
        this.minPval_dn = min_dn;
        this.minPval_up = min_up;
        this.designElementAccession = designElementAccession;
    }

    @RestOut(name = "efv")
    public String getFv() {
        return fv;
    }

    public String getShortFv() {
        String fv_short = StringUtils.capitalize(fv);
        return fv_short.length() > 30 ? fv_short.substring(0, 30) + "..." : fv_short;
    }

    @RestOut(name = "ef")
    public String getEf() {
        return ef;
    }

    public int getUps() {
        return count_up;
    }

    public int getDowns() {
        return count_dn;
    }

    public int getNones() {
        return count_no;
    }

    public float getMinPval() {
        if (isMixedCell()) {
            return Math.min(Math.abs(minPval_dn), Math.abs(minPval_up));
        }
        return (count_dn > 0) ? minPval_dn : minPval_up;
    }

    public String getRow_id() {
        return StringEscapeUtils.escapeHtml(ef) + StringEscapeUtils.escapeHtml(fv);
    }

    public String getText() {
        if (isMixedCell())
            return " found over-expressed in " + fv + " in " + count_up + " experiments and under-expressed in " + count_dn + " experiments";
        else if (count_up > 0)
            return " found over-expressed in " + fv + " in " + count_up + " experiments";
        else if (count_dn > 0)
            return " found under-expressed in " + fv + " in " + count_dn + " experiments";
        else
            return "";
    }

    public boolean isMixedCell() {
        return (count_dn > 0 && count_up > 0);
    }

    public String getExpr() {
        return (count_dn > 0) ? "dn" :
                (count_up > 0) ? "up" : "";
    }


    public int getNoStudies() {
        return count_dn + count_up;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public void setGene(AtlasGene gene) {
        this.gene = gene;
    }

    public String getGene_name() {
        return gene.getGeneName();
    }

    public String getGene_species() {
        return gene.getGeneSpecies();
    }

    public String getGene_identifier() {
        return gene.getGeneIdentifier();
    }

    public long getGene_id() {
        return gene.getGeneId();
    }

    @RestOut(name = "experiments")
    public Collection<ListResultRowExperiment> getExp_list() {
        return exp_list;
    }

    public void setExp_list(Collection<ListResultRowExperiment> exp_list) {
        this.exp_list = exp_list;
    }

    public int compareTo(ListResultRow o) {
        if (this.getNoStudies() == o.getNoStudies()) {
            if (this.minPval_dn + this.minPval_up > o.minPval_dn + o.minPval_up)
                return -1;
            else if (this.minPval_dn + this.minPval_up < o.minPval_dn + o.minPval_up)
                return 1;
            else
                return 0;
        } else if (this.getNoStudies() > o.getNoStudies())
            return 1;
        else
            return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListResultRow that = (ListResultRow) o;

        if (count_dn != that.count_dn) return false;
        if (count_up != that.count_up) return false;
        if (Float.compare(that.minPval_dn, minPval_dn) != 0) return false;
        if (Float.compare(that.minPval_up, minPval_up) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count_up;
        result = 31 * result + count_dn;
        result = 31 * result + (minPval_up != +0.0f ? Float.floatToIntBits(minPval_up) : 0);
        result = 31 * result + (minPval_dn != +0.0f ? Float.floatToIntBits(minPval_dn) : 0);
        return result;
    }

    public String getDesignElementAccession() {
        return designElementAccession != null ? designElementAccession : "";
    }
}

