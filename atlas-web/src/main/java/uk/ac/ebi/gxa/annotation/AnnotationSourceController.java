package uk.ac.ebi.gxa.annotation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.loader.annotationsrc.AnnotationLoaderException;
import uk.ac.ebi.gxa.loader.annotationsrc.BioMartAnnotationSourceLoader;
import uk.ac.ebi.gxa.loader.bioentity.BioMartAccessException;
import uk.ac.ebi.gxa.loader.bioentity.BioMartConnection;
import uk.ac.ebi.gxa.loader.bioentity.BioMartConnectionFactory;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 21/06/2011
 */
public class AnnotationSourceController {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private BioMartAnnotationSourceLoader loader;

    public AnnotationSourceController(AnnotationSourceDAO annotationSourceDAO) {
        loader = new BioMartAnnotationSourceLoader(annotationSourceDAO);
    }

    public Collection<BioMartAnnotationSourceView> getBioMartAnnSrcViews() {
        Set<BioMartAnnotationSourceView> viewSources = new HashSet<BioMartAnnotationSourceView>();
        Collection<BioMartAnnotationSource> annotationSources = loader.getCurrentAnnotationSources();
        for (BioMartAnnotationSource annSrc : annotationSources) {
            try {
                BioMartConnection connection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);
                ValidationReport validationReport = new ValidationReport();
                if (!connection.isValidDataSetName()) {
                    validationReport.setOrganismName(annSrc.getDatasetName());
                }
                validationReport.setMissingProperties(connection.validateAttributeNames(annSrc.getBioMartPropertyNames()));
                validationReport.addMissingProperties(connection.validateAttributeNames(annSrc.getBioMartArrayDesignNames()));

                BioMartAnnotationSourceView view = new BioMartAnnotationSourceView(annSrc, validationReport);

                viewSources.add(view);

            } catch (BioMartAccessException e) {
                log.error("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        return viewSources;
    }

    public String getAnnSrcString(String id) {
        return loader.getAnnSrcAsStringById(id);
    }

    public void saveAnnSrc(String text) {
        try {
            loader.saveAnnSrc(text);
        } catch (AnnotationLoaderException e) {
            //ToDo: add error processing
            log.error(e.getMessage(), e);
        }
    }

    public static class BioMartAnnotationSourceView {
        private BioMartAnnotationSource annSrc;
        private ValidationReport validationReport;

        public BioMartAnnotationSourceView(BioMartAnnotationSource annSrc, ValidationReport validationReport) {
            this.annSrc = annSrc;
            this.validationReport = validationReport;
        }

        public String getAnnSrcId() {
            return String.valueOf(annSrc.getAnnotationSrcId());
        }

        public String getOrganismName() {
            return annSrc.getOrganism().getName();
        }

        public String getBioentityTypes() {
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
            return annSrc.isApplied()?"yes":"no";
        }
    }

    private static class ValidationReport {
        private String organismName;
        private Collection<String> missingProperties;

        public ValidationReport() {
        }

        public void setOrganismName(String organismName) {
            this.organismName = organismName;
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

        public void addMissingProperties(Collection<String> missingProperties) {
            this.missingProperties.addAll(missingProperties);
        }

        public boolean isValid() {
            return StringUtils.isEmpty(organismName) && CollectionUtils.isEmpty(missingProperties);
        }
    }
}
