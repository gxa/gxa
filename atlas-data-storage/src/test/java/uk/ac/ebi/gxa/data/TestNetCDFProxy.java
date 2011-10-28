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
    private File netCDFfile;

    private DataProxy netCDF;

    @Override
    protected void setUp() throws Exception {
        netCDFfile = new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/E-MEXP-1586_A-AFFY-44.nc").toURI());
        netCDF = new NetCDFProxyV1(netCDFfile);
    }

    @Override
    protected void tearDown() throws Exception {
        netCDFfile = null;
        close(netCDF, false);
        netCDF = null;
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
        System.out.println("Experiment: " + netCDF.getExperimentAccession());
    }

    public void testGetArrayDesign() throws AtlasDataException {
        System.out.println("ArrayDesign: " + netCDF.getArrayDesignAccession());
    }

    /*
    public void testGetAssays() throws AtlasDataException {
        System.out.print("Assays: {");
        for (long assay : netCDF.getAssays()) {
            System.out.print(assay + ", ");
        }
        System.out.println("}");
    }

    public void testGetSamples() throws AtlasDataException {
        System.out.print("Samples: {");
        for (long sample : netCDF.getSamples()) {
            System.out.print(sample + ", ");
        }
        System.out.println("}");
    }
    */

    public void testGetFactors() throws AtlasDataException {
        System.out.print("EFs: {");
        for (String factor : netCDF.getFactors()) {
            System.out.print(factor + ", ");
        }
        System.out.println("}");
    }

    public void testGetFactorValues() throws AtlasDataException {
        for (String factor : netCDF.getFactors()) {
            System.out.print("EFVs for " + factor + " {");
            for (String efv : netCDF.getFactorValues(factor)) {
                System.out.print(efv + ", ");
            }
            System.out.println("}");
        }
    }

    public void testGetUniqueEVFs() throws AtlasDataException, StatisticsNotFoundException {
        final Set<KeyValuePair> uniques = new HashSet<KeyValuePair>();
        for (KeyValuePair uefv : netCDF.getUniqueEFVs()) {
            if (uniques.contains(uefv)) {
                fail("Found a duplicate: " + uefv);
            } else {
                uniques.add(uefv);
            }
        }
    }

    public void testGetCharacteristics() throws AtlasDataException {
        System.out.print("SCs: {");
        for (String characteristic : netCDF.getCharacteristics()) {
            System.out.print(characteristic + ", ");
        }
        System.out.println("}");
    }

    public void testGetCharacteristicValues() throws AtlasDataException {
        for (String characteristic : netCDF.getCharacteristics()) {
            System.out.print("SCVs: {");
            for (String scv : netCDF.getCharacteristicValues(characteristic)) {
                System.out.print(scv + ", ");
            }
            System.out.println("}");
        }
    }

}
