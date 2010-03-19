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
package uk.ac.ebi.gxa.unloader;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

/**
 * Atlas unloader implementation
 * @author pashky
 */
public class DefaultAtlasUnloader implements AtlasUnloader {
    private static final Logger log = LoggerFactory.getLogger(DefaultAtlasUnloader.class);

    private AtlasDAO atlasDao;
    private File netcdfPath;

    public AtlasDAO getAtlasDao() {
        return atlasDao;
    }

    public void setAtlasDao(AtlasDAO atlasDao) {
        this.atlasDao = atlasDao;
    }

    public File getNetcdfPath() {
        return netcdfPath;
    }

    public void setNetcdfPath(File netcdfPath) {
        this.netcdfPath = netcdfPath;
    }

    public void unloadExperiment(String accession) throws AtlasUnloaderException {
        try {
            Experiment experiment = getAtlasDao().getExperimentByAccession(accession);
            if(experiment == null)
                throw new AtlasUnloaderException("Can't find experiment to unload");

            List<ArrayDesign> arrayDesigns = getAtlasDao().getArrayDesignByExperimentAccession(accession);

            getAtlasDao().deleteExperiment(accession);

            for(ArrayDesign ad : arrayDesigns) {
                File netCdf = new File(netcdfPath, experiment.getExperimentID() + "_" + ad.getArrayDesignID() + ".nc");
                if(netCdf.exists()) {
                    if(!netCdf.delete())
                        log.warn("Can't delete NetCDF: " + netCdf);
                }
            }
        } catch(DataAccessException e) {
            throw new AtlasUnloaderException("DB error while unloading experiment " + accession, e);
        }
    }

    public void unloadArrayDesign(String accession) throws AtlasUnloaderException {
        log.error("Attempt to unload array design " + accession);
        throw new AtlasUnloaderException("Not implemented");
    }
}
