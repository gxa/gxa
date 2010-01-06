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
