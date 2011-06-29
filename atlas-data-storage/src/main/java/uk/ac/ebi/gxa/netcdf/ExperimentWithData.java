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

package uk.ac.ebi.gxa.netcdf;

import java.util.*;

import java.io.IOException;

import com.google.common.io.Closeables;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

public class ExperimentWithData {
    private final AtlasNetCDFDAO netCDFDao;
    private final Experiment experiment;

    private final Map<ArrayDesign,NetCDFProxy> proxies = new HashMap<ArrayDesign,NetCDFProxy>();

    ExperimentWithData(AtlasNetCDFDAO netCDFDao, Experiment experiment) {
        this.netCDFDao = netCDFDao;
        this.experiment = experiment;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    private NetCDFProxy getProxy(ArrayDesign arrayDesign) throws AtlasDataException {
        NetCDFProxy p = proxies.get(arrayDesign);
        if (p == null) {
            try {
                p = netCDFDao.getNetCDFDescriptor(experiment, arrayDesign).createProxy();
            } catch (IOException e) {
                throw new AtlasDataException(e);
            }
            proxies.put(arrayDesign, p);
        }
        return p;
    }

    public void closeAllDataSources() {
        for (NetCDFProxy p : proxies.values()) {
            Closeables.closeQuietly(p);
        }
        proxies.clear();
    }
}
