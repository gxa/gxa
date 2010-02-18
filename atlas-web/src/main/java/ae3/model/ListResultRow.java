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
	private double minPval_up;
	private double minPval_dn;
	private AtlasGene gene;

    Collection<ListResultRowExperiment> exp_list;

	
	public ListResultRow(String ef, String efv, int count_up, int count_dn, double min_up, double min_dn){
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
	public double getPvalMin_up() {
		return minPval_up;
	}
	public double getPvalMin_dn() {
		return minPval_dn;
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

    @RestOut(name="experiments")
    public Collection<ListResultRowExperiment> getExp_list() {
		return exp_list;
	}

	public void setExp_list(Collection<ListResultRowExperiment> exp_list) {
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

