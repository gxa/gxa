package uk.ac.ebi.gxa.annotation;

import com.google.common.base.Function;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.annotationsrc.TopAnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.annotationsrc.UpdatedAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Collections2.transform;

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

    public Software getSoftware(long softwareId) throws AnnotationSourceControllerException {
        try {
            return manager.getSoftware(softwareId);
        } catch (RecordNotFoundException e) {
            throw new AnnotationSourceControllerException("Can't find annotation software to show. See logs for details.", e);
        }
    }

    public Collection<AnnotationSourceRow> getAnnotationSourcesForSoftware(Software software) {
        return transform(manager.getAnnotationSourcesBySoftware(software), new Function<AnnotationSource, AnnotationSourceRow>() {
            @Override
            public AnnotationSourceRow apply(@Nullable AnnotationSource input) {
                return new AnnotationSourceRow(input);
            }
        });
    }

    public Collection<AnnotationSourceRow> getAnnSrcViews() {
        List<AnnotationSourceRow> viewSources = new ArrayList<AnnotationSourceRow>();

        Collection<UpdatedAnnotationSource> annotationSources = manager.getAllAnnotationSources();
        for (UpdatedAnnotationSource updatedAnnotationSource : annotationSources) {
            ValidationReportBuilder validationReport = new ValidationReportBuilder();

            if (updatedAnnotationSource.isUpdated()) {
                manager.validateProperties(updatedAnnotationSource.getAnnotationSource(), validationReport);
            }
            AnnotationSourceRow view = new ValidatedAnnotationSourceRow(updatedAnnotationSource.getAnnotationSource(), validationReport);
            viewSources.add(view);
        }
        Collections.sort(viewSources, new Comparator<AnnotationSourceRow>() {
            @Override
            public int compare(AnnotationSourceRow o, AnnotationSourceRow o1) {
                return o.getOrganismName().compareTo(o1.getOrganismName());
            }
        });
        return viewSources;
    }

    public AnnotationSourceView getEditableAnnSrc(long id, String typeName) throws AnnotationSourceControllerException {
        AnnotationSourceType type = getType(typeName);
        try {
            return new AnnotationSourceView(
                    id,
                    typeName,
                    manager.getAnnSrcString(id, type)
            );
        } catch (RecordNotFoundException e) {
            throw new AnnotationSourceControllerException("Can't find annotation source to edit. See logs for details.", e);
        }
    }
    
    public String saveAnnSrc(long id, String typeName, String text) throws AnnotationSourceControllerException {
        final ValidationReportBuilder validationReportBuilder = manager.saveAnnSrc(id, text, getType(typeName));
        if (!validationReportBuilder.isEmpty()) {
            //add log msg
            return validationReportBuilder.getSummary("Error(s) ", "\n");
        } else {
            return "";
        }
    }

    public String validate(long annSrcId, String typeName) throws AnnotationSourceControllerException {
        ValidationReportBuilder report = new ValidationReportBuilder();
        manager.validateProperties(annSrcId, getType(typeName), report);
        return report.getSummary("Valid", "Invalid properties ", ", ");
    }

    private AnnotationSourceType getType(String typeName) throws AnnotationSourceControllerException {
        try {
            return AnnotationSourceType.getByName(typeName);
        } catch(IllegalArgumentException e) {
            throw new AnnotationSourceControllerException("Invalid annotation source type parameter: '" + typeName + "'", e);
        }
    }

    public static class AnnotationSourceView {
        private long id;
        private String typeName;
        private String body;

        public AnnotationSourceView(long id, String typeName, String annSrcAsString) {
            this.id = id;
            this.typeName = typeName;
            this.body = annSrcAsString;
        }

        public long getId() {
            return id;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getBody() {
            return body;
        }
    }
    
    public static class AnnotationSourceRow {
        private AnnotationSource annSrc;

        public AnnotationSourceRow(AnnotationSource annSrc) {
            this.annSrc = annSrc;
        }

        public String getId() {
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

        public String getAnnotationsLoaded() {
            return annSrc.isAnnotationsApplied() ? "yes" : "no";
        }

        public String getMappingsLoaded() {
            if (annSrc.getExternalArrayDesigns().isEmpty()) {
                return "N/A";
            }
            return annSrc.isMappingsApplied() ? "yes" : "no";
        }

        public String getType() {
            return AnnotationSourceType.annSrcTypeOf(annSrc).getName();
        }

        public boolean isObsolete() {
            return annSrc.isObsolete();
        }
    }

    public static class ValidatedAnnotationSourceRow extends AnnotationSourceRow {

        private ValidationReportBuilder validationReport;

        public ValidatedAnnotationSourceRow(AnnotationSource annSrc, ValidationReportBuilder validationReport) {
            super(annSrc);
            this.validationReport = validationReport;
        }

        public String getValidationMessage() {
            return validationReport.getSummary("Invalid properties: ", ", ");
        }
    }
}
