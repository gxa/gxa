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

package uk.ac.ebi.gxa.annotator;

import uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceConverter;
import uk.ac.ebi.gxa.annotator.annotationsrc.ConverterFactory;
import uk.ac.ebi.gxa.annotator.loader.Annotator;
import uk.ac.ebi.gxa.annotator.loader.AnnotatorFactory;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;

/**
 * User: nsklyar
 * Date: 07/11/2011
 */
public enum AnnotationSourceType {
    BIOMART(BioMartAnnotationSource.class, "BioMart") {
        @Override
        public AnnotationSourceConverter createConverter(ConverterFactory factory) {
            return factory.getBioMartAnnotationSourceConverter();
        }

        @Override
        public Annotator createAnnotator(AnnotatorFactory factory, AnnotationSource annSrc) {
            return factory.createBioMartAnnotator((BioMartAnnotationSource) annSrc);
        }
    },
    GENESIGDB(GeneSigAnnotationSource.class, "GeneSigDB") {
        @Override
        public Annotator createAnnotator(AnnotatorFactory factory, AnnotationSource annSrc) {
            return factory.createFileBasedAnnotator((GeneSigAnnotationSource) annSrc);
        }

        @Override
        public AnnotationSourceConverter createConverter(ConverterFactory factory) {
            return factory.getGeneSigAnnotationSourceConverter();
        }
    };

    private final Class<? extends AnnotationSource> clazz;
    private final String name;

    <T extends AnnotationSource> AnnotationSourceType(Class<T> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Class<? extends AnnotationSource> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public static AnnotationSourceType getByName(String name) {
        for (AnnotationSourceType annotationSourceType : values()) {
            if (annotationSourceType.getName().equalsIgnoreCase(name)) {
                return annotationSourceType;
            }
        }
        throw new IllegalArgumentException("There is no AnnotationSourceType with a name " + name);
    }

    public static <T extends AnnotationSource> AnnotationSourceType annSrcTypeOf(T annSrc) {
        for (AnnotationSourceType annotationSourceType : values()) {
            if (annotationSourceType.getClazz().equals(annSrc.getClass())) {
                return annotationSourceType;
            }
        }
        throw new IllegalArgumentException("There is no AnnotationSourceType for class " + annSrc.getClass());
    }

    public abstract Annotator createAnnotator(AnnotatorFactory factory, AnnotationSource annSrc);

    public abstract AnnotationSourceConverter createConverter(ConverterFactory factory);
}
