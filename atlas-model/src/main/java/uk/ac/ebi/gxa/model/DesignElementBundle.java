package uk.ac.ebi.gxa.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * User: Nataliya Sklyar
 * Date: Oct 21, 2010
 */
public class DesignElementBundle {

    private String arrayDesignName;
    private String arrayDesignAcc;

    private MappingSoftware software;
    private ListMultimap<String, String> designElementBioentities = ArrayListMultimap.create();

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
        designElementBioentities.putAll(de, bioentities);
    }

    public Collection<String> getDesignElements() {
        return designElementBioentities.keySet();
    }

    public void setDesignElementBioentities(ListMultimap<String, String> designElementBioentities) {
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
