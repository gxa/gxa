package uk.ac.ebi.gxa.annotation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.AnnotationLoaderException;
import uk.ac.ebi.gxa.annotator.loader.annotationsrc.BioMartAnnotationSourceLoader;
import uk.ac.ebi.gxa.annotator.loader.biomart.AnnotationSourceConnectionFactory;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
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

    public AnnotationSourceController() {
    }

    public Collection<BioMartAnnotationSourceView> getBioMartAnnSrcViews() {
        List<BioMartAnnotationSourceView> viewSources = new ArrayList<BioMartAnnotationSourceView>();
        Collection<BioMartAnnotationSource> annotationSources = loader.getCurrentAnnotationSources();
        
        for (BioMartAnnotationSource annSrc : annotationSources) {
            try {
                BioMartConnection connection = AnnotationSourceConnectionFactory.createConnectionForAnnSrc(annSrc);
                ValidationReport validationReport = new ValidationReport();
                if (!connection.isValidDataSetName()) {
                    validationReport.setOrganismName(annSrc.getDatasetName());
                }
                validationReport.setMissingProperties(connection.validateAttributeNames(annSrc.getBioMartPropertyNames()));
                validationReport.addMissingProperties(connection.validateAttributeNames(annSrc.getBioMartArrayDesignNames()));

                BioMartAnnotationSourceView view = new BioMartAnnotationSourceView(annSrc, validationReport);

                viewSources.add(view);

            } catch (BioMartAccessException e) {
                throw LogUtil.createUnexpected("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }

        Collections.sort(viewSources, new Comparator<BioMartAnnotationSourceView>() {
            @Override
            public int compare(BioMartAnnotationSourceView o, BioMartAnnotationSourceView o1) {
                return o.getOrganismName().compareTo(o1.getOrganismName());
            }
        });
        return viewSources;
    }

    public String getAnnSrcString(String id) {
        return loader.getAnnSrcAsStringById(id);
    }

    public void saveAnnSrc(String text) {
        try {
            loader.saveAnnSrc(text);
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save AnnotationSource! ", e);
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
