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
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.CharArrayWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 21/06/2011
 */
public class AnnotationSourceController {
        // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private AnnotationSourceDAO annSrcDAO;
    private BioMartAnnotationSourceLoader loader;

    public AnnotationSourceController(AnnotationSourceDAO annotationSourceDAO) {
        this.annSrcDAO = annotationSourceDAO;
        loader = new BioMartAnnotationSourceLoader(annotationSourceDAO);
    }

    public List<BioMartAnnotationSourceView> getBioMartAnnSrcViews() {
        annSrcDAO.startSession();
        List<BioMartAnnotationSourceView> viewSources = new ArrayList<BioMartAnnotationSourceView>();
        Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getCurrentAnnotationSourcesOfType(BioMartAnnotationSource.class);
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            try {
                BioMartConnection connection  = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);
                String newVersion = connection.getOnlineMartVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    viewSources.add(new BioMartAnnotationSourceView(annSrc, annSrc.getSoftware().getDisplayName()));
                } else {
                    //check if AnnotationSource exists for new version
//                    softwareDAO.startSession();
                    Software newSoftware = annSrcDAO.findOrCreateSoftware(annSrc.getSoftware().getName(), newVersion);
//                    softwareDAO.finishSession();
//                     Software newSoftware = new Software(annSrc.getSoftware().getName(), newVersion);
                    BioMartAnnotationSource newAnnSrc = annSrcDAO.findAnnotationSource(newSoftware, annSrc.getOrganism(), BioMartAnnotationSource.class);
                    //create and Save new AnnotationSource
                    if (newAnnSrc == null) {
                        newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                        annSrcDAO.save(newAnnSrc);
                    }
                    ValidationReport validationReport = new ValidationReport();
                    if (!connection.isValidDataSetName()) {
                        validationReport.setOrganismName(newAnnSrc.getDatasetName());
                    }
                    validationReport.setMissingProperties(connection.validateAttributeNames(newAnnSrc.getBioMartPropertyNames()));

                    BioMartAnnotationSourceView annSrcView = new BioMartAnnotationSourceView(newAnnSrc, annSrc.getSoftware().getDisplayName());
                    annSrcView.setValidationReport(validationReport);
                    viewSources.add(annSrcView);
                }

            } catch (BioMartAccessException e) {
                log.error("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        annSrcDAO.finishSession();
        return viewSources;
    }

    public String getAnnSrcString(String id)  {
        Long aLong = Long.parseLong(id);
        BioMartAnnotationSource annotationSource = (BioMartAnnotationSource) annSrcDAO.getById(aLong);
        Writer writer = new CharArrayWriter();
        loader.writeSource(annotationSource, writer);
        return writer.toString();
    }

    public void saveAnnSrc(String text) {
         Reader reader = new StringReader(text);
        try {
            annSrcDAO.startSession();
            BioMartAnnotationSource annotationSource = loader.readSource(reader);

            annSrcDAO.save(annotationSource);
            annSrcDAO.finishSession();
        } catch (AnnotationLoaderException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static class BioMartAnnotationSourceView {
        private BioMartAnnotationSource annSrc;
        private String currentName;
        private ValidationReport validationReport = new ValidationReport();
        private boolean applied = false;

        public BioMartAnnotationSourceView(BioMartAnnotationSource annSrc, String currentName) {
            this.annSrc = annSrc;
            this.currentName = currentName;
        }

        public BioMartAnnotationSource getAnnSrc() {
            return annSrc;
        }

        public String getCurrentName() {
            return currentName;
        }

        public ValidationReport getValidationReport() {
            return validationReport;
        }

        public void setValidationReport(ValidationReport validationReport) {
            this.validationReport = validationReport;
        }

        public boolean isApplied() {
            return applied;
        }

        public void setApplied(boolean applied) {
            this.applied = applied;
        }
    }

    public static class ValidationReport {
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
            sb.append(organismName + " ");
            sb.append(missingProperties);
            return sb.toString();
        }

        public Collection<String> getMissingProperties() {
            return missingProperties;
        }

        public void setMissingProperties(Collection<String> missingProperties) {
            this.missingProperties = missingProperties;
        }

        public boolean isValid() {
            return StringUtils.isEmpty(organismName) && CollectionUtils.isEmpty(missingProperties);
        }
    }
}
