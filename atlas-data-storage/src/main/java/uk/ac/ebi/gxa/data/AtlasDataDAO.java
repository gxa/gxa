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

package uk.ac.ebi.gxa.data;

import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Alexey Filippov
 * @author Rober Petryszak
 * @author Nikolay Pultsin
 */
public class AtlasDataDAO {
    // Location of the experiment data files
    private File atlasDataRepo;

    File getDataFile(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(
                getDataDirectory(experiment),
                experiment.getAccession() + "_" + arrayDesign.getAccession() + "_data.nc"
        );
    }

    File getStatisticsFile(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(
                getDataDirectory(experiment),
                experiment.getAccession() + "_" + arrayDesign.getAccession() + "_statistics.nc"
        );
    }

    File getV1File(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(
                getDataDirectory(experiment),
                experiment.getAccession() + "_" + arrayDesign.getAccession() + ".nc"
        );
    }

    DataProxy createDataProxy(Experiment experiment, ArrayDesign arrayDesign) throws AtlasDataException {
        DataProxy proxy;
        try {
            proxy = new NetCDFProxyV2(
                    getDataFile(experiment, arrayDesign),
                    getStatisticsFile(experiment, arrayDesign)
            );
        } catch (AtlasDataException e) {
            proxy = new NetCDFProxyV1(getV1File(experiment, arrayDesign));
        }
        return proxy;
    }

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    public ExperimentWithData createExperimentWithData(Experiment experiment) {
        return new ExperimentWithData(this, experiment);
    }

    public File getDataDirectory(Experiment experiment) {
        final String[] parts = experiment.getAccession().split("-");
        if (parts.length != 3 || !"E".equals(parts[0])) {
            throw LogUtil.createUnexpected("Invalid experiment accession: " + experiment.getAccession());
        }
        final String num = (parts[2].length() > 2) ?
                parts[2].substring(0, parts[2].length() - 2) + "00" : "00";
        return new File(new File(new File(atlasDataRepo, parts[1]), num), experiment.getAccession());
    }

    public void deleteExperiment(Experiment experiment) {
        FileUtil.deleteDirectory(getDataDirectory(experiment));
    }
}
