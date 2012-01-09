/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.annotator.loader;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommandListener;
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
    protected AnnotationCommandListener listener;

    protected T annSrc;
    protected final AtlasBioEntityDataWriter beDataWriter;

    protected Annotator(T annSrc, AtlasBioEntityDataWriter beDataWriter) {
        this.annSrc = annSrc;
        this.beDataWriter = beDataWriter;
    }

    public abstract void updateAnnotations();

    public abstract void updateMappings();

    public void setListener(AnnotationCommandListener listener) {
        this.listener = listener;
    }

    protected void reportProgress(String report) {
        log.info(report);
        if (listener != null)
            listener.commandProgress(report);
    }

    protected void reportError(Throwable error) {
        log.error("Annotation failed! ", error);
        if (listener != null)
            listener.commandError(error);
    }

    protected void reportSuccess(String message) {
        log.info(message);
        if (listener != null)
            listener.commandSuccess(message);
    }

    protected static class BETypeExternalAttributesHandler {

        private final List<BioEntityTypeColumns> bioEntityTypeColumns;
        private final Set<ExternalBioEntityProperty> externalBioEntityProperties;


        public BETypeExternalAttributesHandler(AnnotationSource annSrc) throws AnnotationException {
            this.externalBioEntityProperties = Collections.unmodifiableSet(annSrc.getExternalBioEntityProperties());
            bioEntityTypeColumns = new ArrayList<BioEntityTypeColumns>(annSrc.getTypes().size());
            for (BioEntityType type : annSrc.getTypes()) {

                if (getExternalPropertyNamesForProperty(type.getIdentifierProperty()).isEmpty()) {
                    throw new AnnotationException("Annotation source not valid ");
                }

                BioEntityTypeColumns columns = new BioEntityTypeColumns(type,
                        getFirst(getExternalPropertyNamesForProperty(type.getIdentifierProperty()), null)
                );
                bioEntityTypeColumns.add(columns);
            }
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
            return Collections.unmodifiableSet(new HashSet<BioEntityProperty>(Collections2.transform(
                    Collections2.filter(externalBioEntityProperties,
                            new Predicate<ExternalBioEntityProperty>() {
                                @Override
                                public boolean apply(@Nullable ExternalBioEntityProperty extBEProperty) {
                                    if (extBEProperty != null) {
                                        for (BioEntityTypeColumns bioEntityTypeColumn : bioEntityTypeColumns) {
                                            if (extBEProperty.getName().equals(bioEntityTypeColumn.getIdentifierColunm())) {
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

            private BioEntityTypeColumns(BioEntityType type, String identifierColunm) {
                this.type = type;
                this.identifierColunm = identifierColunm;
            }

            public BioEntityType getType() {
                return type;
            }

            public String getIdentifierColunm() {
                return identifierColunm;
            }
        }
    }
}
