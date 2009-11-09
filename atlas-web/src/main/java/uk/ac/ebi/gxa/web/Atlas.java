package uk.ac.ebi.gxa.web;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 09-Nov-2009
 */
public enum Atlas {
    SEARCH_SERVICE("atlas.search.service"),
    STATISTICS_SERVICE("atlas.statistics.service"),
    COMPUTE_SERVICE("atlas.compute.service"),
    DOWNLOAD_SERVICE("atlas.download.service"),
    PLOTTER("atlas.plotter"),
    GENES_CACHE("atlas.genelist.cache.servlet");

    private String key;

    Atlas(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
