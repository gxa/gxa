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

package ae3.dao;

import ae3.model.ExperimentalData;
import org.junit.After;
import org.junit.Test;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pashky
 */
public class NetCDFReaderTest {
    private long assayid = 0;

    private List<Assay> eMexp1586Assays(Experiment eMexp1586, ArrayDesign ad1) {
        List<Assay> result = new ArrayList<Assay>();
        for (int i = 1; i <= 6; i++)
            result.add(new Assay(assayid++, "A127-0" + i, eMexp1586, ad1));
        return result;
    }

    private List<Assay> eMexp1913Assays1(Experiment e, ArrayDesign ad) {
        List<Assay> result = new ArrayList<Assay>();
        for (String s : ("H_A_C99V50F 5-43,H_A_C99WT 5-23,H_A_mock 2-67_3. Negative control," +
                "H_A_C99I45F 4-25a,H_A_C99V50F 4-52,H_A_C99WT 4-2,H_A_C99I45F 4-13," +
                "H_A_mock 2-67_2. Negative control,H_A_C99V50F 5-59a," +
                "H_A_mock 2-67_1. Negative control,H_A_C99WT 5-12,H_A_C99I45F 4-17").split(",")) {
            result.add(new Assay(assayid++, "" + s, e, ad));
        }
        return result;
    }

    private List<Assay> eMexp1913Assays2(Experiment e, ArrayDesign ad) {
        List<Assay> result = new ArrayList<Assay>();
        for (String s : ("H_B_C99WT 5-12,H_B_C99V50F 5-43,H_B_mock 2-67_1. Negative control," +
                "H_B_mock 2-67_3. Negative control,H_B_C99V50F 5-59a,H_B_C99I45F 4-25a," +
                "H_B_mock 2-67_2. Negative control,H_B_C99I45F 4-13,H_B_C99I45F 4-17," +
                "H_B_C99WT 4-2,H_B_C99WT 5-23,H_B_C99V50F 4-52").split(",")) {
            result.add(new Assay(assayid++, "" + s, e, ad));
        }
        return result;
    }


    @Test
    public void testLoadExperiment() throws IOException, URISyntaxException {
        Experiment eMexp1586 = new Experiment(1036805754L, "E-MEXP-1586");
        ArrayDesign ad1 = new ArrayDesign();
        ad1.setArrayDesignID(160588088);
        ad1.setAccession("A-AFFY-44");
        eMexp1586.setAssays(eMexp1586Assays(eMexp1586, ad1));

        AtlasNetCDFDAO dao = new AtlasNetCDFDAO();
        dao.setAtlasDataRepo(getTestNCDir());
        // /atlas-web/target/test-classes/MEXP/1500/E-MEXP-1586/1036805754_160588088.nc
        ExperimentalData expData = new NetCDFReader().loadExperiment(dao, eMexp1586);
        assertNotNull(expData);
        assertEquals(1, expData.getArrayDesigns().size());
    }

    @Test
    public void testMultiArrayDesign() throws IOException, URISyntaxException {
        Experiment eMexp1913 = new Experiment(1036804993L, "E-MEXP-1913");
        ArrayDesign ad21 = new ArrayDesign();
        ad21.setArrayDesignID(153069949);
        ad21.setAccession("A-AFFY-33");
        ArrayDesign ad22 = new ArrayDesign();
        ad22.setArrayDesignID(165554923);
        ad22.setAccession("A-AFFY-44");
        List<Assay> assays = eMexp1913Assays1(eMexp1913, ad21);
        assays.addAll(eMexp1913Assays2(eMexp1913, ad22));
        eMexp1913.setAssays(assays);

        AtlasNetCDFDAO dao = new AtlasNetCDFDAO();
        dao.setAtlasDataRepo(getTestNCDir());
        // /atlas-web/target/test-classes/MEXP/1900/E-MEXP-1913/1036804668_153069949.nc
        // /atlas-web/target/test-classes/MEXP/1900/E-MEXP-1913/1036804668_165554923.nc
        ExperimentalData expData = new NetCDFReader().loadExperiment(dao, eMexp1913);
        assertNotNull(expData);
        assertEquals(2, expData.getArrayDesigns().size());
    }

    private static File getTestNCDir() throws URISyntaxException {
        // won't work for JARs, networks and stuff, but so far so good...
        return new File(NetCDFReaderTest.class.getClassLoader().getResource("").getPath());
    }

    @After
    public void cleanup() {
        ResourceWatchdogFilter.cleanup();
    }
}
