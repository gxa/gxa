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

package uk.ac.ebi.gxa.netcdf.reader;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;

/**
 * This is temporary workaround for what we consider as missing values in the netCDF files. Until there is no single substitution
 * for missing value in R and Java code we should manually check it everywhere. This code cares only for Float values,
 * as it is mostly critical one.
 *
 * @author Olga Melnichuk
 *         Date: 21/04/2011
 */
public class NetCDFMissingVal {
    private static final String[] MISSING_VALUE_ATTRIBUTES = new String[]{"missing_value", N3iosp.FillValue};
    private static final float FLOAT_MISSING_VALUE_R = 1e+30f;

    /**
     * Attaches missing_val attribute to the netCDF variable
     *
     * @param var a netCDF variable
     * @param v   a missing value to attach
     * @return the same variable
     */
    public static Variable attachMissingValue(Variable var, Number v) {
        for (String attr : MISSING_VALUE_ATTRIBUTES) {
            var.addAttribute(new Attribute(attr, v));
        }
        return var;
    }

    private Object missingVal = null;

    private NetCDFMissingVal(Variable var) {
        DataType dt = var.getDataType();
        for (String attr : MISSING_VALUE_ATTRIBUTES) {
            Attribute a = var.findAttribute(attr);
            if (a == null) {
                continue;
            }
            if (dt.isNumeric()) {
                missingVal = a.getNumericValue();
            }
            if (dt.isString()) {
                missingVal = a.getStringValue();
            }
        }
    }

    /**
     * Creates missing value checker for a netCDF variable.
     *
     * @param var a netCDF variable
     * @return a missing value checker
     */
    public static NetCDFMissingVal forVariable(Variable var) {
        return new NetCDFMissingVal(var);
    }

    /**
     * Returns true if the given float value could be considered as missing value.
     * Note: -1e+6f value was used long long time ago in atlas netCDF files. So there is a small chance that it is still
     * could be in netCDF files. But this is need to be checked.
     *
     * @param v a float value to check
     * @return true if the
     */
    public boolean isMissVal(Float v) {
        Float[] values = (missingVal == null) ?
                new Float[]{N3iosp.NC_FILL_FLOAT, FLOAT_MISSING_VALUE_R} :
                new Float[]{(Float) missingVal};

        for (Float mv : values) {
            if ((Float.isNaN(mv) && Float.isNaN(v))
                    || (Float.isInfinite(mv) && Float.isInfinite(v))
                    || (Float.floatToIntBits(mv) == Float.floatToIntBits(v))) {
                return true;
            }
        }
        return v <= -1e+6f;
    }

}
