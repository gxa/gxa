package uk.ac.ebi.gxa.annotator.loader.biomart;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.data.*;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartArrayDesign;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartProperty;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Iterables.getFirst;
import static java.lang.System.currentTimeMillis;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class BioMartAnnotator {
    final private Logger log = LoggerFactory.getLogger(this.getClass());
    private AnnotationLoaderListener listener;

    private Organism organism;

    private AnnotationSourceDAO annSrcDAO;

    private BioEntityPropertyDAO propertyDAO;

    protected final AtlasBioEntityDataWriter beDataWriter;

    public BioMartAnnotator(AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, AtlasBioEntityDataWriter beDataWriter) {
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


            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);

            //Read BioEntities
            readBioEntities(martConnection.getAttributesURL(attributesHandler.getMartBEIdentifiersAndNames()), parser);

            //read synonyms
            fetchSynonyms(annSrc, builder);

            //read properties
            for (BioMartProperty martProperty : annSrc.getBioMartProperties()) {
                //List of Attributes contains for example: {"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}
                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(martProperty.getName());

                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading property " + martProperty.getBioEntityProperty().getName() + " (" + martProperty.getName() + ") for " + organism.getName());
                    log.debug("Parsing property {} ", martProperty.getBioEntityProperty().getName());
                    long startTime = currentTimeMillis();

                    parser.parseBioMartPropertyValues(martProperty.getBioEntityProperty(), url);

                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }
            }

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writeBioEntities(data);
            beDataWriter.writePropertyValues(data.getPropertyValues());
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc.getSoftware(), annSrcDAO.isAnnSrcApplied(annSrc));

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


            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);
            if (!annSrcDAO.isAnnSrcApplied(annSrc)) {
                readBioEntities(martConnection.getAttributesURL(attributesHandler.getMartBEIdentifiersAndNames()), parser);
                beDataWriter.writeBioEntities(parser.getData());
            }


            for (BioMartArrayDesign bioMartArrayDesign : annSrc.getBioMartArrayDesigns()) {
                parser.createNewBioEntityData();

                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(bioMartArrayDesign.getName());


                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading design elements for " + bioMartArrayDesign.getArrayDesign().getAccession() + " (" + bioMartArrayDesign.getName() + ") for " + organism.getName());

                    long startTime = currentTimeMillis();

                    parser.parseDesignElementMappings(url);
                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }

                beDataWriter.writeDesignElements(parser.getData(),
                        bioMartArrayDesign.getArrayDesign(),
                        annSrc.getSoftware(),
                        annSrcDAO.isAnnSrcAppliedForArrayDesignMapping(annSrc, bioMartArrayDesign.getArrayDesign()));
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
        AnnotationSource annotationSource = annSrcDAO.getById(Long.parseLong(annotationSrcId));
        if (annotationSource == null) {
            throw new AtlasAnnotationException("No annotation source with id " + annotationSrcId);
        }

        return (BioMartAnnotationSource) annotationSource;
    }

    private <T extends BioEntityData> void readBioEntities(URL beURL, BioMartParser<T> parser) throws BioMartAccessException, AtlasAnnotationException {
        if (beURL != null) {
            reportProgress("Reading bioentities for " + organism.getName());
            parser.parseBioEntities(beURL, organism);
        }
    }


    private void fetchSynonyms(BioMartAnnotationSource annSrc, BioEntityAnnotationDataBuilder builder) throws BioMartAccessException {
        reportProgress("Reading synonyms for " + organism.getName());
        BioMartDbDAO bioMartDbDAO = new BioMartDbDAO(annSrc.getMySqlDbUrl());

        // Collect gene types for which synonyms are to be collected
        Collection<BioEntityType> ensemblGeneTypes = Collections2.filter(annSrc.getTypes(),
                new Predicate<BioEntityType>() {
                    public boolean apply(@Nullable BioEntityType bioEntityType) {
                        return bioEntityType != null && bioEntityType.getName().equals(BioEntityType.ENSGENE);
                    }
                });

        BioEntityType geneType = getFirst(ensemblGeneTypes, null); // We need only one BioEntityType of type BioEntityType.ENSGENE
        if (geneType != null) {
            HashSet<Pair<String, String>> geneToSynonyms = bioMartDbDAO.getSynonyms(annSrc.getMySqlDbName(), annSrc.getSoftware().getVersion());
            BioEntityProperty propSynonym = propertyDAO.findOrCreate("synonym");
            for (Pair<String, String> geneToSynonym : geneToSynonyms) {
                BEPropertyValue pv = new BEPropertyValue(null, propSynonym, geneToSynonym.getSecond());
                builder.addPropertyValue(geneToSynonym.getFirst(), geneType, pv);
            }

        } else {
            throw LogUtil.createUnexpected("Annotation source for " + annSrc.getOrganism().getName() + " is not for genes. Cannot fetch synonyms.");
        }
    }

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
        this.beDataWriter.setListener(listener);
    }

    private void reportProgress(String report) {
        log.info(report);
        if (listener != null)
            listener.buildProgress(report);
    }

    protected void reportError(Throwable error) {
        log.error("Annotation failed! ", error);
        if (listener != null)
            listener.buildError(error);
    }

    protected void reportSuccess(String message) {
        log.info(message);
        if (listener != null)
            listener.buildSuccess(message);
    }

    static class BETypeMartAttributesHandler {

        private final List<BioEntityType> types;
        private final Set<BioMartProperty> bioMartProperties;
        private final List<String> martBEIdentifiersAndNames = new ArrayList<String>();
        private final List<String> martBEIdentifiers = new ArrayList<String>();

        private BETypeMartAttributesHandler(BioMartAnnotationSource annSrc) throws AtlasAnnotationException {
            this.types = Collections.unmodifiableList(new LinkedList<BioEntityType>(annSrc.getTypes()));
            this.bioMartProperties = annSrc.getBioMartProperties();
            initMartAttributes();
        }

        /**
         * Initialize a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        private void initMartAttributes() throws AtlasAnnotationException {
            for (BioEntityType type : types) {

                Set<String> idPropertyNames = getBioMartPropertyNamesForProperty(type.getIdentifierProperty());
                if (idPropertyNames.isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }
                martBEIdentifiersAndNames.add(getFirst(idPropertyNames, null));
                martBEIdentifiers.add(getFirst(idPropertyNames, null));

                Set<String> namePropertyNames = getBioMartPropertyNamesForProperty(type.getNameProperty());
                if (namePropertyNames.isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }
                martBEIdentifiersAndNames.add(getFirst(namePropertyNames, null));

            }
        }

        /**
         * Returns a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        public List<String> getMartBEIdentifiersAndNames() {
            return Collections.unmodifiableList(martBEIdentifiersAndNames);
        }

        public List<String> getMartBEIdentifiers() {
            return Collections.unmodifiableList(martBEIdentifiers);
        }

        public List<BioEntityType> getTypes() {
            return types;
        }

        private Set<String> getBioMartPropertyNamesForProperty(@Nonnull final BioEntityProperty beProperty) {
            return new HashSet<String>(Collections2.transform(
                    Collections2.filter(bioMartProperties,
                            new Predicate<BioMartProperty>() {
                                public boolean apply(@Nullable BioMartProperty bioMartProperty) {
                                    return bioMartProperty != null && beProperty.equals(bioMartProperty.getBioEntityProperty());
                                }
                            }), new Function<BioMartProperty, String>() {
                        @Override
                        public String apply(@Nonnull BioMartProperty bioMartProperty) {
                            return bioMartProperty.getName();
                        }
                    }));
        }

    }
}
