package ae3.model;

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
        return efv;
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
}
