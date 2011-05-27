package uk.ac.ebi.gxa.loader.bioentity;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.CurrentAnnotationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public abstract class AtlasBioentityAnnotationLoader{

    private final Set<List<String>> geneTranscriptMapping = new HashSet<List<String>>();
    private final Set<List<String>> transcriptProperties = new HashSet<List<String>>();
    private final Set<List<String>> geneProperties = new HashSet<List<String>>();

    private final Set<BEPropertyValue> bePropertyValues = new HashSet<BEPropertyValue>();

    private final Set<BioEntity> transcripts = new HashSet<BioEntity>();
    private final Set<BioEntity> genes = new HashSet<BioEntity>();

    protected BioEntityDAO bioEntityDAO;

    protected TransactionTemplate transactionTemplate;

    private AtlasLoaderServiceListener listener;

    protected Organism targetOrganism;
    //    protected Software software;
    protected AnnotationSource annotationSource;

    protected AnnotationSourceDAO annSrcDAO;

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    protected void writeBioentitiesAndAnnotations(final String transcriptType, final String geneType) {
        final Organism finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcripts.size() + " transcripts for " + finalOrganism.getName());
                bioEntityDAO.writeBioentities(transcripts);
                reportProgress("Wirting " + genes.size() + " genes for " + finalOrganism.getName());
                bioEntityDAO.writeBioentities(genes);
                reportProgress("Wirting " + bePropertyValues.size() + " property values " + finalOrganism.getName());
                bioEntityDAO.writePropertyValues(bePropertyValues);
            }
        });

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcriptProperties.size() + " properties for trasncripts " + finalOrganism.getName());
                bioEntityDAO.writeBioEntityToPropertyValues(transcriptProperties, transcriptType, annotationSource);
                if (StringUtils.isNotEmpty(geneType)) {
                    reportProgress("Wirting " + geneProperties.size() + " properties for genes " + finalOrganism.getName());
                    bioEntityDAO.writeBioEntityToPropertyValues(geneProperties, geneType, annotationSource);
                    reportProgress("Wirting " + geneTranscriptMapping.size() + " transcript to gene mappings " + finalOrganism.getName());
                    bioEntityDAO.writeGeneToTranscriptRelations(geneTranscriptMapping, transcriptType, geneType, annotationSource);
                }
            }
        });

        //Update current Annotation sources
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Updating current annotation sources for " + annotationSource.getDisplayName());
                Collection<CurrentAnnotationSource<? extends AnnotationSource>> currentAnnotationSources = annotationSource.generateCurrentAnnSrcs();
                for (CurrentAnnotationSource currentAnnotationSource : currentAnnotationSources) {
                    annSrcDAO.saveCurrentAnnotationSource(currentAnnotationSource);
                }
            }
        });

    }

    protected void addPropertyValue(String beIdentifier, String geneName, BEProperty property, String value) {
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
        BioEntity bioEntity = new BioEntity(beIdentifier, bioEntityDAO.findOrCreateBioEntityType(type));
        bioEntity.setOrganism(organism);
        return bioEntity;
    }

    protected void addTranscriptGeneMapping(String beIdentifier, String geneIdentifier) {
        List<String> gnToTns = new ArrayList<String>(2);
        gnToTns.add(0, geneIdentifier);
        gnToTns.add(1, beIdentifier);
        geneTranscriptMapping.add(gnToTns);

    }

//    protected void saveProperies(final HashSet<String> properties) {
//        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
//                bioEntityDAO.saveProperies(properties);
//            }
//        });
//    }

    protected void reportProgress(String report) {
        if (listener != null)
            listener.setProgress(report);
    }

    public void setListener(AtlasLoaderServiceListener listener) {
        this.listener = listener;
    }

    /////////// Dependency injection /////////////
    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setTxManager(PlatformTransactionManager txManager) {
        Assert.notNull(txManager, "The 'transactionManager' argument must not be null.");
        this.transactionTemplate = new TransactionTemplate(txManager);

    }
}
