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

package uk.ac.ebi.gxa.netcdf.reader;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.io.Closeables.close;

public class TestNetCDFProxy extends TestCase {
    private File netCDFfile;

    private NetCDFProxy netCDF;

    @Override
    protected void setUp() throws Exception {
        netCDFfile = new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/1036805754_160588088.nc").toURI());
        netCDF = new NetCDFProxy(netCDFfile);
    }

    @Override
    protected void tearDown() throws Exception {
        netCDFfile = null;
        close(netCDF, false);
        netCDF = null;
    }

    public void testisOutOfDate() throws IOException {
        assertTrue(netCDF.isOutOfDate());
    }

    public void testGetExperiment() throws IOException {
        System.out.println("Experiment: " + netCDF.getExperimentAccession());
    }

    public void testGetArrayDesign() throws IOException {
        System.out.println("ArrayDesign: " + netCDF.getArrayDesignAccession());
    }

    public void testGetAssays() throws IOException {
        System.out.print("Assays: {");
        for (long assay : netCDF.getAssays()) {
            System.out.print(assay + ", ");
        }
        System.out.println("}");
    }

    public void testGetSamples() throws IOException {
        System.out.print("Samples: {");
        for (long sample : netCDF.getSamples()) {
            System.out.print(sample + ", ");
        }
        System.out.println("}");
    }

    public void testGetFactors() throws IOException {
        System.out.print("EFs: {");
        for (String factor : netCDF.getFactors()) {
            System.out.print(factor + ", ");
        }
        System.out.println("}");
    }

    public void testGetFactorValues() throws IOException {
        for (String factor : netCDF.getFactors()) {
            System.out.print("EFVs for " + factor + " {");
            for (String efv : netCDF.getFactorValues(factor)) {
                System.out.print(efv + ", ");
            }
            System.out.println("}");
        }
    }

    public void testGetUniqueFactorValues() throws IOException {
        Set<String> uniques = new HashSet<String>();
        List<String> uefvs = netCDF.getUniqueFactorValues();
        for (String uefv : uefvs) {
            if (uniques.contains(uefv)) {
                fail("Found a duplicate: " + uefv);
            } else {
                uniques.add(uefv);
            }
        }
    }

    public void testGetUniqueValues() throws IOException {
        Set<String> uniques = new HashSet<String>();
        List<String> uVals = netCDF.getUniqueValues();
        List<String> uefvs = netCDF.getUniqueFactorValues();
        assert (uVals.size() >= uefvs.size());

        for (String uefv : uVals) {
            if (uniques.contains(uefv)) {
                fail("Found a duplicate: " + uefv);
            } else {
                uniques.add(uefv);
            }
        }
    }

    public void testGetCharacteristics() throws IOException {
        System.out.print("SCs: {");
        for (String characteristic : netCDF.getCharacteristics()) {
            System.out.print(characteristic + ", ");
        }
        System.out.println("}");
    }

    public void testGetCharacteristicValues() throws IOException {
        for (String characteristic : netCDF.getCharacteristics()) {
            System.out.print("SCVs: {");
            for (String scv : netCDF.getCharacteristicValues(characteristic)) {
                System.out.print(scv + ", ");
            }
            System.out.println("}");
        }
    }

}
