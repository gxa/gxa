package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Jan 13, 2010
 * Time: 11:07:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class Pvalue {
    private int designElementId;
    private double pvalue;
    private double tstat;

    public int getDesignElementId(){
        return designElementId;
    }
    public void setDesignElementId(int DesignElementId){
        this.designElementId = DesignElementId;
    }

    public double getPvalue(){
        return pvalue;
    }
    public void setPvalue(double pvalue){
        this.pvalue = pvalue;
    }

    public double getTstat(){
        return tstat;
    }
    public void setTstat(double tstat){
        this.tstat = tstat;
    }

}
