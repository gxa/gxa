package uk.ac.ebi.gxa.netcdf.migrator;

/**
 * @author pashky
 */
public interface AtlasNetCDFMigrator {
    void generateNetCDFForAllExperiments();
    void generateNetCDFForExperiment(String experimentAccession);
}
