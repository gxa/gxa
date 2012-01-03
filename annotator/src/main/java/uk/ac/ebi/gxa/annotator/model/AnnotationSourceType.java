package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.gxa.annotator.loader.annotationsrc.AnnotationSourceConverter;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.ConverterFactory;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.annotator.process.Annotator;
import uk.ac.ebi.gxa.annotator.process.AnnotatorFactory;

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
    FILE(GeneSigAnnotationSource.class, "GeneSigDB") {
        @Override
        public Annotator createAnnotator(AnnotatorFactory factory, AnnotationSource annSrc) {
            return factory.createFileBasedAnnotator((GeneSigAnnotationSource) annSrc);
        }

        @Override
        public AnnotationSourceConverter createConverter(ConverterFactory factory) {
            return factory.getFileBasedAnnotationSourceConverter();
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

    public static <T extends AnnotationSource> AnnotationSourceType getByClass(Class<T> clazz) {
        for (AnnotationSourceType annotationSourceType : values()) {
            if (annotationSourceType.getClazz().equals(clazz)) {
                return annotationSourceType;
            }
        }
        throw new IllegalArgumentException("There is no AnnotationSourceType for class " + clazz);
    }

    public abstract Annotator createAnnotator(AnnotatorFactory factory, AnnotationSource annSrc);

    public abstract AnnotationSourceConverter createConverter(ConverterFactory factory);
}
