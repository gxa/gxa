package uk.ac.ebi.gxa.loader.bioentity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public abstract class AtlasBioentityAnnotator {

    private final Set<List<BioEntity>> geneBioetityMapping = new HashSet<List<BioEntity>>();

    private Multimap<BioEntityType, List<String>> typeToBEPropValues= HashMultimap.create();

    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    private Multimap<BioEntityType, BioEntity> typeToBioentities = HashMultimap.create();

    protected TransactionTemplate transactionTemplate;

    private AtlasLoaderServiceListener listener;

    protected Organism targetOrganism;

    protected AnnotationSource annotationSource;

    protected final AnnotationDAO annotationDAO;

    protected AtlasBioentityAnnotator(AnnotationDAO annotationDAO, TransactionTemplate transactionTemplate) {
        this.annotationDAO = annotationDAO;
        this.transactionTemplate = transactionTemplate;
    }

    protected void writeBioentitiesAndAnnotations() {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for (BioEntityType type : typeToBioentities.keySet()) {
                    Collection<BioEntity> bioEntities = typeToBioentities.get(type);
                    reportProgress("Wirting " + bioEntities.size() + " " + type.getName() + " " + finalOrganism.getName());
                    annotationDAO.writeBioentities(bioEntities);
                }

                reportProgress("Wirting " + propertyValues.size() + " property values " + finalOrganism.getName());
                annotationDAO.writePropertyValues(propertyValues);
            }
        });

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for (BioEntityType type : typeToBEPropValues.keySet()) {
                    Collection<List<String>> propValues = typeToBEPropValues.get(type);
                    reportProgress("Wirting " + propValues.size() + " properties for " + type.getName() + " " + finalOrganism.getName());
                }

                annotationDAO.writeGeneToBioentityRelations(geneBioetityMapping, annotationSource.getSoftware());
            }
        });

        //ToDo:software should be set active outside of annotator
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Updating current annotation sources for " + annotationSource.getDisplayName());
                annotationSource.getSoftware().setActive(true);
                annotationDAO.saveSoftware(annotationSource.getSoftware());
            }
        });

    }

    protected void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        if (StringUtils.isNotBlank(pv.getValue()) && pv.getValue().length() < 1000 && !"NA".equals(pv.getValue())) {
            List<String> beProperty = new ArrayList<String>(3);
            beProperty.add(beIdentifier);
            beProperty.add(pv.getProperty().getName());
            beProperty.add(pv.getValue());
            typeToBEPropValues.put(type, beProperty);

            propertyValues.add(pv);
        }
    }

    protected BioEntity addBioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        BioEntity bioEntity = new BioEntity(identifier, name, type, organism);
        typeToBioentities.put(type, bioEntity);
        return bioEntity;
    }

    protected void addGeneBioEntityMapping(BioEntity gene, BioEntity bioEntity) {
        List<BioEntity> gnToTns = new ArrayList<BioEntity>(2);
        gnToTns.add(0, gene);
        gnToTns.add(1, bioEntity);
        geneBioetityMapping.add(gnToTns);
    }

    protected void reportProgress(String report) {
        if (listener != null)
            listener.setProgress(report);
    }

    public void setListener(AtlasLoaderServiceListener listener) {
        this.listener = listener;
    }

//    protected void initTypeBioentityMap(Collection<BioEntityType> types) {
//        for (BioEntityType type : types) {
//            typeToBioentities.put(type, new HashSet<BioEntity>());
//        }
//    }
}
