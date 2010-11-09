package uk.ac.ebi.microarray.atlas.model;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.utils.ValueListHashMap;

import java.util.Collection;
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

    private ValueListHashMap<String, String> designElementBioentities;


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

    public String getSwVersion() {
        return swVersion;
    }

    public String getAdName() {
        return adName;
    }

    public String getAdAccession() {
        return adAccession;
    }

    public String getAdType() {
        return adType;
    }

    public String getAdProvider() {
        return adProvider;
    }

    public void addDesignElement(String de) {
        designElementBioentities.put(de, StringUtils.EMPTY);
    }

    public void addDesignElementBioentity(String de, String bioentity) {
        designElementBioentities.put(de, bioentity);
    }

    public void addDesignElementBioentities(String de, List<String> bioentities) {
        designElementBioentities.put(de, bioentities);
    }

    public Collection<String> getDesignElements() {
        return designElementBioentities.keySet();
    }

    public void setDesignElementBioentities(ValueListHashMap<String, String> designElementBioentities) {
        this.designElementBioentities = designElementBioentities;
    }

    public ValueListHashMap<String, String> getDesignElementBioentities() {
        return designElementBioentities;
    }

    //    public class ArrayDesign {
//
//    private String name;
//    private String accession;
//    private String type;
//    private String provider;
//
//    public ArrayDesign(String name, String accession, String type, String provider) {
//        this.name = name;
//        this.accession = accession;
//        this.type = type;
//        this.provider = provider;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getAccession() {
//        return accession;
//    }
//
//    public String getType() {
//        return type;
//    }
//
//    public String getProvider() {
//        return provider;
//    }
//}
}
