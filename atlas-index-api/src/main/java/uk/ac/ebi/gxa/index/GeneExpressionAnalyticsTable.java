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

package uk.ac.ebi.gxa.index;

import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.*;
import java.util.*;

/**
 * @author pashky
 */
public class GeneExpressionAnalyticsTable implements Serializable {
    private ArrayList<ExpressionAnalysis> expas = new ArrayList<ExpressionAnalysis>();
    private HashMap<String, BitSet> byEfEfvId = new HashMap<String, BitSet>();
    private HashMap<String, BitSet> byEfoId = new HashMap<String, BitSet>();
    private static final long serialVersionUID = 2L;

    public void add(ExpressionAnalysis analysis) {
        expas.add(analysis);

        int pos = expas.size() - 1;

        String efefvId = EscapeUtil.encode(analysis.getEfName(), analysis.getEfvName());
        if(!byEfEfvId.containsKey(efefvId))
            byEfEfvId.put(efefvId, new BitSet());

        byEfEfvId.get(efefvId).set(pos);

        if(analysis.getEfoAccessions() != null)
            for(String oneefo : analysis.getEfoAccessions()) {
                if(!byEfoId.containsKey(oneefo))
                    byEfoId.put(oneefo, new BitSet());
                byEfoId.get(oneefo).set(pos);
            }

    }

    public void addAll(Collection<ExpressionAnalysis> analyses) {
        expas.addAll(analyses);

        int pos = expas.size() - 1;

        for (ExpressionAnalysis analysis : analyses) {
            String efefvId = EscapeUtil.encode(analysis.getEfName(), analysis.getEfvName());
            if(!byEfEfvId.containsKey(efefvId))
                byEfEfvId.put(efefvId, new BitSet());

            byEfEfvId.get(efefvId).set(pos);

            if(analysis.getEfoAccessions() != null)
                for(String oneefo : analysis.getEfoAccessions()) {
                    if(!byEfoId.containsKey(oneefo))
                        byEfoId.put(oneefo, new BitSet());
                    byEfoId.get(oneefo).set(pos);
                }
        }
    }

    private Iterable<ExpressionAnalysis> makeIterable(final BitSet bs) {
        return new Iterable<ExpressionAnalysis>() {
            public Iterator<ExpressionAnalysis> iterator() {
                return new Iterator<ExpressionAnalysis>() {
                    int pos = bs.nextSetBit(0);

                    public boolean hasNext() {
                        return pos >= 0; 
                    }

                    public ExpressionAnalysis next() {
                        int curr = pos;
                        pos = bs.nextSetBit(pos + 1);
                        return expas.get(curr);
                    }

                    public void remove() { }
                };
            }
        };
    }

    public Iterable<ExpressionAnalysis> findByEfEfv(String ef, String efv) {
        List<ExpressionAnalysis> result = new ArrayList<ExpressionAnalysis>();

        BitSet bs = byEfEfvId.get(EscapeUtil.encode(ef, efv));
        if(bs == null)
            return result;

        for(int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            result.add(expas.get(i));
        }
        
        return makeIterable(bs);
    }

    public Iterable<ExpressionAnalysis> findByEfEfvEfoSet(Iterable<String> efefvs, Iterable<String> efos) {
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

    public Iterable<ExpressionAnalysis> findByEfoSet(Iterable<String> efos) {

        BitSet bs = new BitSet();
        for(String efo : efos) {
            BitSet other = byEfoId.get(efo);
            if(other != null)
                bs.or(other);
        }

        return makeIterable(bs);
    }

    public Iterable<ExpressionAnalysis> getAll() {
        return expas;
    }


    public Iterable<ExpressionAnalysis> findByExperimentId(final long experimentId) {
        return new Iterable<ExpressionAnalysis>() {
            public Iterator<ExpressionAnalysis> iterator() {
                return new FilterIterator<ExpressionAnalysis, ExpressionAnalysis>(expas.iterator()) {
                    @Override
                    public ExpressionAnalysis map(ExpressionAnalysis from) {
                        return from.getExperimentID() == experimentId ? from : null;
                    }
                };
            }
        };
    }

    public Iterable<ExpressionAnalysis> findByFactor(final String factorName) {
        return new Iterable<ExpressionAnalysis>() {
            public Iterator<ExpressionAnalysis> iterator() {
                return new FilterIterator<ExpressionAnalysis, ExpressionAnalysis>(expas.iterator()) {
                    @Override
                    public ExpressionAnalysis map(ExpressionAnalysis from) {
                        return from.getEfName().equals(factorName) ? from : null;
                    }
                };
            }
        };
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bstream);
            ostream.writeObject(this);
            return bstream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GeneExpressionAnalyticsTable deserialize(byte[] from) {
        try {
            ByteArrayInputStream bstream = new ByteArrayInputStream(from);
            ObjectInputStream ostream = new ObjectInputStream(bstream);
            return (GeneExpressionAnalyticsTable)ostream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }
}
