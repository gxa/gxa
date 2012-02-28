package uk.ac.ebi.gxa.loader.steps;

import com.google.common.io.Files;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.service.GeneAnnotationFormatConverterService;
import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

/**
 * This class deals with:
 * 1. Conversion of .gtf file corresponding to HTS experiment's species into a format that can be handled by WiggleRequestHandler.
 * The converted file (with .anno extension) is then put into <ncdf_dir>/<experiment_accession>/annotations directory.
 * 2. Copying of accepted_hits.sorted.bam files from HTS processing directory into <ncdf_dir>/<experiment_accession>/assays directory.
 * <p/>
 * Both types of file are needed for the user to be able to view genes from HTS experiments on Ensembl genome browser.
 *
 * @author rpetry
 */
public class HTSAnnotationStep {
    private final static Logger log = LoggerFactory.getLogger(HTSAnnotationStep.class);

    private final static String ANNOTATIONS = "annotations";
    private final static String ASSAYS = "assays";
    private final static String ASSAY_PROCESSING_DIRS_PATTERN = "*RR*";
    private final static String TOPHAT_OUT = "tophat_out";
    private final static String BAM = "accepted_hits.sorted.bam";
    private final static String GTF_EXT_PATTERN = ".gtf$";
    private final static String ANNO_EXT_PATTERN = ".anno";

    public static String displayName() {
        return "Fetching HTS experiment's annotations and BAM files";
    }

    /**
     * Populates into experiment's ncdf directory a .gtf annotation file (needed by WiggleRequestHandler) corresponding to the first species associated with experiment
     * for which such .gtf file exists in experiment's HTS processing directory (N.B. assumptions below)
     * <p/>
     * Assumptions:
     * 1. Processed HTS experiment's sdrf file resides in <processing_dir>/<experiment_accession>/data/<experiment_accession>.sdrf.txt
     * 2. <processing_dir> contains a soft link, ANNOTATIONS, pointing to a directory containing .gtf files corresponding to all species needed to load HTS-experiments into Atlas.
     * The files inside <processing_dir>/<experiment_accession>/ANNOTATIONS are e.g. Caenorhabditis_elegans.WS220.65.gtf where 65 is the release number
     * of the Ensembl release against which experiment was processed. To avoid confusion only one Ensembl release's gtf file is allowed to be stored in <processing_dir>/ANNOTATIONS
     * per species (an error will be thrown otherwise)
     *
     * @param experiment
     * @param investigation
     * @param atlasDataDAO
     * @throws AtlasLoaderException if
     *                              1. <ncdf_dir>/<experiment_acc>/ANNOTATIONS dir does not exist and could not be created;
     *                              2. <processing_dir>/<experiment_accession>/ANNOTATIONS directory does not exist
     *                              3. More than one <processing_dir>/<experiment_accession>/ANNOTATIONS/.gtf file exists for a given experiment's species (it could be
     *                              that more than one Ensembl release's gtf files are stored under <processing_dir>/<experiment_accession>/ANNOTATIONS; this leads to error
     *                              as we need to be sure which Ensembl release we are loading annotations for)
     *                              4. There was an error copying a gtf file from <processing_dir>/<experiment_accession>/ANNOTATIONS/ to  <ncdf_dir>/<experiment_acc>/ANNOTATIONS
     *                              5. No .gtf files were found for any of the experiment's species
     */
    public void populateAnnotationsForSpecies(MAGETABInvestigation investigation, Experiment experiment, AtlasDataDAO atlasDataDAO) throws AtlasLoaderException {
        Collection<String> species = experiment.getSpecies();
        File sdrfFilePath = new File(investigation.SDRF.getLocation().getFile());
        File htsAnnotationsDir = new File(sdrfFilePath.getParentFile().getParentFile(), ANNOTATIONS);
        if (!htsAnnotationsDir.exists())
            throw new AtlasLoaderException("Cannot find " + htsAnnotationsDir.getAbsolutePath() + "folder to retrieve annotations from for experiment: " + experiment.getAccession());

        boolean found = false;
        for (String specie : species) {
            String encodedSpecies = StringUtil.upcaseFirst(specie.replaceAll(" ", "_"));
            FileFilter fileFilter = new WildcardFileFilter(encodedSpecies + "*" + ".gtf");
            File[] files = htsAnnotationsDir.listFiles(fileFilter);
            File experimentAnnotationsDir = null;
            try {
                experimentAnnotationsDir = new File(atlasDataDAO.getDataDirectory(experiment), ANNOTATIONS);
                if (!experimentAnnotationsDir.exists() && !experimentAnnotationsDir.mkdirs()) {
                    throw new AtlasLoaderException("Cannot create folder to insert annotations into for experiment: " + experiment.getAccession());
                }
                if (files.length == 1) {
                    GeneAnnotationFormatConverterService.transferAnnotation(files[0], new File(experimentAnnotationsDir, files[0].getName().replaceAll(GTF_EXT_PATTERN, ANNO_EXT_PATTERN)));
                    found = true;
                } else if (files.length > 0) {
                    throw new AtlasLoaderException("More than one file in Gene Annotation Format (.gtf) exists in " +
                            htsAnnotationsDir.getAbsolutePath() + " for experiment: " + experiment.getAccession() + " and species: " + specie);
                }
            } catch (IOException ioe) {
                throw new AtlasLoaderException("Error copying annotations from: " + files[0].getAbsolutePath() +
                        " to: " + experimentAnnotationsDir.getAbsolutePath() + " for experiment: " + experiment.getAccession(), ioe);
            }
            log.warn("Failed to find any files in Gene Annotation Format (.gtf) in " + htsAnnotationsDir.getAbsolutePath() + " for experiment: " + experiment.getAccession() + " and species: " + specie);
        }

        if (!found)
            throw new AtlasLoaderException("Failed to find any files in Gene Annotation Format (.gtf) in " + htsAnnotationsDir.getAbsolutePath() + " for experiment: " + experiment.getAccession());

    }


