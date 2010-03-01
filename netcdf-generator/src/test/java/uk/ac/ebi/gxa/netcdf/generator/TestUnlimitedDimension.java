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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.netcdf.generator;

import junit.framework.TestCase;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 05-Jan-2010
 */
public class TestUnlimitedDimension extends TestCase {
    public void testUnlimitedDimensionExample() {
        try {
            NetcdfFileWriteable netCDF = NetcdfFileWriteable.createNew("unlimited-test.nc");

            // define dimensions, including unlimited
            Dimension xDim = netCDF.addUnlimitedDimension("x");
            Dimension yDim = netCDF.addDimension("y", 2);

            netCDF.addVariable("xy", DataType.INT, new Dimension[]{xDim, yDim});

            // create the file
            netCDF.create();

            ArrayInt data = new ArrayInt.D2(10, 2);

            int[] origin = new int[]{0,0};

            for (int x = 0; x < 10; x++) {
                // make up some data for this record, using different ways to fill the data arrays.
                System.out.println("Writing " + x*12 + " to " + x + ",0 and 1");
                data.setInt(data.getIndex().set(x,0), x * 12);
                data.setInt(data.getIndex().set(x,1), x * 12);
//                yData.setInt(yData.getIndex().set(1), x*12+1);

                origin[0] = x;
                netCDF.write("xy", origin, data);
            }

            // all done
            netCDF.close();
        }
        catch (InvalidRangeException e) {
            e.printStackTrace();
            fail();
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
