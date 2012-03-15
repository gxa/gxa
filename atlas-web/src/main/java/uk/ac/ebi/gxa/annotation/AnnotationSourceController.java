package uk.ac.ebi.gxa.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.annotationsrc.TopAnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.annotationsrc.UpdatedAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

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

    public List<Software> getAllSoftware() {
        final List<Software> softwares = manager.getAllSoftware();
        Collections.sort(softwares, new Comparator<Software>() {
            @Override
            public int compare(Software o, Software o1) {

                int answer = o.getName().compareToIgnoreCase(o1.getName());
                if (answer != 0) return answer;

                try {
                    final int v = Integer.parseInt(o.getVersion());
                    final int v1 = Integer.parseInt(o1.getVersion());
                    return v1 - v;
                } catch (NumberFormatException e) {
                    return o.getFullName().compareTo(o1.getFullName());
                }
            }
        });
        return softwares;
    }

    public Collection<AnnotationSource> getAnnotationSourcesForSoftware(String softwareId) {
        return manager.getAnnotationSourcesBySoftwareId(softwareId);
    }

    public Collection<AnnotationSourceView> getAnnSrcViews() {
        List<AnnotationSourceView> viewSources = new ArrayList<AnnotationSourceView>();

        Collection<UpdatedAnnotationSource> annotationSources = manager.getAllAnnotationSources();
        for (UpdatedAnnotationSource updatedAnnotationSource : annotationSources) {
            ValidationReportBuilder validationReport = new ValidationReportBuilder();

            if (updatedAnnotationSource.isUpdated()) {
                manager.validateProperties(updatedAnnotationSource.getAnnotationSource(), validationReport);
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

    public String saveAnnSrc(String id, String type, String text) {
        final ValidationReportBuilder validationReportBuilder = manager.saveAnnSrc(id, text, type);
        if (!validationReportBuilder.isEmpty()) {
            //add log msg
            return validationReportBuilder.getSummary("Error(s) ", "\n");
        } else {
            return "";
        }
    }

    public String validate(String annSrcId, String type) {
        ValidationReportBuilder report = new ValidationReportBuilder();
        manager.validateProperties(annSrcId, type, report);
        return report.getSummary("Valid", "Invalid properties ", ", ");
    }

    public class AnnotationSourceView {
        private AnnotationSource annSrc;
        private ValidationReportBuilder validationReport;

        public AnnotationSourceView(AnnotationSource annSrc, ValidationReportBuilder validationReport) {
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
            return validationReport.getSummary("Invalid properties: ", ", ");
        }

        public String getApplied() {
            return annSrc.isAnnotationsApplied() ? "yes" : "no";
        }

        public String getMappingsApplied() {
            if (annSrc.getExternalArrayDesigns().isEmpty()) {
                return "N/A";
            }
            return annSrc.isMappingsApplied() ? "yes" : "no";
        }

        public String getType() {
            return AnnotationSourceType.annSrcTypeOf(annSrc).getName();
        }
    }
}
