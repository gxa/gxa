package uk.ac.ebi.gxa.annotation;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.annotationsrc.TopAnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.annotationsrc.UpdatedAnnotationSource;
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
    protected TopAnnotationSourceManager manager;

    public AnnotationSourceController() {
    }

    public Collection<AnnotationSourceView> getAnnSrcViews() {
        List<AnnotationSourceView> viewSources = new ArrayList<AnnotationSourceView>();

        Collection<UpdatedAnnotationSource> annotationSources = manager.getAllAnnotationSources();
        for (UpdatedAnnotationSource updatedAnnotationSource : annotationSources) {
            ValidationReport validationReport = null;

            if (updatedAnnotationSource.isUpdated()) {
                Collection<String> invalidPropertyNames = manager.validateProperties(updatedAnnotationSource.getAnnotationSource());
                validationReport = new ValidationReport(invalidPropertyNames);
            }
            AnnotationSourceView view = new AnnotationSourceView(updatedAnnotationSource.getAnnotationSource(), validationReport);
            viewSources.add(view);
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
        return manager.getAnnSrcString(id, type);
    }

    public void saveAnnSrc(String id, String type, String text) {
        manager.saveAnnSrc(id, text, type);
    }

    public String validate(String annSrcId, String type) {
        ValidationReport report = new ValidationReport(manager.validateProperties(annSrcId, type));
        return report.getSummary();
    }

    public class AnnotationSourceView {
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
            if (validationReport != null) {
                return validationReport.getSummary();
            }
            return "";
        }

        public String getApplied() {
            return annSrc.isApplied() ? "yes" : "no";
        }

        public String getType() {
            return AnnotationSourceType.annSrcTypeOf(annSrc).getName();
        }

        public String areMappingsApplied() {
            return manager.areMappingsApplied(annSrc) ? "yes" : "no";
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

            if (!CollectionUtils.isEmpty(missingProperties)) {
                sb.append("Invalid properties: ");
                sb.append(missingProperties);
            }
            return sb.toString();
        }

        public boolean isValid() {
            return missingProperties != null && missingProperties.isEmpty();
        }
    }
}
