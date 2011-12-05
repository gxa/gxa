package uk.ac.ebi.gxa.annotator.process;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartParser;
import uk.ac.ebi.gxa.annotator.loader.data.*;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Iterables.getFirst;
import static java.lang.System.currentTimeMillis;

/**
 * User: nsklyar
 * Date: 02/12/2011
 */
public class Annotator {
        final private Logger log = LoggerFactory.getLogger(this.getClass());
    private AnnotationLoaderListener listener;

    private Organism organism;

    private AnnotationSourceDAO annSrcDAO;

    private BioEntityPropertyDAO propertyDAO;

    private final AtlasBioEntityDataWriter beDataWriter;

    public Annotator(AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, AtlasBioEntityDataWriter beDataWriter) {
        this.annSrcDAO = annSrcDAO;
        this.propertyDAO = propertyDAO;
        this.beDataWriter = beDataWriter;
    }

    public void updateAnnotations(String annotationSrcId) {

        BioMartAnnotationSource annSrc = null;
        try {
            annSrc = fetchAnnotationSource(annotationSrcId);

            organism = annSrc.getOrganism();
            reportProgress("Reading Ensembl annotations for organism " + organism.getName());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeMartAttributesHandler attributesHandler = new BETypeMartAttributesHandler(annSrc);
            BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
            BioMartParser<BioEntityAnnotationData> parser = BioMartParser.initParser(attributesHandler.getTypes(), builder);

            BioMartConnection martConnection = annSrc.createConnection();

            //Read BioEntities
            readBioEntities(martConnection.getAttributesURL(attributesHandler.getMartBEIdentifiersAndNames()), parser);

            //read synonyms
            fetchSynonyms(annSrc, builder);

            //read properties
            for (ExternalBioEntityProperty entityPropertyExternal : annSrc.getExternalBioEntityProperties()) {
                //List of Attributes contains for example: {"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}
                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(entityPropertyExternal.getName());

                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading property " + entityPropertyExternal.getBioEntityProperty().getName() + " (" + entityPropertyExternal.getName() + ") for " + organism.getName());
                    log.debug("Parsing property {} ", entityPropertyExternal.getBioEntityProperty().getName());
                    long startTime = currentTimeMillis();

                    parser.parseBioMartPropertyValues(entityPropertyExternal.getBioEntityProperty(), url);

                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }
            }

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writeBioEntities(data, listener);
            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, listener);

