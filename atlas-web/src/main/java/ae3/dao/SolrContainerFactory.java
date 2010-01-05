package ae3.dao;

import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Spring-friendly factory class, allowing to create CoreContainer's configured by index path represented as java.io.File
 * @author pashky
 */
public class SolrContainerFactory {
    private File atlasIndex;

    public SolrContainerFactory() {

    }

    public File getAtlasIndex() {
        return atlasIndex;
    }

    public void setAtlasIndex(File indexLocation) {
        this.atlasIndex = indexLocation;
    }

    public CoreContainer createContainer() throws IOException, ParserConfigurationException, SAXException {
        return new CoreContainer(atlasIndex.getAbsolutePath(), new File(atlasIndex, "solr.xml"));
    }
}
