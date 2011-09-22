package uk.ac.ebi.gxa.annotator.loader;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 27/06/2011
 */
@Service
public class AtlasBioEntityDataWriter {

    private static Logger log = LoggerFactory.getLogger(AtlasBioEntityDataWriter.class);

    @Autowired
    private BioEntityDAO bioEntityDAO;

    private AnnotationLoaderListener listener;

    public AtlasBioEntityDataWriter() {
    }

    @Transactional
    public void writeBioEntities(final BioEntityData data) {
        for (BioEntityType type : data.getBioEntityTypes()) {
            reportProgress("Writing bioentities of type " + type.getName() + "for Organism " + getOrganismNames(data));
            Collection<BioEntity> bioEntities = data.getBioEntitiesOfType(type);
            bioEntityDAO.writeBioEntities(bioEntities);
        }
    }

    @Transactional
    public void writePropertyValues(final Collection<BEPropertyValue> propertyValues) {
        reportProgress("Writing " + propertyValues.size() + "property values");
        bioEntityDAO.writePropertyValues(propertyValues);
    }

    @Transactional
    public void writeBioEntityToPropertyValues(final BioEntityAnnotationData data, final Software software, boolean deleteBeforeWrite) {
        if (deleteBeforeWrite) {
            for (Organism organism : data.getOrganisms()) {
                deleteBioEntityToPropertyValues(organism, software);
            }
        }
        for (BioEntityType type : data.getBioEntityTypes()) {
            Collection<Pair<String, BEPropertyValue>> propValues = data.getPropertyValuesForBioEntityType(type);
            reportProgress("Writing " + propValues.size() + " property values for " + type.getName() + " Organism: " + getOrganismNames(data));
            bioEntityDAO.writeBioEntityToPropertyValues(propValues, type, software);
        }
    }

     private void deleteBioEntityToPropertyValues(final Organism organism, final Software software) {
         reportProgress("Annotations for organism " + organism.getName() +
                 "already loaded and are going to be deleted before reloading ");
         int count = bioEntityDAO.deleteBioEntityToPropertyValues(organism, software);
         reportProgress("Deleted " + count + " annotations.");
     }

    @Transactional
    public void writeDesignElements(final DesignElementMappingData data, final ArrayDesign arrayDesign, final Software software, boolean deleteBeforeWrite) {
        if (deleteBeforeWrite) {
            deleteDesignElementBioEntityMappings(software, arrayDesign);
        }
        reportProgress("Writing " + data.getDesignElements().size() + " design elements of " + arrayDesign.getAccession());
        bioEntityDAO.writeDesignElements(data.getDesignElements(), arrayDesign);
        for (BioEntityType bioEntityType : data.getBioEntityTypes()) {
            Collection<Pair<String, String>> designElementToBioEntity = data.getDesignElementToBioEntity(bioEntityType);
            reportProgress("Writing " + designElementToBioEntity.size() + " design elements to " +
                    bioEntityType.getName() + " mappings of " + arrayDesign.getAccession());
            bioEntityDAO.writeDesignElementBioEntityMappings(designElementToBioEntity,
                    bioEntityType,
                    software,
                    arrayDesign);
        }
    }

     private void deleteDesignElementBioEntityMappings(final Software software, final ArrayDesign arrayDesign) {
         reportProgress("Mappings for array design " + arrayDesign.getAccession() +
                 "already loaded and are going to be deleted before reloading ");
         int count = bioEntityDAO.deleteDesignElementBioEntityMappings(software, arrayDesign);
         reportProgress("Deleted " + count + " mappings.");
     }

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
    }

    private Collection<String> getOrganismNames(final BioEntityData data) {
        return Collections2.transform(data.getOrganisms(), new Function<Organism, String>() {
            @Override
            public String apply(@Nonnull Organism organism) {
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
