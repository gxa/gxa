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

import uk.ac.ebi.gxa.utils.FlattenIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.PrimitiveIterators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author pashky
 */
public class DataMatrixStorage {
    private List<Block> dataBlocks = new ArrayList<Block>();
    private int width;
    private int initialSize;
    private int growSize;

    public DataMatrixStorage(int width, int initialSize, int growSize) {
        this.width = width;
        this.initialSize = initialSize > 0 ? initialSize : 1;
        this.growSize = growSize;
    }

    public Iterable<String> getDesignElements() {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new FlattenIterator<Block, String>(dataBlocks.iterator()) {
                    public Iterator<String> inner(Block dataMatrixBlock) {
                        return Arrays.asList(dataMatrixBlock.designElements).subList(0, dataMatrixBlock.size()).iterator();
                    }
                };
            }
        };
    }

    public Iterable<Block> getBlocks() {
        return dataBlocks;
    }

    public int getWidth() {
        return width;
    }

    private static class SFIterator extends MappingIterator<String, Float> {
        private final String[] line;
        private final Map<String, Integer> refMap;

        public SFIterator(List<String> referenceNames, Map<String, Integer> refMap, String[] line) {
            super(referenceNames.iterator());
            this.line = line;
            this.refMap = refMap;
        }

        public Float map(String ref) {
            try {
                return Float.parseFloat(line[refMap.get(ref)]);
            } catch (Exception e) {
                return -1000000F;
            }
        }
    }

    public void add(String designElement, Map<String, Integer> refMap, List<String> referenceNames, String[] line) {
        add(designElement, new SFIterator(referenceNames, refMap, line));
    }

    public void add(String designElement, float[] values) {
        add(designElement, PrimitiveIterators.iterator(values));
    }

    public void add(String designElement, Iterator<Float> values) {
        Block block;
        if (dataBlocks.isEmpty()) {
            block = new Block(initialSize, width);
            dataBlocks.add(block);
        } else {
            block = dataBlocks.get(dataBlocks.size() - 1);
            if (block.size() == block.capacity()) {
                block = new Block(growSize, width);
                dataBlocks.add(block);
            }
        }

        int position = block.size++;
        block.designElements[position] = designElement;

        for (int i = 0; i < width; ++i) {
            if (values.hasNext()) {
                final Float v = values.next();
                block.expressionValues[position * width + i] = v != null ? v : 0f;
            }
        }
    }

    public static class ColumnRef {
        public final DataMatrixStorage storage;
        public final int referenceIndex;

        public ColumnRef(DataMatrixStorage storage, int referenceIndex) {
            this.storage = storage;
            this.referenceIndex = referenceIndex;
        }
    }

    public static class Block {
        public final float[] expressionValues;
        public final String[] designElements;
        int size = 0;

        private Block(int capacity, int width) {
            this.expressionValues = new float[capacity * width];
            this.designElements = new String[capacity];
        }

        public int capacity() {
            return designElements.length;
        }

        public int size() {
            return size;
        }
    }
}
