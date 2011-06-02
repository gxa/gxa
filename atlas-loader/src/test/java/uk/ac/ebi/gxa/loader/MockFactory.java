package uk.ac.ebi.gxa.loader;

import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class MockFactory {
    public static LoaderDAO createLoaderDAO() {
        return new MockLoaderDAO();
    }

    static class MockLoaderDAO extends LoaderDAO {
        public MockLoaderDAO() {
            super(null, null, null, null, null);
        }

        private Map<String, Organism> os = newHashMap();
        private Map<String, Property> ps = newHashMap();
        private Map<Pair<String, String>, PropertyValue> pvs = newHashMap();
        private Map<String, ArrayDesign> ads = newHashMap();

        @Override
        public PropertyValue getOrCreateProperty(String name, String value) {
            PropertyValue pv = pvs.get(Pair.create(name, value));
            if (pv == null) {
                Property p = ps.get(name);
                if (p == null) {
                    ps.put(name, p = new Property(null, name));
                }
                pvs.put(Pair.create(name, value), pv = new PropertyValue(null, p, value));
            }
            return pv;
        }

        @Override
        public ArrayDesign getArrayDesign(String accession) {
            ArrayDesign ad = ads.get(accession);
            if (ad == null) {
                ads.put(accession, ad = new ArrayDesign(accession));
            }
            return ad;
        }

        @Override
        public Organism getOrCreateOrganism(String name) {
            Organism o = os.get(name);
            if (o == null) {
                os.put(name, o = new Organism(null, name));
            }
            return o;
        }
    }
}
