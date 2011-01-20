package uk.ac.ebi.gxa.loader.datamatrix;

import uk.ac.ebi.gxa.utils.FlattenIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.PrimitiveIterators;

import java.util.*;

/**
 * @author pashky
 */
public class DataMatrixStorage {
    private LinkedList<Block> dataBlocks = new LinkedList<Block>();
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
            block = dataBlocks.getLast();
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
