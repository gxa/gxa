package ae3.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ae3.model.AtlasGene;
import ae3.service.structuredquery.ExperimentList;
import ae3.service.structuredquery.UpdownCounter;

/**
 * This class represents the data structure used for atlas list views
 * @author iemam
 *
 */
public class ListResultRow implements Comparable<ListResultRow>{
	private String fv;
	private String ef;
	private String row_id;
	private int count_up;
	private int count_dn;
	private double minPval_up;
	private double minPval_dn;
	private String gene_name;
	private String gene_species;
	private ExperimentList exp_list;
	private String gene_id;
	private AtlasGene gene;
	
	
	
	public ListResultRow (String efv, String ef, int count_up, int count_dn, double min_up, double min_dn){
		this.ef = ef;
		this.fv = efv;
		this.count_dn = count_dn;
		this.count_up = count_up;
		this.minPval_dn = min_dn;
		this.minPval_up = min_up;
	}
	public String getFv() {
		return fv;
	}
	public void setFv(String fv) {
		this.fv = fv;
	}
	public String getShortFv(){
		String fv_short = StringUtils.capitalize(fv);
		return fv_short.length() > 30 ? fv_short.substring(0,30)+"..." : fv_short;
	}
	public String getEf() {
		return ef;
	}
	public void setEf(String ef) {
		this.ef = ef;
	}
	public int getCount_up() {
		return count_up;
	}
	public void setCount_up(int count_up) {
		this.count_up = count_up;
	}
	public int getCount_dn() {
		return count_dn;
	}
	public void setCount_dn(int count_dn) {
		this.count_dn = count_dn;
	}
	public double getPvalMin_up() {
		return minPval_up;
	}
	public void setPvalMin_up(double avg_up) {
		this.minPval_up = avg_up;
	}
	public double getPvalMin_dn() {
		return minPval_dn;
	}
	public void setPvalMin_dn(double avg_dn) {
		this.minPval_dn = avg_dn;
	}
	public double getMinPval(){
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
	
	public HashMap<String, String> getCellColor() {
		
		String color="#ffffff";
		HashMap<String, String> colorMap = new HashMap<String, String>();
		if(count_up>0){
			int uc = coltrim((getPvalMin_up() > 0.05 ? 0.05 : getPvalMin_up()) * 255 / 0.05);            
			color =  String.format("#ff%02x%02x", uc, uc);
			colorMap.put("up",color);
		}
		
		if(count_dn>0){
			int dc = coltrim((getPvalMin_dn() > 0.05 ? 0.05 : getPvalMin_dn()) * 255 / 0.05);
            color =  String.format("#%02x%02xff", dc, dc);
            colorMap.put("dn",color);
		}
		
        return colorMap;
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
		return gene_name;
	}
	public void setGene_name(String gene_name) {
		this.gene_name = gene_name;
	}
	public String getGene_species() {
		return gene_species;
	}
	public void setGene_species(String gene_species) {
		this.gene_species = gene_species;
	}
	public String getGene_id() {
		return gene_id;
	}
	public void setGene_id(String gene_id) {
		this.gene_id = gene_id;
	}
		
    public HashMap<String, String> getCellText()
    {
        double c;
        HashMap<String, String> colorMap = new HashMap<String, String>();
        if(count_up>0) {
            c = (getPvalMin_up() > 0.05 ? 0.05 : getPvalMin_up()) * 255 / 0.05;
            colorMap.put("up",c > 127 ? "#000000" : "#ffffff");
        } 
        if(count_dn>0){
            c = (getPvalMin_dn() > 0.05 ? 0.05 : getPvalMin_dn()) * 255 / 0.05;
            colorMap.put("dn",c > 127 ? "#000000" : "#ffffff");
        }
        return colorMap;
    }
    
    public ExperimentList getExp_list() {
		return exp_list;
	}
    public Iterator getExplistIter() {
		return exp_list.iterator();
	}
	public void setExp_list(ExperimentList exp_list) {
		this.exp_list = exp_list;
	}
	private int coltrim(double v)
    {
        return Math.min(255, Math.max(0, (int)v));
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

