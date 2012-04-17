/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.service;

import ae3.service.structuredquery.AtlasStructuredQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.gxa.download.DownloadQueue;
import uk.ac.ebi.gxa.download.DownloadTask;
import uk.ac.ebi.gxa.download.ExperimentDownloadData;
import uk.ac.ebi.gxa.download.dsv.DsvDownloadTask;

import static com.google.common.base.Joiner.on;

/**
 * @author Olga Melnichuk
 */
@Service
public class DownloadDataService {

    private final ExperimentDownloadData expDownloadData;

    private final DownloadQueue downloadQueue;

    @Autowired
    public DownloadDataService(ExperimentDownloadData expDownloadData,
                               DownloadQueue downloadQueue) {
        this.expDownloadData = expDownloadData;
        this.downloadQueue = downloadQueue;
    }

    public String addExperimentAnalyticsTask(String expAcc, String adAcc, String cookie) {
        String token = newToken(expAcc, adAcc, cookie);
        addTask(new DsvDownloadTask(token, expDownloadData.expAnalyticsAsDsv(expAcc, adAcc)));
        return token;
    }

    public String addExperimentExpressionsTask(String expAcc, String cookie) {
        String token = newToken(expAcc, cookie);
        addTask(new DsvDownloadTask(token, expDownloadData.expExpressionsAsDsv(expAcc)));
        return token;
    }

    public String addExperimentDesignTask(String expAcc, String cookie) {
        String token = newToken(expAcc, cookie);
        //TODO
        return token;
    }

    public String addGeneSearchTask(AtlasStructuredQuery atlasQuery, String cookie) {
        String token = newToken(atlasQuery.toString(), cookie);
        //TODO
        return token;
    }

    private <T extends DownloadTask> void addTask(T task) {
       downloadQueue.add(task);
    }

    private String newToken(String... parts) {
        //TODO proper token generation needed
        return on("").join(parts);
    }

    public void cancelTask(String token) {
        //TODO should it return anything ?
        downloadQueue.cancelTask(token);
    }

}
