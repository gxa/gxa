package uk.ac.ebi.gxa.annotator.loader;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 27/06/2011
 */
@Service
public class AtlasBioEntityDataWriter {

    private static Logger log = LoggerFactory.getLogger(AtlasBioEntityDataWriter.class);

    @Autowired
    private BioEntityDAO bioEntityDAO;
    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private SoftwareDAO softwareDAO;

    private AnnotationLoaderListener listener;

    public AtlasBioEntityDataWriter() {
    }

    @Transactional
    public void writeBioEntities(Collection<BioEntity> bioEntities) {
        bioEntityDAO.writeBioEntities(bioEntities);
    }

    @Transactional
    public void writeBioEntities(BioEntityData data) {
        for (BioEntityType type : data.getBioEntityTypes()) {
            reportProgress("Writing bioentities of type " + type.getName() + "for Organism " + getOrganismNames(data));
            Collection<BioEntity> bioEntities = data.getBioEntitiesOfType(type);
            bioEntityDAO.writeBioEntities(bioEntities);
        }
    }

    @Transactional
    public void writePropertyValues(Collection<BEPropertyValue> propertyValues) {
        reportProgress("Writing " + propertyValues.size() + "property values");
        bioEntityDAO.writePropertyValues(propertyValues);
    }

    @Transactional
    public void writeBioEntityToPropertyValues(final BioEntityAnnotationData data, Software software) {
        for (BioEntityType type : data.getBioEntityTypes()) {
            Collection<List<String>> propValues = data.getPropertyValuesForBioEntityType(type);
            reportProgress("Wirting " + propValues.size() + " property values for " + type.getName() + " Organism: " + getOrganismNames(data));
            bioEntityDAO.writeBioEntityToPropertyValues(propValues, type, software);
        }
    }

    @Transactional
    public void writeDesignElements(final DesignElementMappingData data, final ArrayDesign arrayDesign, Software software) {
        reportProgress("Writing " + data.getDesignElements().size() + " design elements of " + arrayDesign.getAccession());
        bioEntityDAO.writeDesignElements(data.getDesignElements(), arrayDesign);
        for (BioEntityType bioEntityType : data.getBioEntityTypes()) {
            Collection<Pair<String,String>> designElementToBioEntity = data.getDesignElementToBioEntity(bioEntityType);
            reportProgress("Writing " + designElementToBioEntity.size() + " design elements to bioentity mappings of " + arrayDesign.getAccession());
            bioEntityDAO.writeDesignElementBioEntityMappings(designElementToBioEntity,
                    bioEntityType,
                    software,
                    arrayDesign);
        }
    }

    @Transactional
    public AnnotationSource getAnnSrcById(long id) {
        return annSrcDAO.getById(id);
    }

    public BioEntityProperty getPropertyByName(String name) {
        return propertyDAO.getByName(name);
    }

    @Transactional
    public void saveSoftware(Software object) {
        softwareDAO.save(object);
    }

    public boolean isAnnSrcApplied(AnnotationSource annSrc) {
        return annSrcDAO.isAnnSrcApplied(annSrc);
    }

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
    }

    private Collection<String> getOrganismNames(BioEntityData data) {
        return Collections2.transform(data.getOrganisms(), new Function<Organism, String>() {
            @Override
            public String apply(@Nullable Organism organism) {
                return organism.getName();
            }
        });
    }

    protected void reportProgress(String report) {
        log.info(report);
        if (listener != null)
            listener.buildProgress(report);
    }
}
