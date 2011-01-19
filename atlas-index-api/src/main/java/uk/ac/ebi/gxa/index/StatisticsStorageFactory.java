package uk.ac.ebi.gxa.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 2:58:59 PM
 * This factory class returns de-serialized bit index of gene expression data
 */
public class StatisticsStorageFactory {

    final private Logger log = LoggerFactory.getLogger(getClass());

    private File atlasIndex;
    private String indexFileName;
    private StatisticsStorage<Long> statisticsStorage = null;

    public StatisticsStorageFactory(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public File getAtlasIndex() {
        return atlasIndex;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndex = atlasIndexDir;
    }

    /**
     * @return StatisticsStorage containing indexes of all types in StatisticType enum
     * @throws IOException
     */
    public StatisticsStorage createStatisticsStorage() throws IOException {

        File indexFile = new File(atlasIndex, indexFileName);
        if (indexFile.exists()) {
            ObjectInputStream obj = new ObjectInputStream(new FileInputStream(indexFile));
            try {
                statisticsStorage = (StatisticsStorage<Long>) obj.readObject();
                log.info("De-serialized " + indexFile.getAbsolutePath() + " successfully");
            } catch (ClassNotFoundException cnfe) {
                log.error("Failed to de-serialize: " + indexFile.getAbsolutePath());
            } finally {
                obj.close();
            }
        }
        return statisticsStorage;
    }

}
