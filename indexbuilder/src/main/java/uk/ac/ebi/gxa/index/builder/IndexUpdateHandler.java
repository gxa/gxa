package uk.ac.ebi.gxa.index.builder;

/**
 * Interface for index update call-backs
 * @author pashky
 */
public interface IndexUpdateHandler {
    void onIndexUpdate(IndexBuilder builder);
}