            reportSuccess("Update annotations for Organism " + annSrc.getOrganism().getName() + " completed");

        } catch (BioMartAccessException e) {
            reportError(new AtlasAnnotationException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e));
        } catch (AtlasAnnotationException e) {
            reportError(e);
        }
    }

    public void updateMappings(String annotationSrcId) {

        BioMartAnnotationSource annSrc = null;
        try {
            annSrc = fetchAnnotationSource(annotationSrcId);

            organism = annSrc.getOrganism();
            reportProgress("Reading Ensembl design element mappings for organism " + organism.getName());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeMartAttributesHandler attributesHandler = new BETypeMartAttributesHandler(annSrc);
            BioMartParser<DesignElementMappingData> parser = BioMartParser.initParser(attributesHandler.getTypes(), new DesignElementDataBuilder());


            BioMartConnection martConnection = annSrc.createConnection();
            if (!annSrc.isApplied()) {
                readBioEntities(martConnection.getAttributesURL(attributesHandler.getMartBEIdentifiersAndNames()), parser);
                beDataWriter.writeBioEntities(parser.getData(), listener);
            }

            for (ExternalArrayDesign externalArrayDesign : annSrc.getExternalArrayDesigns()) {
                parser.createNewBioEntityData();

                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(externalArrayDesign.getName());


                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading design elements for " + externalArrayDesign.getArrayDesign().getAccession() + " (" + externalArrayDesign.getName() + ") for " + organism.getName());

                    long startTime = currentTimeMillis();

                    parser.parseDesignElementMappings(url);
                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }

                beDataWriter.writeDesignElements(parser.getData(),
                        externalArrayDesign.getArrayDesign(),
                        annSrc.getSoftware(),
                        annSrcDAO.isAnnSrcAppliedForArrayDesignMapping(annSrc, externalArrayDesign.getArrayDesign()), listener);
            }

            reportSuccess("Update mappings for Organism " + annSrc.getOrganism().getName() + " completed");
        } catch (BioMartAccessException e) {
            reportError(new AtlasAnnotationException("Cannot update mappings for Organism.Problem when connecting to biomart. " + annSrc.getDatasetName(), e));
        } catch (AtlasAnnotationException e) {
            e.printStackTrace();
            reportError(e);
        }
    }

    private BioMartAnnotationSource fetchAnnotationSource(String annotationSrcId) throws AtlasAnnotationException {
        AnnotationSource annotationSource = annSrcDAO.getById(Long.parseLong(annotationSrcId), BioMartAnnotationSource.class);
        if (annotationSource == null) {
            throw new AtlasAnnotationException("No annotation source with id " + annotationSrcId);
        }

        return (BioMartAnnotationSource) annotationSource;
    }

    private <T extends BioEntityData> void readBioEntities(URL beURL, BioMartParser<T> parser) throws AtlasAnnotationException {
        if (beURL != null) {
            reportProgress("Reading bioentities for " + organism.getName());
            parser.parseBioEntities(beURL, organism);
        }
    }

    private void fetchSynonyms(BioMartAnnotationSource annSrc, BioEntityAnnotationDataBuilder builder) throws BioMartAccessException {
//        reportProgress("Reading synonyms for " + organism.getName());
//        BioMartDbDAO bioMartDbDAO = new BioMartDbDAO(annSrc.getMySqlDbUrl());
//
//        BioEntityType ensgene = annSrc.getBioEntityType(BioEntityType.ENSGENE);
//        if (ensgene == null) {
//            throw createUnexpected("Annotation source for " + annSrc.getOrganism().getName() + " is not for genes. Cannot fetch synonyms.");
//        }
//
//        Collection<Pair<String, String>> geneToSynonyms = bioMartDbDAO.getSynonyms(annSrc.getMySqlDbName(), annSrc.getSoftware().getVersion());
//        BioEntityProperty propSynonym = propertyDAO.findOrCreate("synonym");
//        for (Pair<String, String> geneToSynonym : geneToSynonyms) {
//            BEPropertyValue pv = new BEPropertyValue(null, propSynonym, geneToSynonym.getSecond());
//            builder.addPropertyValue(geneToSynonym.getFirst(), ensgene, pv);
//        }
    }

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
    }

    private void reportProgress(String report) {
        log.info(report);
        if (listener != null)
            listener.buildProgress(report);
    }

    void reportError(Throwable error) {
        log.error("Annotation failed! ", error);
        if (listener != null)
            listener.buildError(error);
    }

    void reportSuccess(String message) {
        log.info(message);
        if (listener != null)
            listener.buildSuccess(message);
    }

    static class BETypeMartAttributesHandler {

        private final List<BioEntityTypeColumns> bioEntityTypeColumns;
        private final Set<ExternalBioEntityProperty> externalBioEntityProperties;


        BETypeMartAttributesHandler(BioMartAnnotationSource annSrc) throws AtlasAnnotationException {
            this.externalBioEntityProperties = Collections.unmodifiableSet(annSrc.getExternalBioEntityProperties());
            bioEntityTypeColumns = new ArrayList<BioEntityTypeColumns>(annSrc.getTypes().size());
            for (BioEntityType type : annSrc.getTypes()) {

                Set<String> idPropertyNames = getBioMartPropertyNamesForProperty(type.getIdentifierProperty());
                if (idPropertyNames.isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }

                Set<String> namePropertyNames = getBioMartPropertyNamesForProperty(type.getNameProperty());
                if (namePropertyNames.isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }

                BioEntityTypeColumns columns = new BioEntityTypeColumns(type, getFirst(idPropertyNames, null), getFirst(namePropertyNames, null));
                bioEntityTypeColumns.add(columns);
            }
        }

        /**
         * Returns a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        public List<String> getMartBEIdentifiersAndNames() {
            List<String> answer =  new ArrayList<String>(bioEntityTypeColumns.size()*2);
            for (BioEntityTypeColumns bioEntityTypeColumn : bioEntityTypeColumns) {
                answer.add(bioEntityTypeColumn.getIdentifierColunm());
                answer.add(bioEntityTypeColumn.getNameColumn());
            }
            return Collections.unmodifiableList(answer);
        }

        public List<String> getMartBEIdentifiers() {
            return Collections.unmodifiableList(new ArrayList<String>(Collections2.transform(bioEntityTypeColumns, new Function<BioEntityTypeColumns, String>() {
                @Override
                public String apply(@Nonnull BioEntityTypeColumns bioEntityTypeColumns) {
                    return bioEntityTypeColumns.getIdentifierColunm();
                }
            })));
        }

        public List<BioEntityType> getTypes() {
            return Collections.unmodifiableList(new ArrayList<BioEntityType>(Collections2.transform(bioEntityTypeColumns, new Function<BioEntityTypeColumns, BioEntityType>() {
                @Override
                public BioEntityType apply(@Nonnull BioEntityTypeColumns bioEntityTypeColumns) {
                    return bioEntityTypeColumns.getType();
                }
            })));
        }

        private Set<String> getBioMartPropertyNamesForProperty(@Nonnull final BioEntityProperty beProperty) {
            return new HashSet<String>(Collections2.transform(
                    Collections2.filter(externalBioEntityProperties,
                            new Predicate<ExternalBioEntityProperty>() {
                                public boolean apply(@Nullable ExternalBioEntityProperty externalBioEntityProperty) {
                                    return externalBioEntityProperty != null && beProperty.equals(externalBioEntityProperty.getBioEntityProperty());
                                }
                            }),
                    new Function<ExternalBioEntityProperty, String>() {
                @Override
                public String apply(@Nonnull ExternalBioEntityProperty externalBioEntityProperty) {
                    return externalBioEntityProperty.getName();
                }
            }));
        }

        private static class BioEntityTypeColumns {
            BioEntityType type;
            String identifierColunm;
            String nameColumn;

            private BioEntityTypeColumns(BioEntityType type, String identifierColunm, String nameColumn) {
                this.type = type;
                this.identifierColunm = identifierColunm;
                this.nameColumn = nameColumn;
            }

            public BioEntityType getType() {
                return type;
            }

            public String getIdentifierColunm() {
                return identifierColunm;
            }

            public String getNameColumn() {
                return nameColumn;
            }
        }
    }
}
