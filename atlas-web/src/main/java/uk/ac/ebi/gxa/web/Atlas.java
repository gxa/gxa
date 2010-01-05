package uk.ac.ebi.gxa.web;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 09-Nov-2009
 */
public enum Atlas {
    ATLAS_MAGETAB_LOADER("atlas.magetab.loader"),
    INDEX_BUILDER("atlas.index.builder"),
    NETCDF_GENERATOR("atlas.netcdf.generator"),
    ANALYTICS_GENERATOR("atlas.analytics.generator"),
    STATISTICS_SERVICE("atlas.statistics.service"),
    COMPUTE_SERVICE("atlas.compute.service"),
    DOWNLOAD_SERVICE("atlas.download.service"),
    PLOTTER("atlas.plotter"),
    GENES_CACHE("atlas.genelist.cache.servlet"),
    ATLAS_SOLR_DAO("atlas.dao"),       
    ATLAS_DAO("atlas.dao");

    private String key;

    Atlas(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
