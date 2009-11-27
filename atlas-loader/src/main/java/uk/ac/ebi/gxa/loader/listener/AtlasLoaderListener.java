package uk.ac.ebi.gxa.loader.listener;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public interface AtlasLoaderListener {
    /**
     * Indicates that loading of a resource completed successfully
     *
     * @param event the event representing this build success event
     */
    public void loadSuccess(AtlasLoaderEvent event);

    /**
     * Indicates that loading of a resource failed
     *
     * @param event the event representing this build failure
     */
    public void loadError(AtlasLoaderEvent event);
}