    /**
     * Populates into experiment's ncdf directory a BAM file (needed by WiggleRequestHandler) for each of experiment's assays.
     * <p/>
     * Assumptions:
     * 1. Processed HTS experiment's sdrf file resides in <processing_dir>/<experiment_accession>/data/<experiment_accession>.sdrf.txt
     * 2. <processing_dir>&#47;*RR*&#47;TOPHAT_OUT/BAM pattern pick out BAM files for all assays that were processed for experiment.
     *
     * @param experiment
     * @param investigation
     * @param atlasDataDAO
     * @throws AtlasLoaderException if
     *                              1. <processing_dir> does not exist
     *                              2. A given <processing_dir>/<assay_id> (matching pattern <processing_dir>&#47;*RR*&#47;) processing directory does not contain a BAM file
     *                              3. There was an error copying a BAM file from <processing_dir>&#47;*RR*&#47;TOPHAT_OUT/BAM to <ncdf_dir>/<experiment_acc>/ASSAYS
     *                              4. No <processing_dir>/<assay_id> matching pattern <processing_dir>&#47;*RR*&#47; was found.
     */
    public void populateBams(MAGETABInvestigation investigation, Experiment experiment, AtlasDataDAO atlasDataDAO) throws AtlasLoaderException {
        File sdrfFilePath = new File(investigation.SDRF.getLocation().getFile());
        File htsProcessingDir = sdrfFilePath.getParentFile().getParentFile();
        if (!htsProcessingDir.exists())
            throw new AtlasLoaderException("Cannot find " + htsProcessingDir.getAbsolutePath() + "folder to retrieve BAM files from for experiment: " + experiment.getAccession());

        FileFilter dirFilter = new WildcardFileFilter(ASSAY_PROCESSING_DIRS_PATTERN);
        File[] dirs = htsProcessingDir.listFiles(dirFilter);

        if (dirs.length == 0)
            throw new AtlasLoaderException("Failed to find any assay processing directories matching pattern: " +
                    ASSAY_PROCESSING_DIRS_PATTERN + " in: " + htsProcessingDir.getAbsolutePath() + " for experiment: " + experiment.getAccession());

        for (int i = 0; i < dirs.length; i++) {
            File bamFile = new File(new File(dirs[i], TOPHAT_OUT), BAM);
            if (!bamFile.exists())
                throw new AtlasLoaderException("BAM file: + " + bamFile.getAbsolutePath() + "does not exist for experiment: " + experiment.getAccession() + " and assay: " + dirs[i]);
            File toFile = null;
            try {
                File experimentAssaysDir = new File(atlasDataDAO.getDataDirectory(experiment), ASSAYS);
                toFile = new File(experimentAssaysDir, new File(dirs[i].getName(), bamFile.getName()).getPath());
                Files.createParentDirs(toFile);
                Files.copy(bamFile, toFile);
            } catch (IOException ioe) {
                throw new AtlasLoaderException("Error copying BAM file from: " + bamFile.getAbsolutePath() +
                        (toFile != null ? " to: " + toFile.getAbsolutePath() : "") + " for experiment: " + experiment.getAccession(), ioe);
            }
        }
    }
}
