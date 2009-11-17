package uk.ac.ebi.gxa.R;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public enum RType {
    LOCAL("LOCAL"),
    REMOTE("REMOTE"),
    BIOCEP("BIOCEP");

    private String key;

    RType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
