package uk.ac.ebi.gxa.annotator.loader;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.annotator.dao.AnnotationDAO;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public abstract class AtlasBioentityAnnotator {

    private final Set<List<BioEntity>> geneBioetityMapping = new HashSet<List<BioEntity>>();

    private Multimap<BioEntityType, List<String>> typeToBEPropValues = HashMultimap.create();

    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    private Multimap<BioEntityType, BioEntity> typeToBioentities = HashMultimap.create();

    private Multimap<BioEntityType, List<String>> typeToDesignElementBEMapping = HashMultimap.create();
    private Set<DesignElement> designElements = new HashSet<DesignElement>();

    protected TransactionTemplate transactionTemplate;

    private AnnotationLoaderListener listener;

    protected Organism targetOrganism;

    protected AnnotationSource annotationSource;

    protected final AnnotationDAO annotationDAO;

    private static Logger log = LoggerFactory.getLogger(AtlasBioentityAnnotator.class);

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
    }

    protected AtlasBioentityAnnotator(AnnotationDAO annotationDAO, TransactionTemplate transactionTemplate) {
        this.annotationDAO = annotationDAO;
        this.transactionTemplate = transactionTemplate;
    }

    protected void writeBioentitiesAndAnnotations() {
        writeBioentitiesToDB();

        writePropertyValuesToDB();

        writeBEPropertyValueMappingsToDB();

        writeGeneToBioentityRelationsToDB();

        //ToDo:software should be set active outside of annotator
        setSoftwareAsActive();
    }

    protected void writeBioentitiesToDB() {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for (BioEntityType type : typeToBioentities.keySet()) {
                    Collection<BioEntity> bioEntities = typeToBioentities.get(type);
                    reportProgress("Wirting " + bioEntities.size() + " " + type.getName() + " " + finalOrganism.getName());
                    annotationDAO.writeBioentities(bioEntities);
                }
            }
        });
    }

    protected void writePropertyValuesToDB() {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + propertyValues.size() + " property values " + finalOrganism.getName());
                annotationDAO.writePropertyValues(propertyValues);
            }
        });
    }

    protected void writeBEPropertyValueMappingsToDB() {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for (BioEntityType type : typeToBEPropValues.keySet()) {
                    Set<List<String>> propValues = (Set<List<String>>) typeToBEPropValues.get(type);
                    reportProgress("Wirting " + propValues.size() + " properties for " + type.getName() + " " + finalOrganism.getName());
                    annotationDAO.writeBioEntityToPropertyValues(propValues, type, annotationSource.getSoftware());
                }
            }
        });
    }

    protected void writeGeneToBioentityRelationsToDB() {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + geneBioetityMapping.size() + "  bioentities mapped to gene for " + finalOrganism.getName());
                annotationDAO.writeGeneToBioentityRelations(geneBioetityMapping, annotationSource.getSoftware());
            }
        });
    }

    protected void setSoftwareAsActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Updating current annotation sources for " + annotationSource.getDisplayName());
                annotationSource.getSoftware().setActive(true);
                annotationDAO.saveSoftware(annotationSource.getSoftware());
            }
        });
    }

    protected void writeDesignElementBEMappingsToDB(final ArrayDesign arrayDesign) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                annotationDAO.writeDesignElements(designElements, arrayDesign);
                for (BioEntityType bioEntityType : typeToDesignElementBEMapping.keySet()) {
                    annotationDAO.writeDesignElementBioentityMappings(typeToDesignElementBEMapping.get(bioEntityType),
                            bioEntityType,
                            annotationSource.getSoftware(),
                            arrayDesign);
                }
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

    protected void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        if (StringUtils.isNotBlank(deAccession) && deAccession.length() < 1000 && !"NA".equals(deAccession)) {
            List<String> de2be = new ArrayList<String>(2);
            de2be.add(deAccession);
            de2be.add(beIdentifier);
            typeToDesignElementBEMapping.put(type, de2be);

            DesignElement designElement = new DesignElement(deAccession, deAccession);
            designElements.add(designElement);
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
        log.info(report);
        if (listener != null)
            listener.buildProgress(report);
    }

    protected void clearTypeToDesignElementBEMapping() {
        typeToDesignElementBEMapping.clear();
    }

    protected void clearDesignElements() {
        designElements.clear();
    }
//    protected void initTypeBioentityMap(Collection<BioEntityType> types) {
//        for (BioEntityType type : types) {
//            typeToBioentities.put(type, new HashSet<BioEntity>());
//        }
//    }
}
