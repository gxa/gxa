package uk.ac.ebi.gxa.web;

/**
 * Session variables used in the Atlas web interface.
 *
 * @author Tony Burdett
 * @date 09-Nov-2009
 */
public enum Atlas {
    STATISTICS_SERVICE("atlas.statistics.service"),
    DOWNLOAD_SERVICE("atlas.download.service"),
    PLOTTER("atlas.plotter"),
    GENES_CACHE("atlas.genelist.cache.service"),
    ATLAS_SOLR_DAO("atlas.solr.dao"),       
    ATLAS_DAO("atlas.dao"),
    ADMIN_PAGE_NUMBER("atlas.page.number");

    private String key;

    Atlas(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
