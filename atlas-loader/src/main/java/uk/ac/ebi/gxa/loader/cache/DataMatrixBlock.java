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
package uk.ac.ebi.gxa.loader.cache;

/**
 * @author pashky
*/
public class DataMatrixBlock {
    public final float[] expressionValues;
    public final String[] designElements;
    int size = 0;

    DataMatrixBlock(DataMatrixBlock block) {
        this.expressionValues = block.expressionValues;
        this.designElements = block.designElements;
    }

    DataMatrixBlock(int size, int width) {
        this.expressionValues = new float[size * width];
        this.designElements = new String[size];
    }

    public int capacity() {
        return designElements.length;
    }

    public int size() {
        return size;
    }
}
