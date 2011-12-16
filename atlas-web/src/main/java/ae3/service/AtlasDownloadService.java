/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.service;

import ae3.dao.GeneSolrDAO;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.io.Closeables.closeQuietly;
import static java.util.Collections.synchronizedMap;

/**
 * Manages Atlas download requests for list results.
 *
 * @author iemam
 */
public class AtlasDownloadService {
    private AtlasStructuredQueryService atlasStructuredQueryService;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private GeneSolrDAO geneSolrDAO;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private AtomicInteger countDownloads = new AtomicInteger();

    private Map<String, Map<Integer, Download>> downloads = synchronizedMap(new HashMap<String, Map<Integer, Download>>());
    private ExecutorService downloadThreadPool;
    private AtlasProperties atlasProperties;

    public void setDownloadThreadPool(ExecutorService downloadThreadPool) {
        this.downloadThreadPool = downloadThreadPool;
    }

    public void setAtlasStructuredQueryService(AtlasStructuredQueryService atlasStructuredQueryService) {
        this.atlasStructuredQueryService = atlasStructuredQueryService;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    /**
     * Cleans up all session downloads and shuts down worker thread pool.
     */
    public void shutdown() {
        for (String sessionId : downloads.keySet()) {
            cleanupDownloads(sessionId);
        }
    }

    public Map<Integer, Download> getDownloads(String sessionID) {
        return downloads.get(sessionID);
    }

    /**
     * Starts a new download within the session, with query parameters.
     *
     * @param session session in which the download is kept
     * @param query   download query
     * @return download id, always positive; -1 in case of error.
     */
    public int requestDownload(HttpSession session, AtlasStructuredQuery query) {
        Map<Integer, Download> downloadList;

        if (downloads.containsKey(session.getId())) {
            downloadList = downloads.get(session.getId());
        } else {
            downloadList = synchronizedMap(new LinkedHashMap<Integer, Download>());
        }

        try {
            final String q = query.toString();
            for (Download d : downloadList.values()) {
                if (d.getQuery().equals(q)) {
                    log.info("There's already a download {} going on - ignoring request.", q);
                    return -1;
                }
            }
            final Download download = new Download(countDownloads.incrementAndGet(),
                    atlasStructuredQueryService, atlasStatisticsQueryService, geneSolrDAO,
                    query, atlasProperties.getDataRelease());

            downloadList.put(download.getId(), download);
            downloads.put(session.getId(), downloadList);

            downloadThreadPool.execute(download);

            return download.getId();
        } catch (IOException e) {
            log.error("Problem creating new download for {}, error {}", query, e.getMessage());
        }

        return -1;
    }

    public int getNumOfDownloads(String sessionID) {
        if (downloads.containsKey(sessionID))
            return downloads.get(sessionID).size();
        else
            return 0;
    }

    public void cleanupDownloads(String sessionId) {
        if (downloads.containsKey(sessionId)) {
            for (Download download : downloads.get(sessionId).values()) {
                File outputFile = download.getOutputFile();
                if (outputFile.exists() && outputFile.delete()) {
                    log.info("Deleted session expired list view download file {}", outputFile.getName());
                } else {
                    log.error("Couldn't delete list view download file {}", outputFile.getName());
                }
            }
        }
    }
}
