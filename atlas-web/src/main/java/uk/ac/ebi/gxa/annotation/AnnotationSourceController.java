package uk.ac.ebi.gxa.annotation;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.*;

/**
 * User: nsklyar
 * Date: 21/06/2011
 */
public class AnnotationSourceController {

    @Autowired
    private AnnotationSourceManager annotationSourceManager;

    public AnnotationSourceController() {
    }

    public Collection<AnnotationSourceView> getBioMartAnnSrcViews() {
        List<AnnotationSourceView> viewSources = new ArrayList<AnnotationSourceView>();
        for (AnnotationSourceType sourceType : AnnotationSourceType.values()) {
            final Collection<? extends AnnotationSource> currentAnnotationSourcesOfType = annotationSourceManager.getCurrentAnnotationSourcesOfType(sourceType.getClazz());
            for (AnnotationSource annSrc : currentAnnotationSourcesOfType) {
                ValidationReport validationReport = new ValidationReport(annSrc.findInvalidProperties());
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

    public String getAnnSrcString(String id, String type) {
        return annotationSourceManager.getAnnSrcString(id, AnnotationSourceType.getByName(type));
    }

    public void saveAnnSrc(String id, String type, String text) {
        annotationSourceManager.saveAnnSrc(id, AnnotationSourceType.getByName(type), text);
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
            return AnnotationSourceType.annSrcTypeOf(annSrc).getName();
        }
    }

    private static class ValidationReport {
        private Collection<String> missingProperties;

        ValidationReport(Collection<String> missingProperties) {
            this.missingProperties = missingProperties;
        }

        public String getSummary() {
            if (isValid()) {
                return "Valid";
            }
            StringBuilder sb = new StringBuilder();

            if (!missingProperties.isEmpty()) {
                sb.append("Invalid properties: ");
                sb.append(missingProperties);
            }
            return sb.toString();
        }

        public boolean isValid() {
            return CollectionUtils.isEmpty(missingProperties);
        }
    }
}
