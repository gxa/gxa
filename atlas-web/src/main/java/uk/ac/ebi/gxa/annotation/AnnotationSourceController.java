package uk.ac.ebi.gxa.annotation;

import com.google.common.base.Function;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.annotationsrc.CompositeAnnotationSourceManager;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;

/**
 * User: nsklyar
 * Date: 21/06/2011
 */
public class AnnotationSourceController {

    @Autowired
    protected CompositeAnnotationSourceManager manager;

    @Autowired
    private AtlasProperties atlasProperties;

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

    public Software activateSoftware(long softwareId) throws AnnotationSourceControllerException {
        try {
            return manager.activateSoftware(softwareId);
        } catch (RecordNotFoundException e) {
            throw new AnnotationSourceControllerException("Can't find annotation software to show. See logs for details.", e);
        }
    }

    public Collection<AnnotationSourceRow> getAnnotationSourcesForSoftware(Software software) {
        final List<AnnotationSourceRow> annotationSourceRows = newArrayList(transform(manager.getAnnotationSourcesBySoftware(software), new Function<AnnotationSource, AnnotationSourceRow>() {
            @Override
            public AnnotationSourceRow apply(@Nullable AnnotationSource input) {
                return new AnnotationSourceRow(input);
            }
        }));
        Collections.sort(annotationSourceRows, new Comparator<AnnotationSourceRow>() {
            @Override
            public int compare(AnnotationSourceRow o, AnnotationSourceRow o1) {
                return o.getOrganismName().compareTo(o1.getOrganismName());
            }
        });
        return annotationSourceRows;
    }

    public AnnotationSourceView getEditableAnnSrc(long id, String typeName) throws AnnotationSourceControllerException {
        AnnotationSourceType type = getType(typeName);
        try {
            AnnotationSource annotSource = manager.getAnnSrc(id, type);
            return new AnnotationSourceView(annotSource, type, manager.getAnnSrcString(id, type));
        } catch (RecordNotFoundException e) {
            throw new AnnotationSourceControllerException("Can't find annotation source to edit. See logs for details.", e);
        }
    }
    
    public ValidationReportBuilder validateAndSaveAnnSrc(long id, String typeName, String text) throws AnnotationSourceControllerException {
        return manager.validateAndSaveAnnSrc(id, text, getType(typeName));
    }

    public ValidationReportBuilder validate(long id, String typeName) throws AnnotationSourceControllerException {
        AnnotationSourceType type = getType(typeName);
        try {
            AnnotationSource annSrc = manager.getAnnSrc(id, type);
            ValidationReportBuilder errors = new ValidationReportBuilder();
            manager.validateProperties(annSrc, errors);
            return errors;
        } catch (RecordNotFoundException e) {
            throw new AnnotationSourceControllerException("Can't find annotation source to validate. See logs for details.", e);
        }
    }

    public ValidationReportBuilder updateLatestAnnotationSourcesFromMaster() {
        return manager.updateLatestAnnotationSourcesFromUrl(atlasProperties.getAnnotationSourceList());
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
        private boolean obsolete;

        public AnnotationSourceView(AnnotationSource annotSource, AnnotationSourceType type, String annSrcAsString) {
            this.typeName = type.getName();
            this.body = annSrcAsString;
            this.id = annotSource.getAnnotationSrcId();
            this.obsolete = annotSource.isObsolete();
        }

        public long getId() {
            return id;
        }

        //The following methods are used in admin.js

        public String getTypeName() {
            return typeName;
        }

        public String getBody() {
            return body;
        }

        public boolean isObsolete() {
            return obsolete;
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

        //The following methods are used in admin.js

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
}
