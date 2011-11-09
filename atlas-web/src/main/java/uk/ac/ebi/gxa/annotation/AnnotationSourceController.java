package uk.ac.ebi.gxa.annotation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.AnnotationLoaderException;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.AnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.BioMartAnnotationSourceLoader;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSourceClass;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.*;

/**
 * User: nsklyar
 * Date: 21/06/2011
 */
public class AnnotationSourceController {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BioMartAnnotationSourceLoader loader;

    @Autowired
    private AnnotationSourceManager annotationSourceManager;

    public AnnotationSourceController() {
    }

    public Collection<AnnotationSourceView> getBioMartAnnSrcViews() {
        List<AnnotationSourceView> viewSources = new ArrayList<AnnotationSourceView>();
        for (AnnotationSourceClass sourceClass : AnnotationSourceClass.values()) {
            final Collection<? extends AnnotationSource> currentAnnotationSourcesOfType = annotationSourceManager.getCurrentAnnotationSourcesOfType(sourceClass.getClazz());
            for (AnnotationSource annSrc : currentAnnotationSourcesOfType) {
                ValidationReport validationReport = new ValidationReport();
                validationReport.setMissingProperties(annSrc.findInvalidProperties());

                AnnotationSourceView view = new AnnotationSourceView(annSrc, validationReport);

                viewSources.add(view);
            }
        }
        Collections.sort(viewSources, new Comparator<AnnotationSourceView>() {
            @Override
            public int compare(AnnotationSourceView o, AnnotationSourceView o1) {
                return o.getOrganismName().compareTo(o1.getOrganismName());
            }
        });
        return viewSources;
    }

    public String getAnnSrcString(String id) {
        return loader.getAnnSrcAsStringById(id);
    }

    public String getAnnSrcString(String id, String type) {
        return annotationSourceManager.getAnnSrcString(id, AnnotationSourceClass.getByName(type));
    }

    public void saveAnnSrc(String text) {
        try {
            loader.saveAnnSrc(text);
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save AnnotationSource! ", e);
        }
    }

    public void saveAnnSrc(String id, String type, String text) {
        annotationSourceManager.saveAnnSrc(id, AnnotationSourceClass.getByName(type), text);
    }

    public static class AnnotationSourceView {
        private AnnotationSource annSrc;
        private ValidationReport validationReport;

        public AnnotationSourceView(AnnotationSource annSrc, ValidationReport validationReport) {
            this.annSrc = annSrc;
            this.validationReport = validationReport;
        }

        public String getAnnSrcId() {
            return String.valueOf(annSrc.getAnnotationSrcId());
        }

        public String getOrganismName() {
            if (annSrc instanceof BioMartAnnotationSource) {
                return ((BioMartAnnotationSource) annSrc).getOrganism().getName();
            }
            return "ANY";
        }

        public String getBioEntityTypes() {
            StringBuilder sb = new StringBuilder();
            int count = 1;
            for (BioEntityType type : annSrc.getTypes()) {
                sb.append(type.getName());
                if (count++ < annSrc.getTypes().size()) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }

        public String getSoftware() {
            return annSrc.getSoftware().getName() + " " + annSrc.getSoftware().getVersion();
        }

        public String getValidationMessage() {
            return validationReport.getSummary();
        }

        public String getApplied() {
            return annSrc.isApplied() ? "yes" : "no";
        }

        public String getType() {
            return AnnotationSourceClass.getByClass(annSrc.getClass()).getName();
        }
    }

    private static class ValidationReport {
        private String organismName;
        private Collection<String> missingProperties;

        public ValidationReport() {
        }

        public String getSummary() {
            if (isValid()) {
                return "Valid";
            }
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isEmpty(organismName)) {
                sb.append("Dataset name is not valid: " + organismName + "\n ");
            }
            if (!missingProperties.isEmpty()) {
                sb.append("Invalid properties: ");
                sb.append(missingProperties);
            }
            return sb.toString();
        }

        public void setMissingProperties(Collection<String> missingProperties) {
            this.missingProperties = missingProperties;
        }


        public boolean isValid() {
            return StringUtils.isEmpty(organismName) && CollectionUtils.isEmpty(missingProperties);
        }
    }
}
