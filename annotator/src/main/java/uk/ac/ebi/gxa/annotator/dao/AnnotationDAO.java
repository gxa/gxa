package uk.ac.ebi.gxa.annotator.dao;

import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
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

    public void writeArrayDesign(ArrayDesign arrayDesign, Software software) {
        bioEntityDAO.writeArrayDesign(arrayDesign, software);
    }

    public void writeDesignElements(Set<DesignElement> designElements, ArrayDesign arrayDesign) {
        bioEntityDAO.writeDesignElements(designElements, arrayDesign);
    }

    public void writeDesignElementBioentityMappings(Collection<List<String>> deToBeMappings, BioEntityType beType, Software software, ArrayDesign arrayDesign) {
        bioEntityDAO.writeDesignElementBioentityMappings(deToBeMappings, beType, software, arrayDesign);
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

    public Software findOrCreateSoftware(String name, String newVersion) {
        return softwareDAO.findOrCreate(name, newVersion);
    }
}
