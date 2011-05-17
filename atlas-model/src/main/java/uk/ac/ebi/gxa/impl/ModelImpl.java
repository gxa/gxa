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

import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.util.List;

public class ModelImpl implements Model {
    public interface DbAccessor {
        List<Experiment> getAllExperiments();

        List<Experiment> getExperimentsByArrayDesignAccession(String arrayDesignAccession);

        Experiment getExperimentByAccession(String accession);

        void deleteExperimentFromDatabase(String accession);

        void writeExperimentInternal(Experiment experiment);
    }

    public interface DataAccessor {
        File getDataDirectory(String experimentAccession);
    }

    private DbAccessor dbAccessor;
    private DataAccessor dataAccessor;

    public void setDbAccessor(DbAccessor dbAccessor) {
        this.dbAccessor = dbAccessor;
    }

    public void setDataAccessor(DataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

    public Experiment createExperiment(String accession) {
        // TODO: 4geometer: why not <code>Long id = null</code>?
        return new Experiment(null, accession);
    }

    public Experiment createExperiment(long id, String accession) {
        return new Experiment(id, accession);
    }

    public List<Experiment> getAllExperiments() {
        return dbAccessor.getAllExperiments();
    }

    public List<Experiment> getExperimentsByArrayDesignAccession(String arrayDesignAccession) {
        return dbAccessor.getExperimentsByArrayDesignAccession(arrayDesignAccession);
    }

    public Experiment getExperimentByAccession(String accession) {
        return dbAccessor.getExperimentByAccession(accession);
    }

    @Override
    public void delete(Experiment experiment) {
        deleteExperiment(experiment.getAccession());
    }

    @Override
    public void save(Experiment experiment) {
        writeExperiment(experiment);
    }

    public void deleteExperiment(String accession) {
        dbAccessor.deleteExperimentFromDatabase(accession);
        FileUtil.deleteDirectory(dataAccessor.getDataDirectory(accession));
    }

    public void writeExperiment(Experiment experiment) {
        dbAccessor.writeExperimentInternal(experiment);
    }
}
