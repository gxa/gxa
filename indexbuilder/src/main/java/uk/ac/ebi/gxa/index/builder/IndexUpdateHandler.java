package uk.ac.ebi.gxa.index.builder;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

/**
 * Interface for index update call-backs
 * @author pashky
 */
public interface IndexUpdateHandler {
    void onIndexUpdate(IndexBuilder builder, IndexBuilderEvent event);
}
