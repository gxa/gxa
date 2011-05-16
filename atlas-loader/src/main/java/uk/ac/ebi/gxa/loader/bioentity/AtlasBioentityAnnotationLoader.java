package uk.ac.ebi.gxa.loader.bioentity;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public abstract class AtlasBioentityAnnotationLoader {

    private final Set<List<String>> geneTranscriptMapping = new HashSet<List<String>>();
    private final Set<List<String>> transcriptProperties = new HashSet<List<String>>();
    private final Set<List<String>> geneProperties = new HashSet<List<String>>();

    private final Set<BEPropertyValue> bePropertyValues = new HashSet<BEPropertyValue>();

    private final Set<BioEntity> transcripts = new HashSet<BioEntity>();
    private final Set<BioEntity> genes = new HashSet<BioEntity>();

    protected BioEntityDAO bioEntityDAO;

    private TransactionTemplate transactionTemplate;

    private AtlasLoaderServiceListener listener;

    protected String targetOrganism;
    protected Software software;

    protected void writeBioentitiesAndAnnotations(final String transcriptType, final String geneType) {
        final String finalOrganism = targetOrganism;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcripts.size() + " transcripts for " + finalOrganism);
                bioEntityDAO.writeBioentities(transcripts);
                reportProgress("Wirting " + genes.size() + " genes for " + finalOrganism);
                bioEntityDAO.writeBioentities(genes);
                reportProgress("Wirting " + bePropertyValues.size() + " property values " + finalOrganism);
                bioEntityDAO.writePropertyValues(bePropertyValues);
            }
        });

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                reportProgress("Wirting " + transcriptProperties.size() + " properties for trasncripts " + finalOrganism);
                bioEntityDAO.writeBioEntityToPropertyValues(transcriptProperties, transcriptType, software);
                if (StringUtils.isNotEmpty(geneType)) {
                    reportProgress("Wirting " + geneProperties.size() + " properties for genes " + finalOrganism);
                    bioEntityDAO.writeBioEntityToPropertyValues(geneProperties, geneType, software);
                    reportProgress("Wirting " + geneTranscriptMapping.size() + " transcript to gene mappings " + finalOrganism);
                    bioEntityDAO.writeGeneToTranscriptRelations(geneTranscriptMapping, transcriptType, geneType, software);
                }
            }
        });
    }

    protected void addPropertyValue(String beIdentifier, String geneName, String propertyName, String value) {
        if (StringUtils.isNotBlank(value) && value.length() < 1000 && !"NA".equals(value)) {
            List<String> tnsProperty = new ArrayList<String>(3);
            tnsProperty.add(beIdentifier);
            tnsProperty.add(propertyName);
            tnsProperty.add(value);
            transcriptProperties.add(tnsProperty);

            BEPropertyValue propertyValue = new BEPropertyValue(propertyName, value);
            bePropertyValues.add(propertyValue);


            if (geneName != null) {
                List<String> gProperty = new ArrayList<String>(3);
                gProperty.add(geneName);
                gProperty.add(propertyName);
                gProperty.add(value);
                geneProperties.add(gProperty);
            }
        }
    }

    protected void addTransctipt(String organism, String type, String beIdentifier) {
        BioEntity transcript = createBioEntity(organism, type, beIdentifier);
        transcripts.add(transcript);
    }

    protected void addGene(String organism, String type, String beIdentifier, String geneName) {
        BioEntity gene = createBioEntity(organism, type, beIdentifier);
        gene.setName(geneName);
        genes.add(gene);
    }

    private BioEntity createBioEntity(String organism, String type, String beIdentifier) {
        BioEntity bioEntity = new BioEntity(beIdentifier, BioEntityType.parse(type));
        bioEntity.setSpecies(organism);
        return bioEntity;
    }
    protected void addTranscriptGeneMapping(String beIdentifier, String geneIdentifier) {
        List<String> gnToTns = new ArrayList<String>(2);
        gnToTns.add(0, geneIdentifier);
        gnToTns.add(1, beIdentifier);
        geneTranscriptMapping.add(gnToTns);

    }

    protected void writeProperties(final HashSet<String> properties) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                bioEntityDAO.writeProperties(properties);
            }
        });
    }

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
