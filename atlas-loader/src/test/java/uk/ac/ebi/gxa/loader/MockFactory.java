package uk.ac.ebi.gxa.loader;

import org.easymock.EasyMock;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

public class MockFactory {
    public static LoaderDAO createLoaderDAO() {
        final LoaderDAO dao = EasyMock.createMock(LoaderDAO.class);
        EasyMock.expect(dao.getOrCreateProperty("Type", "specific factor value")).andReturn(new PropertyValue(null, new Property(null, "Type"), "specific factor value"));
        EasyMock.replay(dao);
        return dao;
    }
}
