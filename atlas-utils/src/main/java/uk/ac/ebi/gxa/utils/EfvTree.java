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

package uk.ac.ebi.gxa.utils;

import java.io.Serializable;
import java.util.*;

/**
 * Experimental factor/values storage collection class. Allows to keep any payload correspoding to several
 * experimental factor values over some factors and retrieve stored information in variety of views sorted by
 * factor values or some custom payload sort orders.
 * <p/>
 * Each payload is associated to one of EFVs which belongs to one of factors.
 *
 * @param <Payload> The class is paramterized with payload type which should be Comparable
 * @author pashky
 */
public class EfvTree<Payload extends Comparable<Payload>> {
    /**
     * View class representing one EFV with associated payload
     *
     * @param <Payload> The class is parametrized with payload type which should be Comparable
     */
    public static class Efv<Payload extends Comparable<Payload>> implements Comparable<Efv<Payload>> {
        private String efv;
        private Payload payload;

        /**
         * Default constructor
         *
         * @param efv     factor value string
         * @param payload asociated payload
         */
        public Efv(final String efv, final Payload payload) {
            this.efv = efv;
            this.payload = payload;
        }

        /**
         * Returns EFV
         *
         * @return EFV string
         */
        public String getEfv() {
            return efv;
        }

        /**
         * Return associated payload
         *
         * @return payload
         */
        public Payload getPayload() {
            return payload;
        }

