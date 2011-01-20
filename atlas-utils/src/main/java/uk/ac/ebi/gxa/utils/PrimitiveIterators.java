package uk.ac.ebi.gxa.utils;

import java.util.Iterator;

public class PrimitiveIterators {
    public static Iterator<Float> iterator(float[] data) {
        return new FloatIterator(data);
    }

    private static class FloatIterator implements Iterator<Float> {
        private int current = 0;
        private final float[] data;

        public FloatIterator(float[] data) {
            this.data = data;
        }

        public void remove() throws IllegalStateException {
            throw new IllegalStateException("not implemented");
        }

        public Float next() {
            return data[current++];
        }

        public boolean hasNext() {
            return current < data.length;
        }
    }
}
