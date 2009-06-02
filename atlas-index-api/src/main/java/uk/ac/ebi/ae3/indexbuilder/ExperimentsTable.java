package uk.ac.ebi.ae3.indexbuilder;

import java.io.*;
import java.util.*;

/**
 * @author pashky
 */
public class ExperimentsTable implements Serializable {
    private ArrayList<Experiment> experiments = new ArrayList<Experiment>();
    private HashMap<String, BitSet> byEfEfvId = new HashMap<String, BitSet>();
    private HashMap<String, BitSet> byEfoId = new HashMap<String, BitSet>();

    public void add(String ef, String efv, String[] efo, long experimentId, boolean isUp, double pvalue) {
        Experiment exp = new Experiment(Expression.valueOf(isUp), experimentId, ef, efv, efo, pvalue);
        experiments.add(exp);

        int pos = experiments.size() - 1;

        String efefvId = IndexField.encode(ef, efv);
        if(!byEfEfvId.containsKey(efefvId))
            byEfEfvId.put(efefvId, new BitSet());

        byEfEfvId.get(efefvId).set(pos);

        if(efo != null)
            for(String oneefo : efo) {
                String efoId = IndexField.encode(oneefo);
                if(!byEfoId.containsKey(efoId))
                    byEfoId.put(efoId, new BitSet());
                byEfoId.get(efoId).set(pos);
            }

    }

    private Iterable<Experiment> makeIterable(final BitSet bs) {
        return new Iterable<Experiment>() {
            public Iterator<Experiment> iterator() {
                return new Iterator<Experiment>() {
                    int pos = bs.nextSetBit(0);

                    public boolean hasNext() {
                        return pos >= 0; 
                    }

                    public Experiment next() {
                        int curr = pos;
                        pos = bs.nextSetBit(pos + 1);
                        return experiments.get(curr);
                    }

                    public void remove() { }
                };
            }
        };
    }

    public Iterable<Experiment> findByEfEfv(String ef, String efv) {
        List<Experiment> result = new ArrayList<Experiment>();

        BitSet bs = byEfEfvId.get(IndexField.encode(ef, efv));
        if(bs == null)
            return result;

        for(int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            result.add(experiments.get(i));
        }
        
        return makeIterable(bs);
    }

    public Iterable<Experiment> findByEfEfvEfoSet(Iterable<String> efefvs, Iterable<String> efos) {
        final BitSet bs = new BitSet();
        for(String efefv : efefvs) {
            BitSet other = byEfEfvId.get(efefv);
            if(other != null)
                bs.or(other);
        }
        for(String efo : efos) {
            BitSet other = byEfoId.get(efo);
            if(other != null)
                bs.or(other);
        }

        return makeIterable(bs);
    }

    public Iterable<Experiment> findByEfoSet(Iterable<String> efos) {

        BitSet bs = new BitSet();
        for(String efo : efos) {
            BitSet other = byEfoId.get(efo);
            if(other != null)
                bs.or(other);
        }

        return makeIterable(bs);
    }

    public String serialize() {
        try {
            return Base64.encodeObject(this, Base64.GZIP);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExperimentsTable deserialize(String from) {
        try {
            return (ExperimentsTable)Base64.decodeToObject(from);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }
}
