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

package ae3.model;

import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.IOException;

/**
 * Lazy expression matrix class
 *
 * @author pashky
 */
public class ExpressionMatrix {
    final NetCDFProxy proxy;
    int lastDesignElement = -1;
    float[] lastData = null;

    ExpressionMatrix(NetCDFProxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns expression for design element in assay position
     *
     * @param designElementIndex design element id
     * @param assayId            assay's position in matrix
     * @return expression value
     */
    float getExpression(int designElementIndex, int assayId) {
        try {
            if (lastData == null || lastDesignElement != designElementIndex) {
                lastDesignElement = designElementIndex;
                lastData = proxy.getExpressionDataForDesignElementAtIndex(designElementIndex);
            }
            return lastData[assayId];
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Exception during matrix load", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw LogUtil.createUnexpected("Exception during matrix load", e);
        }
    }
}
