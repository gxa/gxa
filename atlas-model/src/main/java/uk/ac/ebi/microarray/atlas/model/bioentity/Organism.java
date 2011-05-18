package uk.ac.ebi.microarray.atlas.model.bioentity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * User: nsklyar
 * Date: 04/05/2011
 */
public class Organism {
    private long organismid;
    private String atlasName;

    public Organism(String atlasName) {
        this.atlasName = atlasName;
    }

    public long getOrganismid() {
        return organismid;
    }

    public void setOrganismid(long organismid) {
        this.organismid = organismid;
    }

    public String getAtlasName() {
        return atlasName;
    }

    public void setAtlasName(String atlasName) {
        this.atlasName = atlasName;
    }

}
