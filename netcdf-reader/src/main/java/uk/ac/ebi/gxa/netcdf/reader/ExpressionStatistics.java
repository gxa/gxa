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

import com.google.common.collect.Maps;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A helper class to extract expression statistics (form the netcdf) for specified design element indices.
 *
 * @author Olga Melnichuk
 *         Date: 21/04/2011
 */
public class ExpressionStatistics {

    private float[][] pvalues;
    private float[][] tstatistics;
    private Map<String, Integer> efEfv = Maps.newHashMap();
    private Map<Integer, Integer> deIndices = Maps.newHashMap();

    private ExpressionStatistics() {
    }

    private ExpressionStatistics load(int[] deIndices, NetCDFProxy proxy) throws IOException, InvalidRangeException {
        tstatistics = proxy.getTStatistics(deIndices);
        pvalues = proxy.getPValues(deIndices);
        List<String> values = proxy.getUniqueValues();
        for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
            String v = values.get(i);
            efEfv.put(v.toLowerCase(), i);
        }
        for (int i = 0; i < deIndices.length; i++) {
            this.deIndices.put(deIndices[i], i);
        }
        return this;
    }

    public static ExpressionStatistics create(int[] deIndices, NetCDFProxy proxy) throws IOException, InvalidRangeException {
        return (new ExpressionStatistics()).load(deIndices, proxy);
    }

    public UpDownExpression getUpDownExpression(String factor, String factorValue, int deIndex) {
        Integer i = deIndices.get(deIndex);
        Integer j = efEfv.get((factor + "||" + factorValue).toLowerCase());
        if (i == null || j == null) {
            throw new IllegalStateException("Illegal state (" + i + ", " + j + ") for (ef=" + factor + ", efv=" + factorValue + ", deIndex=" + deIndex + ")");
        }
        float p = pvalues[i][j];
        float t = tstatistics[i][j];
        return UpDownExpression.valueOf(p, t);
    }
}
