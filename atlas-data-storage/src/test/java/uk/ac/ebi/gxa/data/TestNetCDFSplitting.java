/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import com.google.common.io.Files;
import junit.framework.TestCase;
import ucar.ma2.ArrayChar;
import ucar.nc2.NetcdfFile;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.gxa.utils.ResourceUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertArrayEquals;
import static uk.ac.ebi.gxa.utils.FileUtil.getMD5;
import static uk.ac.ebi.microarray.atlas.model.Property.createProperty;

public class TestNetCDFSplitting extends TestCase {
    private File tempDirectory;
    private File baseExperimentDirectory;
    private File experimentDirectory;
    private AtlasDataDAO atlasDataDAO;

    @Override
    protected void setUp() throws Exception {
        atlasDataDAO = new AtlasDataDAO();
        File baseDirectory = ResourceUtil.getResourceRoot(getClass());
        tempDirectory = FileUtil.createTempDirectory("atlas-test");
        baseExperimentDirectory = new File(baseDirectory.getAbsolutePath() + "/MTAB/00/E-MTAB-25");
        experimentDirectory = new File(tempDirectory.getAbsolutePath() + "/MTAB/00/E-MTAB-25");
        if (!experimentDirectory.mkdirs())
            throw new IOException("Cannot create directories: " + experimentDirectory);
        for (String name : new String[]{"E-MTAB-25_A-AFFY-33.nc", "E-MTAB-25_A-AFFY-39.nc", "E-MTAB-25_A-AFFY-40.nc"}) {
            Files.copy(new File(baseExperimentDirectory, name), new File(experimentDirectory, name));
        }
        atlasDataDAO.setAtlasDataRepo(tempDirectory);
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtil.deleteDirectory(tempDirectory);
    }

    private String[] getStringArray(NetcdfFile file, String varName) throws IOException {
        final ArrayChar array = (ArrayChar) file.findVariable(varName).read();
        final ArrayChar.StringIterator iter = array.getStringIterator();
        final String[] result = new String[array.getShape()[0]];
        for (int i = 0; i < result.length && iter.hasNext(); ++i) {
            result[i] = iter.next();
        }
        return result;
    }

    private String[][] getStringArray2D(NetcdfFile file, String varName) throws IOException {
        final ArrayChar.D3 array = (ArrayChar.D3) file.findVariable(varName).read();
        final String[][] result = new String[array.getShape()[0]][array.getShape()[1]];
        for (int i = 0; i < result.length; ++i) {
            final ArrayChar.D2 array2D = (ArrayChar.D2) array.slice(0, i);
            final ArrayChar.StringIterator iter = array2D.getStringIterator();
            for (int j = 0; j < result[0].length; ++j) {
                result[i][j] = iter.next();
            }
        }
        return result;
    }

    public void testSplitting() throws IOException {
        try {
            final Experiment experiment = new Experiment(411512559L, "E-MTAB-25");

            final List<Assay> assays = newArrayList();
            final List<Sample> samples = newArrayList();

            long id = 0;

            for (String name : new String[]{"E-MTAB-25_A-AFFY-33.nc", "E-MTAB-25_A-AFFY-39.nc", "E-MTAB-25_A-AFFY-40.nc"}) {
                final NetcdfFile data = NetcdfFile.open(new File(baseExperimentDirectory, name).getAbsolutePath());
                final ArrayDesign ad =
                        new ArrayDesign(name.substring("E-MTAB-25_".length(), name.length() - 3));
                final String[] assayAccessions = getStringArray(data, "ASacc");
                final String[] sampleAccessions = getStringArray(data, "BSacc");
                final String[] efs = getStringArray(data, "EF");
                final String[][] efvs = getStringArray2D(data, "EFV");
                final String[] scs = getStringArray(data, "SC");
                final String[][] scvs = getStringArray2D(data, "SCV");
                data.close();
                for (int i = 0; i < assayAccessions.length; ++i) {
                    final Assay a = new Assay(assayAccessions[i]);
                    final Sample s = new Sample(sampleAccessions[i]);
                    a.setArrayDesign(ad);
                    for (int j = 0; j < efs.length; ++j) {
                        a.addProperty(new PropertyValue(
                                ++id, createProperty(efs[j]), efvs[j][i]
                        ));
                    }
                    for (int j = 0; j < scs.length; ++j) {
                        s.addProperty(new PropertyValue(
                                ++id, createProperty(scs[j]), scvs[j][i]
                        ));
                    }
                    assays.add(a);
                    samples.add(s);
                    s.addAssay(a);
                }
            }

            experiment.setAssays(assays);
            experiment.setSamples(samples);

            atlasDataDAO.createExperimentWithData(experiment).updateDataToNewestVersion();

            for (String name : new String[]{"E-MTAB-25_A-AFFY-33_data.nc", "E-MTAB-25_A-AFFY-39_data.nc", "E-MTAB-25_A-AFFY-40_data.nc"}) {
                assertArrayEquals(
                        getMD5(new File(new File(baseExperimentDirectory, "v2"), name)),
                        getMD5(new File(experimentDirectory, name))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