        public int compareTo(Efv<Payload> o) {
            return getPayload().compareTo(o.getPayload());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Efv))
                return false;
            @SuppressWarnings("unchecked")
            Efv<Payload> other = (Efv<Payload>) obj;
            return !getPayload().equals(other.getPayload());
        }

        @Override
        public int hashCode() {
            return payload != null ? payload.hashCode() : 0;
        }
    }

    /**
     * View class representing one EF with associated EFVs and their payloads
     *
     * @param <Payload> The class is paramterized with payload type which should be Comparable
     */
    public static class Ef<Payload extends Comparable<Payload>> implements Comparable<Ef<Payload>> {
        private String ef;
        private List<Efv<Payload>> efvs;

        /**
         * Default constructor
         *
         * @param ef   factor string
         * @param efvs list of EFVs with associated payloads
         */
        public Ef(String ef, List<Efv<Payload>> efvs) {
            this.ef = ef;
            this.efvs = efvs;
        }

        /**
         * Returns factor
         *
         * @return factor string
         */
        public String getEf() {
            return ef;
        }

        /**
         * Returns list of EFVs with associated payloads
         *
         * @return list of Efv<Payload> items
         */
        public List<Efv<Payload>> getEfvs() {
            return efvs;
        }

        public int compareTo(Ef<Payload> o) {
            int d = 0;
            for (Efv<Payload> efv1 : getEfvs()) {
                for (Efv<Payload> efv2 : o.getEfvs()) {
                    d += efv1.compareTo(efv2);
                }
            }
            return d;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ef ef1 = (Ef) o;

            if (ef != null ? !ef.equals(ef1.ef) : ef1.ef != null) return false;
            if (efvs != null ? !efvs.equals(ef1.efvs) : ef1.efvs != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ef != null ? ef.hashCode() : 0;
            result = 31 * result + (efvs != null ? efvs.hashCode() : 0);
            return result;
        }
    }

    /**
     * View class representing one EF/EFV pair with associated payload
     *
     * @param <Payload> The class is paramterized with payload type which should be Comparable
     */
    public static class EfEfv<Payload> implements Comparable<EfEfv<Payload>>, Serializable {
        private String ef;
        private String efv;

        private Payload payload;

        /**
         * Default constructor
         *
         * @param ef      factor string
         * @param efv     factor value string
         * @param payload associated payload
         */
        public EfEfv(String ef, String efv, Payload payload) {
            this.ef = ef;
            this.efv = efv;
            this.payload = payload;
        }

        /**
         * Returns factor
         *
         * @return factor string
         */
        public String getEf() {
            return ef;
        }

        /**
         * Returns factor value string
         *
         * @return factor value string
         */
        public String getEfv() {
            return efv;
        }

        /**
         * Returns payload
         *
         * @return payload associated with this EFV
         */
        public Payload getPayload() {
            return payload;
        }

    /**
     * This method is used when heatmap column ordering needs to be imposed after an EfEfv was created
     * (c.f. AtlasStructuredQueryService.processResultGenes())
     *
     * @param payload override
     */
        public void setPayload(Payload payload) {
            this.payload = payload;
        }

        /**
         * Returns encoded EF/EFV pair ID encoded with EscapeUtil.encode(ef,efv) method
         *
         * @return string id
         */
        public String getEfEfvId() {
            return EscapeUtil.encode(getEf(), getEfv());
        }

        @Override
        public String toString() {
            return "EfEfv{" +
                    "ef='" + ef + '\'' +
                    ", efv='" + efv + '\'' +
                    ", pl=" + payload +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EfEfv efEfv = (EfEfv) o;

            return !(ef != null ? !ef.equals(efEfv.ef) : efEfv.ef != null) &&
                    !(efv != null ? !efv.equals(efEfv.efv) : efEfv.efv != null);

        }

        @Override
        public int hashCode() {
            int result = ef != null ? ef.hashCode() : 0;
            result = 31 * result + (efv != null ? efv.hashCode() : 0);
            return result;
        }

        public int compareTo(EfEfv<Payload> o) {
            int d = getEf().compareTo(o.getEf());
            return d != 0 ? d : getEfv().compareTo(o.getEfv());
        }
    }

    private SortedMap<String, SortedMap<String, Payload>> efvs = new TreeMap<String, SortedMap<String, Payload>>(String.CASE_INSENSITIVE_ORDER);
    private int efLimit;
    private int efvLimit;

    /**
     * Default constructor
     */
    public EfvTree() {
        efLimit = efvLimit = 5;
    }

    /**
     * Copy constructor
     *
     * @param other source EfvTree to copy from
     */
    public EfvTree(EfvTree<Payload> other) {
        super();
        put(other);
    }

    /**
     * Add another EfvTree to this one, overwriting existing payloads
     *
     * @param other source EfvTree to copy from
     */
    public void put(EfvTree<Payload> other) {
        for (EfEfv<Payload> i : other.getNameSortedList())
            put(i);
    }

    /**
     * Returns payload for EF/EFV pair
     *
     * @param ef  factor string
     * @param efv factor value string
     * @return payload if found or null if not
     */
    public Payload get(String ef, String efv) {
        if (!efvs.containsKey(ef))
            return null;
        return efvs.get(ef).get(efv);
    }

    /**
     * Checks if tree contains payload for EF/EFV pair
     *
     * @param ef  factor string
     * @param efv factor value string
     * @return true if found or false if not
     */
    public boolean has(String ef, String efv) {
        return efvs.containsKey(ef) && efvs.get(ef).containsKey(efv);
    }

    /**
     * Returns payload for corresponding EF/EFV pair or creates a new one using provided factory, stores it and returns
     *
     * @param ef                     factor string
     * @param efv                    factor value string
     * @param plEfoEfvPayloadCreator payload creating factory
     * @return payload
     */
    public Payload getOrCreate(String ef, String efv, Maker<Payload> plEfoEfvPayloadCreator) {
        if (!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String, Payload>(String.CASE_INSENSITIVE_ORDER));

        if (efvs.get(ef).containsKey(efv))
            return efvs.get(ef).get(efv);

        Payload pl = plEfoEfvPayloadCreator.make();
        efvs.get(ef).put(efv, pl);

        return pl;
    }

    /**
     * Stores payload for EF/EFV from some another EfEfv class instance
     *
     * @param efEfv ef/efv pair and associated payload
     * @return view of stored ef/efv/payload
     */
    public EfEfv<Payload> put(EfEfv<Payload> efEfv) {
        return put(efEfv.getEf(), efEfv.getEfv(), efEfv.getPayload());
    }

    /**
     * Stores payload for EF/EFV from some another EfEfv class instance
     *
     * @param ef      factor string
     * @param efv     factor value string
     * @param payload payload to store
     * @return view of stored ef/efv/payload
     */
    public EfEfv<Payload> put(String ef, String efv, Payload payload) {
        if (!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String, Payload>(String.CASE_INSENSITIVE_ORDER));
        efvs.get(ef).put(efv, payload);
        return new EfEfv<Payload>(ef, efv, payload);
    }

    /**
     * Returns total number of stored EFVs with associated payloads
     *
     * @return number
     */
    public int getNumEfvs() {
        int n = 0;
        for (SortedMap<String, Payload> i : efvs.values()) {
            n += i.size();
        }
        return n;
    }

    /**
     * Returns number of stored factors
     *
     * @return number
     */
    public int getNumEfs() {
        return efvs.size();
    }

    /**
     * Return list of EFVs with associated payloads for specific factor
     *
     * @param ef factor string
     * @return list of Efvs
     */
    public List<Efv<Payload>> getEfvs(String ef) {
        List<Efv<Payload>> result = new ArrayList<Efv<Payload>>();
        if (efvs.containsKey(ef)) {
            for (Map.Entry<String, Payload> j : efvs.get(ef).entrySet()) {
                result.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
        }
        return result;
    }

    /**
     * Returns tree-like structure (list of lists) corresponding to stored tree of EFVs with associated payloads.
     * All lists are sorted in lexicographical order of factor and value strings
     *
     * @return list of factors
     */
    public List<Ef<Payload>> getNameSortedTree() {
        List<Ef<Payload>> efs = new ArrayList<Ef<Payload>>();
        for (SortedMap.Entry<String, SortedMap<String, Payload>> i : efvs.entrySet()) {
            List<Efv<Payload>> efvs = new ArrayList<Efv<Payload>>();
            for (Map.Entry<String, Payload> j : i.getValue().entrySet()) {
                efvs.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
            efs.add(new Ef<Payload>(i.getKey(), efvs));
        }

        return efs;
    }

    /**
     * Returns tree-like structure (list of lists) corresponding to stored tree of EFVs with associated payloads.
     * At ef level, the list is sorted alphabetically; the list of efvs under each ef is sorted in in payload sorting order
     * (the one provided by implementation of payload's Comparable interface)
     *
     * @return list of factors
     */
    public List<Ef<Payload>> getEfValueSortedTree() {
        List<Ef<Payload>> efs = new ArrayList<Ef<Payload>>();
        Map<String, List<Efv<Payload>>> efToEfvs = new TreeMap<String, List<Efv<Payload>>>();

        for (EfEfv<Payload> efEfv : getValueSortedList()) {
            String ef = efEfv.getEf();
            if (!efToEfvs.containsKey(ef)) {
                efToEfvs.put(ef, new ArrayList<Efv<Payload>>());
            }
            efToEfvs.get(ef).add(new Efv<Payload>(efEfv.getEfv(), efEfv.getPayload()));
        }
        for (Map.Entry<String, List<Efv<Payload>>> efEntry : efToEfvs.entrySet()) {
            efs.add(new Ef<Payload>(efEntry.getKey(), efEntry.getValue()));
        }
        return efs;
    }

    /**
     * Returns flat list of all EF/EFV pairs with associated payloads sorted in lexicographical order
     * first for EFs, then for values
     *
     * @return list of ef/efv pairs ({@link javax.annotation.Nonnull} values)
     */
    public List<EfEfv<Payload>> getNameSortedList() {
        List<EfEfv<Payload>> result = new ArrayList<EfEfv<Payload>>();
        for (SortedMap.Entry<String, SortedMap<String, Payload>> i : efvs.entrySet()) {
            for (SortedMap.Entry<String, Payload> j : i.getValue().entrySet()) {
                result.add(new EfEfv<Payload>(i.getKey(), j.getKey(), j.getValue()));
            }
        }
        return result;
    }

    /**
     * Returns flat list of  all EF/EFV pairs with associated payloads sorted in payload sorting order
     * (the one provided by implementation of payload's Comparable interface)
     *
     * @return list of ef/efv pairs
     */
    public List<EfEfv<Payload>> getValueSortedList() {
        List<EfEfv<Payload>> result = getNameSortedList();
        Collections.sort(result, new Comparator<EfEfv<Payload>>() {
            public int compare(EfEfv<Payload> o1, EfEfv<Payload> o2) {
                return o1.getPayload().compareTo(o2.getPayload());
            }
        });
        return result;
    }

    /**
     * Throw back object, representing two-level tree of EF/EFVs sorted by Payload and sliced
     * no more than efLimit group with no more than efvLimit values
     *
     * @return iterable collection of objects with "ef" property and "efvs"
     */
    public List<Ef<Payload>> getValueSortedTrimmedTree() {
        List<Ef<Payload>> efs = new ArrayList<Ef<Payload>>();
        for (SortedMap.Entry<String, SortedMap<String, Payload>> i : efvs.entrySet()) {
            List<Efv<Payload>> efvs = new ArrayList<Efv<Payload>>();
            for (Map.Entry<String, Payload> j : i.getValue().entrySet()) {
                efvs.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
            Collections.sort(efvs);
            efs.add(new Ef<Payload>(i.getKey(), efvs.subList(0, Math.min(efvLimit, efvs.size()))));
        }
        Collections.sort(efs);
        return efs.subList(0, Math.min(efLimit, efs.size()));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (EfEfv<Payload> efefv : getNameSortedList()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(efefv.getEf()).append(":").append(efefv.getEfv())
                    .append("(").append(efefv.getEfEfvId()).append(")")
                    .append("=").append(efefv.getPayload());
        }
        return sb.toString();
    }

    /**
     * Removed EFV and associated payload from the tree
     *
     * @param ef  factor string
     * @param efv factor value string
     */
    public void removeEfv(String ef, String efv) {
        if (efvs.containsKey(ef))
            efvs.get(ef).remove(efv);
    }
}
