package uk.ac.ebi.gxa.data;/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

/**
 * @author Olga Melnichuk
 */
public class ExpressionDataCursor {

    private final FloatMatrixProxy rawExpressions;

    private int dii = -1;
    private int[] des;
    private final String[] deAccessions;
    private final String[] assayAccessions;

    public ExpressionDataCursor(DataProxy dataProxy) throws AtlasDataException {
        this(dataProxy, null);
    }

    public ExpressionDataCursor(DataProxy dataProxy, int[] deIndices) throws AtlasDataException {
        des = deIndices;
        assayAccessions = dataProxy.getAssayAccessions();
        deAccessions = dataProxy.getDesignElementAccessions();
        rawExpressions = deIndices == null ? dataProxy.getAllExpressionData() : dataProxy.getExpressionData(deIndices);
    }

    public String[] getAssayAccessions() {
        return assayAccessions;
    }

    public String getDeAccession() {
        return deAccessions[de()];
    }

    public boolean hasNextDE() {
        return (dii + 1) < getDeCount();
    }

    public boolean nextDE() {
        if (hasNextDE()) {
            dii++;
            return true;
        }
        dii = -1;
        return false;
    }

    public float[] getValues() {
        return dii < getDeCount() ? rawExpressions.getRow(dii) : null;
    }

    public int getDeCount() {
        return des == null ? deAccessions.length : des.length;
    }

    private int de() {
        return des == null ? dii : des[dii];
    }
}
