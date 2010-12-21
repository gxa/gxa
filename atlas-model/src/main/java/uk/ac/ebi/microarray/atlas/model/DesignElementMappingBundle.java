package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Nataliya Sklyar
 * Date: Oct 21, 2010
 */
public class DesignElementMappingBundle {

    private String swName;
    private String swVersion;

    private String adName;
    private String adAccession;
    private String adType;
    private String adProvider;

    private List<Object[]> batch = new ArrayList<Object[]>();

    public DesignElementMappingBundle() {
    }

    public DesignElementMappingBundle(String swName, String swVersion, String adName, String adAccession, String adType, String adProvider) {
        this.swName = swName;
        this.swVersion = swVersion;
        this.adName = adName;
        this.adAccession = adAccession;
        this.adType = adType;
        this.adProvider = adProvider;
    }

    public String getSwName() {
        return swName;
    }

    public void setSwName(String swName) {
        this.swName = swName;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdAccession() {
        return adAccession;
    }

    public void setAdAccession(String adAccession) {
        this.adAccession = adAccession;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getAdProvider() {
        return adProvider;
    }

    public void setAdProvider(String adProvider) {
        this.adProvider = adProvider;
    }

    public List<Object[]> getBatch() {
        return batch;
    }

    public void setBatch(List<Object[]> batch) {
        this.batch = batch;
    }
}
