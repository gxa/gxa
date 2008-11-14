package ae3.service.structuredquery;

import java.util.*;
import java.io.UnsupportedEncodingException;

/**
 * @author pashky
 */
public class EfvTree<PayLoad extends Comparable<PayLoad>> {

    public static class Efv<PayLoad extends Comparable<PayLoad>> implements Comparable<Efv<PayLoad>> {
        private String efv;
        private PayLoad payload;
        private int number;

        public Efv(final String efv, final PayLoad payload, final int number) {
            this.efv = efv;
            this.payload = payload;
            this.number = number;
        }

        public String getEfv() {
            return efv;
        }

        public PayLoad getPayload() {
            return payload;
        }

        public int getNumber() {
            return number;
        }

        public int compareTo(Efv<PayLoad> o) {
            return getPayload().compareTo(o.getPayload());
        }
    }

    public static class Ef<PayLoad extends Comparable<PayLoad>> implements Comparable<Ef<PayLoad>> {
        private String ef;
        private List<Efv<PayLoad>> efvs;

        public Ef(String ef, List<Efv<PayLoad>> efvs) {
            this.ef = ef;
            this.efvs = efvs;
        }

        public String getEf() {
            return ef;
        }

        public List<Efv<PayLoad>> getEfvs() {
            return efvs;
        }

        public int compareTo(Ef<PayLoad> o) {
            int d = 0;
            for(Efv<PayLoad> efv1 : getEfvs()) {
                for(Efv<PayLoad> efv2 : o.getEfvs()) {
                    d += efv1.compareTo(efv2);
                }
            }
            return d;
        }
    }

    public static class EfEfv<PayLoad> {
        private String ef;
        private String efv;

        private PayLoad payload;

        public EfEfv(String ef, String efv, PayLoad payLoad) {
            this.ef = ef;
            this.efv = efv;
            this.payload = payLoad;
        }

        public String getEf() {
            return ef;
        }

        public String getEfv() {
            return efv;
        }

        public PayLoad getPayload() {
            return payload;
        }

        public String getEfEfvId() {
            return EfvTree.getEfEfvId(getEf(), getEfv());
        }

        @Override
        public String toString() {
            return "EfEfv{" +
                    "ef='" + ef + '\'' +
                    ", efv='" + efv + '\'' +
                    ", pl=" + payload +
                    '}';
        }
    }

    private SortedMap<String,SortedMap<String, PayLoad>> efvs = new TreeMap<String,SortedMap<String, PayLoad>>();
    private int efLimit;
    private int efvLimit;

    public EfvTree() {
        efLimit = efvLimit = 5;
    }

    public EfvTree(EfvTree<PayLoad> other) {
        super();
        put(other);
    }

    public void put(EfvTree<PayLoad> other)
    {
        for(EfEfv<PayLoad> i : other.getNameSortedList())
            put(i);
    }

    public PayLoad get(String ef, String efv)
    {
        if(!efvs.containsKey(ef))
            return null;
        return efvs.get(ef).get(efv);
    }

    public boolean has(String ef, String efv)
    {
        return efvs.containsKey(ef) && efvs.get(ef).containsKey(efv);
    }

    public interface Creator<T> { T make(); }

    public PayLoad getOrCreate(String ef, String efv, Creator<PayLoad> plCreator)
    {
        if(!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String,PayLoad>());

        if(efvs.get(ef).containsKey(efv))
            return efvs.get(ef).get(efv);
        
        PayLoad pl = plCreator.make();
        efvs.get(ef).put(efv, pl);
        return pl;
    }

    public void put(EfEfv<PayLoad> efEfv)
    {
        put(efEfv.getEf(), efEfv.getEfv(), efEfv.getPayload());
    }

    public void put(String ef, String efv, PayLoad payLoad)
    {
        if(!efvs.containsKey(ef))
            efvs.put(ef, new TreeMap<String,PayLoad>());
        efvs.get(ef).put(efv, payLoad);
    }

