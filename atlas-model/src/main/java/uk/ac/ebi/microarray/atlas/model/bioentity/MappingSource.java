package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * User: nsklyar
 * Date: 17/05/2011
 */
public class MappingSource extends Software{

    private Long mappingSrcId;
    private ArrayDesign arrayDesign;

    public MappingSource(String name, String version, ArrayDesign arrayDesign) {
        super(name, version);
        this.arrayDesign = arrayDesign;
    }

    public Long getMappingSrcId() {
        return mappingSrcId;
    }

    public void setMappingSrcId(Long mappingSrcId) {
        this.mappingSrcId = mappingSrcId;
    }
}
