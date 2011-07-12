package uk.ac.ebi.gxa.loader.dao;

import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.dao.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.MappingSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 27/06/2011
 */
public class AnnotationDAO {
    private final BioEntityDAO bioEntityDAO;
    private final AnnotationSourceDAO annSrcDAO;
    private final BioEntityPropertyDAO propertyDAO;
    private final SoftwareDAO softwareDAO;

    public AnnotationDAO(BioEntityDAO bioEntityDAO, AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, SoftwareDAO softwareDAO) {
        this.bioEntityDAO = bioEntityDAO;
        this.annSrcDAO = annSrcDAO;
        this.propertyDAO = propertyDAO;
        this.softwareDAO = softwareDAO;
    }

    public void writeBioentities(Collection<BioEntity> bioEntities) {
        bioEntityDAO.writeBioentities(bioEntities);
    }

    public void writePropertyValues(Collection<BEPropertyValue> propertyValues) {
        bioEntityDAO.writePropertyValues(propertyValues);
    }

    public void writeBioEntityToPropertyValues(Set<List<String>> beProperties, BioEntityType beType, Software software) {
        bioEntityDAO.writeBioEntityToPropertyValues(beProperties, beType, software);
    }

    public void writeGeneToBioentityRelations(Set<List<BioEntity>> relations, Software software) {
        bioEntityDAO.writeGeneToBioentityRelations(relations, software);
    }

    public void writeArrayDesign(ArrayDesign arrayDesign, MappingSource mappingSrc) {
        bioEntityDAO.writeArrayDesign(arrayDesign, mappingSrc);
    }

    public void writeDesignElements(List<DesignElement> designElements, String arrayDesignAccession) {
        bioEntityDAO.writeDesignElements(designElements, arrayDesignAccession);
    }

    public void writeDesignElementBioentityMappings(Collection<List<String>> deToBeMappings, String beType, MappingSource mappingSrc, String arrayDesignAccession) {
        bioEntityDAO.writeDesignElementBioentityMappings(deToBeMappings, beType, mappingSrc, arrayDesignAccession);
    }

    public AnnotationSource getAnnSrcById(long id) {
        return annSrcDAO.getById(id);
    }

    public BioEntityProperty getPropertyByName(String name) {
        return propertyDAO.getByName(name);
    }

    public void saveProperty(BioEntityProperty object) {
        propertyDAO.save(object);
    }

    public void saveSoftware(Software object) {
        softwareDAO.save(object);
    }

    public BioEntityType findOrCreateBioEntityType(String name) {
        return bioEntityDAO.findOrCreateBioEntityType(name);
    }

    public Organism findOrCreateOrganism(String organism) {
        return annSrcDAO.findOrCreateOrganism(organism);
    }

    public void saveAnnSrc(AnnotationSource annotationSource) {
        annSrcDAO.save(annotationSource);
    }

}
