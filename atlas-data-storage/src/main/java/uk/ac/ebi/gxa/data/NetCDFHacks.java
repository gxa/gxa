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

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;

import java.io.IOException;
import java.util.List;

/**
 * @author alf
 */
class NetCDFHacks {
    public static void safeCreate(NetcdfFileWriteable ncdf) throws IOException {
        updateAttributes(ncdf);
        ncdf.create();
    }

    public static void updateAttributes(NetcdfFileWriteable ncdf) {
        updateAttributes(ncdf.getRootGroup());
        updateAttributes(ncdf.getVariables());
    }

    private static void updateAttributes(Group group) {
        if (group == null)
            return;

        updateAttributes(group.getVariables());
        if (group.getGroups() != null) {
            for (Group g : group.getGroups()) {
                updateAttributes(g);
            }
        }
    }

    private static void updateAttributes(List<Variable> variables) {
        if (variables == null)
            return;
        for (Variable v : variables) {
            if (v.getDataType() == DataType.DOUBLE) {
                fixDouble(v);
            } else if (v.getDataType() == DataType.FLOAT) {
                fixFloat(v);
            }
        }
    }

    private static void fixFloat(Variable v) {
        v.addAttribute(new Attribute(N3iosp.FillValue, Float.NaN));
        v.addAttribute(new Attribute("missing_value", Float.NaN));
    }

    private static void fixDouble(Variable v) {
        v.addAttribute(new Attribute(N3iosp.FillValue, Double.NaN));
        v.addAttribute(new Attribute("missing_value", Double.NaN));
    }
}
