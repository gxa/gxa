package ae3.model;

import org.apache.commons.lang.WordUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Apr 17, 2008
 * Time: 9:32:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasTuple {
    private String ef;
    private String efv;
    private Integer updn;
    private Double pval;

    public AtlasTuple(String ef, String efv, Integer updn, Double pval) {
        this.ef = ef;
        this.efv = efv;
        this.updn = updn;
        this.pval = pval;
    }

    public String getEf() {
        return ef;
    }

    public void setEf(String ef) {
        this.ef = ef;
    }

    public String getEfv() {
        if(efv != null)
            return WordUtils.capitalize(efv, new char[] {0x01});

        return efv;
//        String efvalue ="";
//    	String[] array = efv.split(" ");
//    	for(int i=0; i<array.length; i++)
//        {
//            if(array[i].length() > 0)
//            {
//    		    efvalue+= array[i].substring(0,1).toUpperCase()+array[i].substring(1) +" ";
//    	    }
//        }
//        return efvalue;
    }

    public void setEfv(String efv) {
        this.efv = efv;
    }

    public Integer getUpdn() {
        return updn;
    }

    public void setUpdn(Integer updn) {
        this.updn = updn;
    }

    public Double getPval() {
        return pval;
    }

    public void setPval(Double pval) {
        this.pval = pval;
    }
    
    public String getTxtSummary(){
    	String text="";
    	if(updn.intValue() == 1)
    		text= "is over-expressed in "+efv+" vs other "+ef+"s in this study";
    	else if(updn.intValue() == -1)
    		text= "is under-expressed in "+efv+" vs other "+ef+"s in this study";
    	return text;
    }
}
