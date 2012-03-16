package uk.ac.ebi.gxa.loader;

import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.service.PropertyValueMergeService;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static uk.ac.ebi.microarray.atlas.model.Property.createProperty;
import static org.easymock.EasyMock.*;

public class MockFactory {
    public static LoaderDAO createLoaderDAO() {
        return new MockLoaderDAO();
    }

    static class MockLoaderDAO extends LoaderDAO {
        public MockLoaderDAO() {
            super(null, null, null, null);
        }

        private Map<String, Organism> os = newHashMap();
        private Map<String, Property> ps = newHashMap();
        private Map<Pair<String, String>, PropertyValue> pvs = newHashMap();
        private Map<String, ArrayDesign> ads = newHashMap();

        @Override
        public PropertyValue getOrCreatePropertyValue(String name, String value) {
            PropertyValue pv = pvs.get(Pair.create(name, value));
            if (pv == null) {
                Property p = ps.get(name);
                if (p == null) {
                    ps.put(name, p = createProperty(name));
                }
                pvs.put(Pair.create(name, value), pv = new PropertyValue(null, p, value));
            }
            return pv;
        }

        @Override
        public ArrayDesign getArrayDesignShallow(String accession) {
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

    public static PropertyValueMergeService createPropertyValueMergeService() {
        MockPropertyValueMergeService mockPropertyValueMergeService = new MockPropertyValueMergeService();
        mockPropertyValueMergeService.setEfo(mockEfo());
        return mockPropertyValueMergeService;
    }

    static class MockPropertyValueMergeService extends PropertyValueMergeService {
        public MockPropertyValueMergeService() {
            super();
        }
    }


    private static Efo mockEfo() {
        final Efo efo = createMock(Efo.class);
        expect(efo.searchTermPrefix("milligram")).andReturn(Collections.singleton(new EfoTerm("", "milligram", Collections.<String>emptySet(), false, false, false, 0))).anyTimes();
        expect(efo.searchTermPrefix("degree celsius")).andReturn(Collections.singleton(new EfoTerm("", "degree celsius", Collections.<String>emptySet(), false, false, false, 0))).anyTimes();
        expect(efo.searchTermPrefix("degree fahrenheit")).andReturn(Collections.singleton(new EfoTerm("", "degree fahrenheit", Collections.<String>emptySet(), false, false, false, 0))).anyTimes();
        expect(efo.searchTermPrefix("kelvin")).andReturn(Collections.singleton(new EfoTerm("", "kelvin", Collections.<String>emptySet(), false, false, false, 0))).anyTimes();
        expect(efo.searchTermPrefix("becquerel")).andReturn(Collections.singleton(new EfoTerm("", "becquerel", Collections.<String>emptySet(), false, false, false, 0))).anyTimes();
        expect(efo.searchTermPrefix("mg")).andReturn(Collections.<EfoTerm>emptySet()).anyTimes();
        replay(efo);
        return efo;
    }
}
