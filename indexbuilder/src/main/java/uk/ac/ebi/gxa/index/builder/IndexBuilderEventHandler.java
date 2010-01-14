package uk.ac.ebi.gxa.index.builder;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

/**
 * Interface for index update call-backs
 * @author pashky
 */
public interface IndexBuilderEventHandler {
    void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event);
    void onIndexBuildStart(IndexBuilder builder);
}
