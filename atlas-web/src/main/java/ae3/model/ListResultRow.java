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

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;

/**
 * This class represents the data structure used for atlas list views
 * @author iemam
 *
 */
public class ListResultRow implements Comparable<ListResultRow> {
	private String fv;
	private String ef;
	private int count_up;
	private int count_dn;
	private float minPval_up;
	private float minPval_dn;
	private AtlasGene gene;

    Collection<ListResultRowExperiment> exp_list;

	
	public ListResultRow(String ef, String efv, int count_up, int count_dn, float min_up, float min_dn){
		this.ef = ef;
		this.fv = efv;
		this.count_dn = count_dn;
		this.count_up = count_up;
		this.minPval_dn = min_dn;
		this.minPval_up = min_up;
	}

    @RestOut(name="efv")
	public String getFv() {
		return fv;
	}
	public String getShortFv(){
		String fv_short = StringUtils.capitalize(fv);
		return fv_short.length() > 30 ? fv_short.substring(0,30)+"..." : fv_short;
	}

    @RestOut(name="ef")
	public String getEf() {
		return ef;
	}
	public int getCount_up() {
		return count_up;
	}
	public int getCount_dn() {
		return count_dn;
	}
	public float getPvalMin_up() {
		return minPval_up;
	}
	public float getPvalMin_dn() {
		return minPval_dn;
	}
	public float getMinPval(){
		if(isMixedCell())
		return Math.min(Math.abs(minPval_dn), Math.abs(minPval_up));
		else
			if(count_dn>0)
				return minPval_dn;
			else
				return minPval_up;
	}
	
	public String getRow_id(){
		return StringEscapeUtils.escapeHtml(ef)+StringEscapeUtils.escapeHtml(fv);
	}
	
	public String getText(){
		if(isMixedCell())
			return " found over-expressed in "+fv+ " in "+ count_up + " experiments and under-expressed in "+ count_dn+ " experiments";
		else if(count_up > 0)
			return " found over-expressed in "+fv+ " in " + count_up + " experiments";
		else
			if(count_dn > 0)
				return " found under-expressed in "+fv+ " in " + count_dn + " experiments";
			else
				return "";
	}
	
	public boolean isMixedCell(){
		return (count_dn>0 && count_up>0);
	}
	
	public String getExpr(){
		if (count_dn>0) return "dn";
		else if (count_up>0) return "up";
		else return "";
	}
	
		
	public int getNoStudies(){
		return count_dn+count_up;
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
	public String getGene_id() {
		return gene.getGeneId();
	}

    @RestOut(name="experiments")
    public Collection<ListResultRowExperiment> getExp_list() {
		return exp_list;
	}

	public void setExp_list(Collection<ListResultRowExperiment> exp_list) {
		this.exp_list = exp_list;
	}

	public int compareTo(ListResultRow o) {
		if (this.getNoStudies() == o.getNoStudies()){
            if(this.minPval_dn+this.minPval_up > o.minPval_dn+o.minPval_up)
            	return -1;
            else if(this.minPval_dn+this.minPval_up < o.minPval_dn+o.minPval_up)
            	return 1;
            else
            	return 0;
		}
        else if (this.getNoStudies() > o.getNoStudies())
            return 1;
        else
            return -1;
	}
}