    public int getNumEfvs()
    {
        int n = 0;
        for(SortedMap<String,PayLoad> i : efvs.values()) {
            n += i.size();
        }
        return n;
    }
    
    public int getNumEfs()
    {
        return efvs.size();
    }

    public List<Ef<PayLoad>> getNameSortedTree()
    {
        List<Ef<PayLoad>> efs = new ArrayList<Ef<PayLoad>>();
        int number = 0;
        for(SortedMap.Entry<String,SortedMap<String,PayLoad>> i : efvs.entrySet()) {
            List<Efv<PayLoad>> efvs = new ArrayList<Efv<PayLoad>>();
            for(Map.Entry<String,PayLoad> j : i.getValue().entrySet()) {
                efvs.add(new Efv<PayLoad>(j.getKey(), j.getValue(), number++));
            }
            efs.add(new Ef<PayLoad>(i.getKey(), efvs));
        }

        return efs;
    }

    public List<EfEfv<PayLoad>> getNameSortedList()
    {
        List<EfEfv<PayLoad>> result = new ArrayList<EfEfv<PayLoad>>();
        for(SortedMap.Entry<String,SortedMap<String,PayLoad>> i : efvs.entrySet()) {
            for(SortedMap.Entry<String,PayLoad> j : i.getValue().entrySet()) {
                result.add(new EfEfv<PayLoad>(i.getKey(), j.getKey(), j.getValue()));
            }
        }
        return result;
    }

    public List<EfEfv<PayLoad>> getValueSortedList()
    {
        List<EfEfv<PayLoad>> result = getNameSortedList();
        Collections.sort(result, new Comparator<EfEfv<PayLoad>>() {
            public int compare(EfEfv<PayLoad> o1, EfEfv<PayLoad> o2) {
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
     * Throw back object, representing two-level tree of EF/EFVs sorted by payload and sliced
     * no more than efLimit group with no more than efvLimit values
     * @return iterable collection of objects with "ef" property and "efvs"
     */
    public List<Ef<PayLoad>> getValueSortedTrimmedTree() {
        List<Ef<PayLoad>> efs = new ArrayList<Ef<PayLoad>>();
        int number = 0;
        for(SortedMap.Entry<String,SortedMap<String,PayLoad>> i : efvs.entrySet()) {
            List<Efv<PayLoad>> efvs = new ArrayList<Efv<PayLoad>>();
            for(Map.Entry<String,PayLoad> j : i.getValue().entrySet()) {
                efvs.add(new Efv<PayLoad>(j.getKey(), j.getValue(), number++));
            }
            Collections.sort(efvs);
            efs.add(new Ef<PayLoad>(i.getKey(), efvs.subList(0, Math.min(efvLimit, efvs.size()))));
        }
        Collections.sort(efs);
        return efs.subList(0, Math.min(efLimit, efs.size()));
    }

    public static String getEfEfvId(String ef, String efv)
    {
        return encodeEfv(ef) + "_" + encodeEfv(efv);
    }

    public static String getEfEfvId(Ef<?> ef, Efv<?> efv)
    {
        return getEfEfvId(ef.getEf(), efv.getEfv());
    }

    public static String encodeEfv(String v) {
        try {
            StringBuffer r = new StringBuffer();
            for(char x : v.toCharArray())
            {
                if(Character.isJavaIdentifierPart(x))
                    r.append(x);
                else
                    for(byte b : Character.toString(x).getBytes("UTF-8"))
                        r.append("_").append(String.format("%x", b));
            }
            return r.toString();
        } catch(UnsupportedEncodingException e){
            throw new IllegalArgumentException("Unable to encode EFV in UTF-8", e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(EfEfv<PayLoad> efefv : getNameSortedList())
        {
            if(sb.length() > 0)
                sb.append(", ");
            sb.append(efefv.getEf()).append(":").append(efefv.getEfv())
                    .append("(").append(efefv.getEfEfvId()).append(")")
                    .append("=").append(efefv.getPayload());
        }
        return sb.toString();
    }
}
