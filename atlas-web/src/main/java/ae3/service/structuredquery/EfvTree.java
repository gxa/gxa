package ae3.service.structuredquery;

import ae3.restresult.RestOut;
import ae3.servlet.structuredquery.result.ExperimentRestProfile;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

/**
 * @author pashky
 */
public class EfvTree<Payload extends Comparable<Payload>> {

    public static class Efv<Payload extends Comparable<Payload>> implements Comparable<Efv<Payload>> {
        private String efv;
        private Payload Payload;

        public Efv(final String efv, final Payload Payload) {
            this.efv = efv;
            this.Payload = Payload;
        }

        public String getEfv() {
            return efv;
        }

        public Payload getPayload() {
            return Payload;
        }

        public int compareTo(Efv<Payload> o) {
            return getPayload().compareTo(o.getPayload());
        }
    }

    public static class Ef<Payload extends Comparable<Payload>> implements Comparable<Ef<Payload>> {
        private String ef;
        private List<Efv<Payload>> efvs;

        public Ef(String ef, List<Efv<Payload>> efvs) {
            this.ef = ef;
            this.efvs = efvs;
        }

        public String getEf() {
            return ef;
        }

        public List<Efv<Payload>> getEfvs() {
            return efvs;
        }

        public int compareTo(Ef<Payload> o) {
            int d = 0;
            for(Efv<Payload> efv1 : getEfvs()) {
                for(Efv<Payload> efv2 : o.getEfvs()) {
                    d += efv1.compareTo(efv2);
                }
            }
            return d;
        }
    }

    @RestOut(xmlItemName = "expression", forProfile = ExperimentRestProfile.class) 
    public static class EfEfv<Payload> {
        private String ef;
        private String efv;

        private Payload Payload;

        public EfEfv(String ef, String efv, Payload Payload) {
            this.ef = ef;
            this.efv = efv;
            this.Payload = Payload;
        }

        @RestOut(name = "ef")
        public String getEf() {
            return ef;
        }

        @RestOut(name = "efv")
        public String getEfv() {
            return efv;
        }

        @RestOut(name = "stat", forProfile = ExperimentRestProfile.class)
        public Payload getPayload() {
            return Payload;
        }

        public String getEfEfvId() {
            return EfvTree.getEfEfvId(getEf(), getEfv());
        }

        @Override
        public String toString() {
            return "EfEfv{" +
                    "ef='" + ef + '\'' +
                    ", efv='" + efv + '\'' +
                    ", pl=" + Payload +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EfEfv efEfv = (EfEfv) o;

            if (ef != null ? !ef.equals(efEfv.ef) : efEfv.ef != null) return false;
            if (efv != null ? !efv.equals(efEfv.efv) : efEfv.efv != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ef != null ? ef.hashCode() : 0;
            result = 31 * result + (efv != null ? efv.hashCode() : 0);
            return result;
        }
    }

    private SortedMap<String,SortedMap<String, Payload>> efvs = new TreeMap<String,SortedMap<String, Payload>>();
    private int efLimit;
    private int efvLimit;

    public EfvTree() {
        efLimit = efvLimit = 5;
    }

    public EfvTree(EfvTree<Payload> other) {
        super();
        put(other);
    }

    public void put(EfvTree<Payload> other)
    {
        for(EfEfv<Payload> i : other.getNameSortedList())
            put(i);
    }

    public Payload get(String ef, String efv)
    {
        if(!efvs.containsKey(ef))
            return null;
        return efvs.get(ef).get(efv);
    }

    public boolean has(String ef, String efv)
    {
        return efvs.containsKey(ef) && efvs.get(ef).containsKey(efv);
    }

    public Payload getOrCreate(String ef, String efv, EfoEfvPayloadCreator<Payload> plEfoEfvPayloadCreator)
    {
        if(!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String,Payload>());

        if(efvs.get(ef).containsKey(efv))
            return efvs.get(ef).get(efv);
        
        Payload pl = plEfoEfvPayloadCreator.make();
        efvs.get(ef).put(efv, pl);
        return pl;
    }

    public EfEfv<Payload> put(EfEfv<Payload> efEfv)
    {
        return put(efEfv.getEf(), efEfv.getEfv(), efEfv.getPayload());
    }

    public EfEfv<Payload> put(String ef, String efv, Payload payload)
    {
        if(!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String,Payload>());
        efvs.get(ef).put(efv, payload);
        return new EfEfv<Payload>(ef, efv, payload);
    }

