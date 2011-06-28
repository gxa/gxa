package uk.ac.ebi.gxa.loader.bioentity;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public abstract class AtlasBioentityAnnotator {

    private final Set<List<String>> geneTranscriptMapping = new HashSet<List<String>>();
    private final Set<List<String>> transcriptProperties = new HashSet<List<String>>();
    private final Set<List<String>> geneProperties = new HashSet<List<String>>();

    private final Set<BEPropertyValue> bePropertyValues = new HashSet<BEPropertyValue>();

    private final Set<BioEntity> transcripts = new HashSet<BioEntity>();
    private final Set<BioEntity> genes = new HashSet<BioEntity>();

    protected TransactionTemplate transactionTemplate;

    private AtlasLoaderServiceListener listener;

    protected Organism targetOrganism;
    //    protected Software software;
    protected AnnotationSource annotationSource;

    protected final AnnotationDAO annotationDAO;

    protected AtlasBioentityAnnotator( AnnotationDAO annotationDAO, TransactionTemplate transactionTemplate) {
        this.annotationDAO = annotationDAO;
        this.transactionTemplate= transactionTemplate;
    }

    protected void writeBioentitiesAndAnnotations(final String transcriptType, final String geneType) {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcripts.size() + " transcripts for " + finalOrganism.getName());
                annotationDAO.writeBioentities(transcripts);
                reportProgress("Wirting " + genes.size() + " genes for " + finalOrganism.getName());
                annotationDAO.writeBioentities(genes);
                reportProgress("Wirting " + bePropertyValues.size() + " property values " + finalOrganism.getName());
                annotationDAO.writePropertyValues(bePropertyValues);
            }
        });

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcriptProperties.size() + " properties for trasncripts " + finalOrganism.getName());
                annotationDAO.writeBioEntityToPropertyValues(transcriptProperties, transcriptType, annotationSource.getSoftware());
                if (StringUtils.isNotEmpty(geneType)) {
                    reportProgress("Wirting " + geneProperties.size() + " properties for genes " + finalOrganism.getName());
                    annotationDAO.writeBioEntityToPropertyValues(geneProperties, geneType, annotationSource.getSoftware());
                    reportProgress("Wirting " + geneTranscriptMapping.size() + " transcript to gene mappings " + finalOrganism.getName());
                    annotationDAO.writeGeneToTranscriptRelations(geneTranscriptMapping, transcriptType, geneType, annotationSource.getSoftware());
                }
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

    protected void addPropertyValue(String beIdentifier, String geneName, BioEntityProperty property, String value) {
        if (StringUtils.isNotBlank(value) && value.length() < 1000 && !"NA".equals(value)) {
            List<String> tnsProperty = new ArrayList<String>(3);
            tnsProperty.add(beIdentifier);
            tnsProperty.add(property.getName());
            tnsProperty.add(value);
            transcriptProperties.add(tnsProperty);

            BEPropertyValue propertyValue = new BEPropertyValue(property, value);
            bePropertyValues.add(propertyValue);


            if (geneName != null) {
                List<String> gProperty = new ArrayList<String>(3);
                gProperty.add(geneName);
                gProperty.add(property.getName());
                gProperty.add(value);
                geneProperties.add(gProperty);
            }
        }
    }

    protected void addTransctipt(Organism organism, String type, String beIdentifier) {
        BioEntity transcript = createBioEntity(organism, type, beIdentifier);
        transcripts.add(transcript);
    }

    protected void addGene(Organism organism, String type, String beIdentifier, String geneName) {
        BioEntity gene = createBioEntity(organism, type, beIdentifier);
        gene.setName(geneName);
        genes.add(gene);
    }

    private BioEntity createBioEntity(Organism organism, String type, String beIdentifier) {
        BioEntity bioEntity = new BioEntity(beIdentifier, annotationDAO.findOrCreateBioEntityType(type));
        bioEntity.setOrganism(organism);
        return bioEntity;
    }

    protected void addTranscriptGeneMapping(String beIdentifier, String geneIdentifier) {
        List<String> gnToTns = new ArrayList<String>(2);
        gnToTns.add(0, geneIdentifier);
        gnToTns.add(1, beIdentifier);
        geneTranscriptMapping.add(gnToTns);

    }


    protected void reportProgress(String report) {
        if (listener != null)
            listener.setProgress(report);
    }

    public void setListener(AtlasLoaderServiceListener listener) {
        this.listener = listener;
    }

}
