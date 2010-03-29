package uk.ac.ebi.gxa.loader.datamatrix;

import uk.ac.ebi.gxa.utils.FlattenIterator;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Arrays;

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
        this.initialSize = initialSize;
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

    public void add(String designElement, Iterator<Float> values) {
        Block block;
        if(dataBlocks.isEmpty()) {
            block = new Block(initialSize, width);
            dataBlocks.add(block);
        } else {
            block = dataBlocks.getLast();
            if(block.size() == block.capacity()) {
                block = new Block(growSize, width);
                dataBlocks.add(block);
            }
        }

        int position = block.size++;
        block.designElements[position] = designElement;
        for(int i = 0; i < width; ++i)
            if(values.hasNext())
                block.expressionValues[position * width + i] = values.next();
        
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

        private Block(int size, int width) {
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
}
