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
