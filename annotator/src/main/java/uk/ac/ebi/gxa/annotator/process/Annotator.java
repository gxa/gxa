package uk.ac.ebi.gxa.annotator.process;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Iterables.getFirst;

/**
 * User: nsklyar
 * Date: 02/12/2011
 */
public abstract class Annotator<T extends AnnotationSource> {
    final private Logger log = LoggerFactory.getLogger(this.getClass());
    protected AnnotationLoaderListener listener;

    protected T annSrc;
    protected final AtlasBioEntityDataWriter beDataWriter;

    public Annotator(T annSrc, AtlasBioEntityDataWriter beDataWriter) {
        this.annSrc = annSrc;
        this.beDataWriter = beDataWriter;
    }

    public abstract void updateAnnotations();

    public abstract void updateMappings();

    public void setListener(AnnotationLoaderListener listener) {
        this.listener = listener;
    }

    protected void reportProgress(String report) {
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

    protected static class BETypeExternalAttributesHandler {

        private final List<BioEntityTypeColumns> bioEntityTypeColumns;
        private final Set<ExternalBioEntityProperty> externalBioEntityProperties;


        public BETypeExternalAttributesHandler(AnnotationSource annSrc) throws AtlasAnnotationException {
            this.externalBioEntityProperties = Collections.unmodifiableSet(annSrc.getExternalBioEntityProperties());
            bioEntityTypeColumns = new ArrayList<BioEntityTypeColumns>(annSrc.getTypes().size());
            for (BioEntityType type : annSrc.getTypes()) {

//                Set<String> idPropertyNames = getExternalPropertyNamesForProperty(type.getIdentifierProperty());
                if (getExternalPropertyNamesForProperty(type.getIdentifierProperty()).isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }

                Set<String> namePropertyNames = getExternalPropertyNamesForProperty(type.getNameProperty());
                if (namePropertyNames.isEmpty()) {
                    throw new AtlasAnnotationException("Annotation source not valid ");
                }

                BioEntityTypeColumns columns = new BioEntityTypeColumns(type, getFirst(getExternalPropertyNamesForProperty(type.getIdentifierProperty()), null), getFirst(namePropertyNames, null));
                bioEntityTypeColumns.add(columns);
            }
        }

        /**
         * Returns a List of external attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        public List<String> getExternalBEIdentifiersAndNames() {
            List<String> answer = new ArrayList<String>(bioEntityTypeColumns.size() * 2);
            for (BioEntityTypeColumns bioEntityTypeColumn : bioEntityTypeColumns) {
                answer.add(bioEntityTypeColumn.getIdentifierColunm());
                answer.add(bioEntityTypeColumn.getNameColumn());
            }
            return Collections.unmodifiableList(answer);
        }

        public List<String> getExternalBEIdentifiers() {
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

        public Collection<BioEntityProperty> getBioEntityProperties() {
            return Collections.unmodifiableSet(new  HashSet<BioEntityProperty>(Collections2.transform(
                    Collections2.filter(externalBioEntityProperties,
                            new Predicate<ExternalBioEntityProperty>() {
                                @Override
                                public boolean apply(@Nullable ExternalBioEntityProperty extBEProperty) {
                                    if (extBEProperty!= null) {
                                        for (BioEntityTypeColumns bioEntityTypeColumn : bioEntityTypeColumns) {
                                            if (extBEProperty.getName().equals(bioEntityTypeColumn.getIdentifierColunm())
                                            || extBEProperty.getName().equals(bioEntityTypeColumn.getNameColumn())) {
                                               return false;
                                            }
                                        }
                                    }
                                    return true;
                                }
                            }),
                    new Function<ExternalBioEntityProperty, BioEntityProperty>() {
                        @Override
                        public BioEntityProperty apply(@Nonnull ExternalBioEntityProperty externalBioEntityProperty) {
                            return externalBioEntityProperty.getBioEntityProperty();
                        }
                    })));
        }

        private Set<String> getExternalPropertyNamesForProperty(@Nonnull final BioEntityProperty beProperty) {
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
