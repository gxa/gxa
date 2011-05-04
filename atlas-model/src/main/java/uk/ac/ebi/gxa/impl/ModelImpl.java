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

package uk.ac.ebi.gxa.impl;

import java.util.*;
import java.io.File;

import uk.ac.ebi.gxa.*;
import uk.ac.ebi.gxa.utils.FileUtil;

//import uk.ac.ebi.gxa.dao.AtlasDAO;
//import ae3.dao.ExperimentSolrDAO;
//import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

public class ModelImpl implements Model {
    public interface DbAccessor {
        List<Experiment> getAllExperiments(ModelImpl atlasModel);

        List<Experiment> getExperimentsByArrayDesignAccession(ModelImpl atlasModel, String arrayDesignAccession);

        Experiment getExperimentByAccession(ModelImpl atlasModel, String accession);
    
        List<Asset> loadAssetsForExperiment(Experiment experiment);

        void deleteExperimentFromDatabase(String accession);
        void writeExperimentInternal(Experiment experiment);
    }

    public interface DataAccessor {
        File getDataDirectory(String experimentAccession);
    }

    private DbAccessor dbAccessor;
    private DataAccessor dataAccessor;
/*
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private ExperimentSolrDAO experimentSolrDAO;
*/

    public void setDbAccessor(DbAccessor dbAccessor) {
        this.dbAccessor = dbAccessor;
    }

    public void setDataAccessor(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

/*
    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }
*/

    public Experiment createExperiment(String accession) {
        return new ExperimentImpl(this, accession, 0);
    }

    public Experiment createExperiment(String accession, long id) {
        return new ExperimentImpl(this, accession, id);
    }

    public List<Experiment> getAllExperiments() {
        return dbAccessor.getAllExperiments(this);
    }

    public List<Experiment> getExperimentsByArrayDesignAccession(String arrayDesignAccession) {
        return dbAccessor.getExperimentsByArrayDesignAccession(this, arrayDesignAccession);
    }

    public Experiment getExperimentByAccession(String accession) {
        return dbAccessor.getExperimentByAccession(this, accession);
    }

    List<Asset> loadAssetsForExperiment(Experiment experiment) {
        return dbAccessor.loadAssetsForExperiment(experiment);
    }

    void deleteExperiment(String accession) {
        dbAccessor.deleteExperimentFromDatabase(accession);
        FileUtil.deleteDirectory(dataAccessor.getDataDirectory(accession));
    }

    void writeExperiment(Experiment experiment) {
        dbAccessor.writeExperimentInternal(experiment);
    }
}
