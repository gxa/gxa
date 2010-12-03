package uk.ac.ebi.gxa.model;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.utils.ValueListHashMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Nataliya Sklyar
 * Date: Oct 21, 2010
 */
public class DesignElementBundle {

    private String arrayDesignName;
    private String arrayDesignAcc;

    private MappingSoftware software;
    private ValueListHashMap<String, String> designElementBioentities = new ValueListHashMap<String, String>();

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    public void setArrayDesignName(String arrayDesignName) {
        this.arrayDesignName = arrayDesignName;
    }

    public String getArrayDesignAcc() {
        return arrayDesignAcc;
    }

    public void setArrayDesignAcc(String arrayDesignAcc) {
        this.arrayDesignAcc = arrayDesignAcc;
    }

    public MappingSoftware getSoftware() {
        return software;
    }

    public void setSoftware(MappingSoftware software) {
        this.software = software;
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

    static class MappingSoftware {

        MappingSoftware(String name, String version) {
            this.name = name;
            this.version = version;
        }

        private String name;
        private String version;

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
