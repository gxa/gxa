package uk.ac.ebi.gxa.netcdf.migrator;

/**
 * @author pashky
 */
public interface AtlasNetCDFMigrator {
    void generateNetCDFForAllExperiments(boolean missingOnly);
    void generateNetCDFForExperiment(String experimentAccession, boolean missingOnly);
}
