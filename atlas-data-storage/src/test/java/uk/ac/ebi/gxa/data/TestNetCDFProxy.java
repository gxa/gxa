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

import junit.framework.TestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.io.Closeables.close;

public class TestNetCDFProxy extends TestCase {
    private DataProxy netCDFV1;
    private DataProxy netCDFV2;

    @Override
    protected void setUp() throws Exception {
        netCDFV1 = new NetCDFProxyV1(new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/E-MEXP-1586_A-AFFY-44.nc").toURI()));
        netCDFV2 = new NetCDFProxyV2(new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/v2/E-MEXP-1586_A-AFFY-44_data.nc").toURI()),
                new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/v2/E-MEXP-1586_A-AFFY-44_statistics.nc").toURI()));
    }

    @Override
    protected void tearDown() throws Exception {
        close(netCDFV1, false);
        netCDFV1 = null;
        close(netCDFV2, false);
        netCDFV2 = null;
    }

    public void testisOutOfDate() throws Exception {
        try {
            new NetCDFProxyV1(new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/E-MEXP-1586_A-AFFY-44_old.nc").toURI())).isOutOfDate();
        } catch (AtlasDataException e) {
            return;
        }
        fail("AtlasDataException is not thrown");
    }

    public void testGetExperiment() throws AtlasDataException {
        System.out.println("Experiment: " + netCDFV1.getExperimentAccession());
        System.out.println("Experiment: " + netCDFV2.getExperimentAccession());
    }

    public void testGetArrayDesign() throws AtlasDataException {
        System.out.println("ArrayDesign: " + netCDFV1.getArrayDesignAccession());
        System.out.println("ArrayDesign: " + netCDFV1.getArrayDesignAccession());
    }

    public void testGetFactorValues() throws AtlasDataException {
        testGetFactorValues(netCDFV1);
        testGetFactorValues(netCDFV2);
    }

    public void testGetUniqueEVFs() throws AtlasDataException, StatisticsNotFoundException {
        testGetUniqueEVFs(netCDFV1);
        testGetUniqueEVFs(netCDFV2);
    }

    public void testGetCharacteristicValues() throws AtlasDataException {
        testGetCharacteristicValues(netCDFV1);
        testGetCharacteristicValues(netCDFV2);
    }

    private void testGetFactorValues(DataProxy netCDF) throws AtlasDataException {
        for (String factor : netCDF.getFactors()) {
            System.out.print("EFVs for " + factor + " {");
            for (String efv : netCDF.getFactorValues(factor)) {
                System.out.print(efv + ", ");
            }
            System.out.println("}");
        }
    }

    private void testGetUniqueEVFs(DataProxy netCDF) throws AtlasDataException, StatisticsNotFoundException {
        final Set<KeyValuePair> uniques = new HashSet<KeyValuePair>();
        for (KeyValuePair uefv : netCDF.getUniqueEFVs()) {
            if (uniques.contains(uefv)) {
                fail("Found a duplicate: " + uefv);
            } else {
                uniques.add(uefv);
            }
        }
    }

    private void testGetCharacteristicValues(DataProxy netCDF) throws AtlasDataException {
        for (String characteristic : netCDF.getCharacteristics()) {
            System.out.print("EFVs for " + characteristic + " {");
            for (String scv : netCDF.getCharacteristicValues(characteristic)) {
                System.out.print(scv + ", ");
            }
            System.out.println("}");
        }
    }

}