    public int getNumEfvs()
    {
        int n = 0;
        for(SortedMap<String,Payload> i : efvs.values()) {
            n += i.size();
        }
        return n;
    }
    
    public int getNumEfs()
    {
        return efvs.size();
    }

    public List<Efv<Payload>> getEfvs(String ef)
    {
        List<Efv<Payload>> result = new ArrayList<Efv<Payload>>();
        if(efvs.containsKey(ef)) {
            for (Map.Entry<String, Payload> j : efvs.get(ef).entrySet()) {
                result.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
        }
        return result;
    }

    public List<Ef<Payload>> getNameSortedTree()
    {
        List<Ef<Payload>> efs = new ArrayList<Ef<Payload>>();
        for(SortedMap.Entry<String,SortedMap<String,Payload>> i : efvs.entrySet()) {
            List<Efv<Payload>> efvs = new ArrayList<Efv<Payload>>();
            for(Map.Entry<String,Payload> j : i.getValue().entrySet()) {
                efvs.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
            efs.add(new Ef<Payload>(i.getKey(), efvs));
        }

        return efs;
    }

    public List<EfEfv<Payload>> getNameSortedList()
    {
        List<EfEfv<Payload>> result = new ArrayList<EfEfv<Payload>>();
        for(SortedMap.Entry<String,SortedMap<String,Payload>> i : efvs.entrySet()) {
            for(SortedMap.Entry<String,Payload> j : i.getValue().entrySet()) {
                result.add(new EfEfv<Payload>(i.getKey(), j.getKey(), j.getValue()));
            }
        }
        return result;
    }

    public List<EfEfv<Payload>> getValueSortedList()
    {
        List<EfEfv<Payload>> result = getNameSortedList();
        Collections.sort(result, new Comparator<EfEfv<Payload>>() {
            public int compare(EfEfv<Payload> o1, EfEfv<Payload> o2) {
                return o1.getPayload().compareTo(o2.getPayload());
            }
        });
        return result;
    }

    public int getEfLimit() {
        return efLimit;
    }

    public void setEfLimit(int efLimit) {
        this.efLimit = efLimit;
    }

    public int getEfvLimit() {
        return efvLimit;
    }

    public void setEfvLimit(int efvLimit) {
        this.efvLimit = efvLimit;
    }

    /**
     * Throw back object, representing two-level tree of EF/EFVs sorted by Payload and sliced
     * no more than efLimit group with no more than efvLimit values
     * @return iterable collection of objects with "ef" property and "efvs"
     */
    public List<Ef<Payload>> getValueSortedTrimmedTree() {
        List<Ef<Payload>> efs = new ArrayList<Ef<Payload>>();
        for(SortedMap.Entry<String,SortedMap<String,Payload>> i : efvs.entrySet()) {
            List<Efv<Payload>> efvs = new ArrayList<Efv<Payload>>();
            for(Map.Entry<String,Payload> j : i.getValue().entrySet()) {
                efvs.add(new Efv<Payload>(j.getKey(), j.getValue()));
            }
            Collections.sort(efvs);
            efs.add(new Ef<Payload>(i.getKey(), efvs.subList(0, Math.min(efvLimit, efvs.size()))));
        }
        Collections.sort(efs);
        return efs.subList(0, Math.min(efLimit, efs.size()));
    }

    public static String getEfEfvId(String ef, String efv)
    {
        return EscapeUtil.encode(ef, efv);
    }

    public static String getEfEfvId(Ef<?> ef, Efv<?> efv)
    {
        return getEfEfvId(ef.getEf(), efv.getEfv());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(EfEfv<Payload> efefv : getNameSortedList())
        {
            if(sb.length() > 0)
                sb.append(", ");
            sb.append(efefv.getEf()).append(":").append(efefv.getEfv())
                    .append("(").append(efefv.getEfEfvId()).append(")")
                    .append("=").append(efefv.getPayload());
        }
        return sb.toString();
    }

    public void removeEf(String ef)
    {
        efvs.remove(ef);
    }

    public void removeEfv(String ef, String efv)
    {
        if(efvs.containsKey(ef))
            efvs.get(ef).remove(efv);
    }

    public String[] getEfvArray()
    {
        List<EfEfv<Payload>> list = getNameSortedList();
        String[] result = new String[list.size()];
        int k = 0;
        for(EfEfv<Payload> e : list) {
            result[k++] = e.getEfv();
        }
        return result;
    }

    public Set<String> getEfvSet(String ef)
    {
        Map<String,Payload> efvmap = efvs.get(ef);
        return efvmap == null ? new HashSet<String>() : efvmap.keySet();
    }
}
