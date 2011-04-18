package uk.ac.ebi.gxa.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * This factory class returns de-serialized bit index of gene expression data
 */
public class StatisticsStorageFactory {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private File atlasIndex;
    private String indexFileName;
    private StatisticsStorage statisticsStorage = null;

    public StatisticsStorageFactory(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndex = atlasIndexDir;
    }

    /**
     * @return StatisticsStorage containing indexes of all types in StatisticType enum
     * @throws IOException in case of I/O problems
     */
    public StatisticsStorage createStatisticsStorage() throws IOException {

        File indexFile = new File(atlasIndex, indexFileName);
        if (indexFile.exists()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(indexFile));
                readStatisticsStorage(ois);
                log.info("De-serialized " + indexFile.getAbsolutePath() + " successfully");
            } catch (ClassNotFoundException cnfe) {
                log.error("Failed to de-serialize: " + indexFile.getAbsolutePath());
            } finally {
                closeQuietly(ois);
            }
        }
        return statisticsStorage;
    }

    @SuppressWarnings("unchecked")
    private void readStatisticsStorage(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        statisticsStorage = (StatisticsStorage) ois.readObject();
    }
}
