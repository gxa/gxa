package uk.ac.ebi.gxa.jmx;

/**
 * @author pashky
 */
public interface AtlasManagerMBean {
    void rebuildIndex(String index);

    void rebuildAllIndexes();

    String getIndexPath();

    String getNetCDFPath();

    String getDataSourceURL();

    String getVersion();

    String getEFO();

    String getAtlasProperty(String property);

    void setAtlasProperty(String property, String newValue);
}
