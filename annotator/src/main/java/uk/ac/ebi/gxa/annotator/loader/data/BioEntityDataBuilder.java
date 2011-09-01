package uk.ac.ebi.gxa.annotator.loader.data;

import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public abstract class BioEntityDataBuilder<T extends BioEntityData> {

    protected T data;
//    protected List<BioEntityType> types;

//    public BioEntityDataBuilder(List<BioEntityType> types) {
//        this.types = types;
//    }


    protected BioEntityDataBuilder() {
    }

    public abstract void createNewData(List<BioEntityType> types);

    public T getBioEntityData() throws AtlasAnnotationException {
        if (isValidData())
            return data;
        else
            throw new AtlasAnnotationException("Annotation/Mapping data is not valid");
    }

    protected abstract boolean isValidData();

//    public List<BioEntityType> getTypes() {
//        return Collections.unmodifiableList(types);
//    }

    public BioEntity addBioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        return data.addBioEntity(identifier, name, type, organism);
    }

    public abstract void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession);

    public abstract void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv);
}
