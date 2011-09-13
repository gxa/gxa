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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.Floats;

import static uk.ac.ebi.gxa.utils.CollectionUtil.multiget;

public class DataUpdater {
    private static final Logger log = LoggerFactory.getLogger(DataUpdater.class);

    public void update(ExperimentWithData ewd) throws AtlasDataException {
        for (ArrayDesign arrayDesign : ewd.getExperiment().getArrayDesigns()) {
            log.info("Reading existing NetCDF");
            final NetCDFData data = readNetCDF(ewd, arrayDesign);

            log.info("Writing updated NetCDF");
            writeNetCDF(data, ewd, arrayDesign);

            log.info("Successfully updated the NetCDF");
        }
    }

    private NetCDFData readNetCDF(ExperimentWithData ewd, ArrayDesign arrayDesign) throws AtlasDataException {
        final NetCDFData data = new NetCDFData();
        final List<Integer> usedAssays = new LinkedList<Integer>();
        int index = 0;
        // WARNING: ewd.getAssays() list assays in the order they are listed in netcdf file
        for (Assay assay : ewd.getAssays(arrayDesign)) {
            data.addAssay(assay);
            usedAssays.add(index);
            ++index;
        }

        final String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
        data.setStorage(new DataMatrixStorage(data.getWidth(), deAccessions.length, 1));
        for (int i = 0; i < deAccessions.length; ++i) {
            final float[] values = ewd.getExpressionDataForDesignElementAtIndex(arrayDesign, i);
            data.addToStorage(deAccessions[i], multiget(Floats.asList(values), usedAssays).iterator());
        }
        return data;
    }

    private void writeNetCDF(NetCDFData data, ExperimentWithData ewd, ArrayDesign arrayDesign) throws AtlasDataException {
        final NetCDFDataCreator dataCreator = ewd.getDataCreator(arrayDesign);

        dataCreator.setAssayDataMap(data.getAssayDataMap());
        // TODO: restore statistics
        //netCdfCreator.setPvalDataMap(data.getPValDataMap());
        //netCdfCreator.setTstatDataMap(data.getTStatDataMap());

        dataCreator.createNetCdf();

        log.info("Successfully finished NetCDF for " + ewd.getExperiment().getAccession() + " and " + arrayDesign.getAccession());
    }
}
