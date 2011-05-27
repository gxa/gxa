package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * User: nsklyar
 * Date: 17/05/2011
 */

public class MappingSource {

    private Long mappingSrcId;
    private Software software;
    private ArrayDesign arrayDesign;

    public MappingSource(Software software, ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
        this.software = software;
    }

    public Long getMappingSrcId() {
        return mappingSrcId;
    }

    public void setMappingSrcId(Long mappingSrcId) {
        this.mappingSrcId = mappingSrcId;
    }
}
