/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ae3.service.structuredquery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.ebi.ae3.indexbuilder.Efo;

/**
 *
 * @author pashky
 */
public class EfoTree<PayLoad extends Comparable<PayLoad>> {
    private Efo efo;
    private Map<String,PayLoad> efos = new HashMap<String,PayLoad>();
    private Set<String> explicitEfos = new HashSet<String>();

    public EfoTree(Efo efo) {
        this.efo = efo;
    }

    public PayLoad get(String id) {
        return efos.get(id);
    }

    public PayLoad getOrCreate(String id, EfoEfvPayloadCreator<PayLoad> plCreator)
    {
        
        if(efos.containsKey(id) && explicitEfos.contains(id))
            return efos.get(id);

        Iterable<String> parents = efo.getTermParents(id, true);
        if(parents == null) // it's not in EFO, don't add it
            return null;

        explicitEfos.add(id);

        for(String pId : parents)
            if (!efos.containsKey(pId)) {
                PayLoad pl = plCreator.make();
                efos.put(pId, pl);
            }

        if (!efos.containsKey(id)) {
            PayLoad pl = plCreator.make();
            efos.put(id, pl);
            return pl;
        }

        return efos.get(id);
    }

    public int getNumEfos() {
        return efos.size();
    }

    public int getNumExplicitEfos() {
        return explicitEfos.size();
    }

    public Set<String> getEfoIds() {
        return efos.keySet();
    }

    public Set<String> getExplicitEfos() {
        return explicitEfos;
    }

    public static class EfoItem<PayLoad extends Comparable<PayLoad>> {
        private String id;
        private String term;
        private int depth;
        private PayLoad payload;

        private EfoItem(String id, String term, int depth, PayLoad payload) {
            this.id = id;
            this.term = term;
            this.depth = depth;
            this.payload = payload;
        }

        public int getDepth() {
            return depth;
        }

        public String getId() {
            return id;
        }

        public PayLoad getPayload() {
            return payload;
        }

        public String getTerm() {
            return term;
        }
    }

    public List<EfoItem<PayLoad>> getSubTreeList()
    {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (Efo.Term t : efo.getSubTree(efos.keySet())) {
            result.add(new EfoItem<PayLoad>(t.getId(), t.getTerm(), t.getDepth(), efos.get(t.getId())));
        }
        return result;
    }

    public List<EfoItem<PayLoad>> getValueOrderedList()
    {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        List<String> ids = new ArrayList<String>(efos.keySet());
        Collections.sort(ids, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return efos.get(o1).compareTo(efos.get(o2));
            }
        });
        for (String id : ids) {
            Efo.Term t = efo.getTermById(id);
            result.add(new EfoItem<PayLoad>(t.getId(), t.getTerm(), t.getDepth(), efos.get(t.getId())));
        }
        return result;
    }

    public List<EfoItem<PayLoad>> getExplicitList()
    {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (String id : explicitEfos) {
            Efo.Term t = efo.getTermById(id);
            result.add(new EfoItem<PayLoad>(t.getId(), t.getTerm(), t.getDepth(), efos.get(t.getId())));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(EfoItem<PayLoad> i : getExplicitList()) {
            if(sb.length() > 0)
                sb.append(", ");
            sb.append(i.getId() + "(" + i.getTerm() + ")=" + i.getPayload());
        }
        return sb.toString();
    }


    public void put(EfoItem<PayLoad> other, EfoTree<PayLoad> otherTree) {
        for(String pId : efo.getTermParents(other.getId(), true))
            if (!efos.containsKey(pId)) {
                efos.put(pId, otherTree.get(pId));
            }

        efos.put(other.getId(), other.getPayload());
        explicitEfos.add(other.getId());
    }
}
