import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.net.URL;
import java.io.IOException;

import uk.ac.ebi.gxa.model.AssayQuery;
import uk.ac.ebi.gxa.model.PropertyQuery;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 10, 2009
 * Time: 1:07:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAtlasDao extends TestCase {
    private AtlasDao atlasDao = new AtlasDao();

    public void setUp() {
       try {
           //
       }
       catch (Exception e) {
         fail();
       }
     }
    

    public void testGetAssay(){

        AssayQuery atlasAssayQuery = new AssayQuery();

        PropertyQuery propertyQuery = new PropertyQuery(); 

        //atlasAssayQuery.hasProperty(propertyQuery);

        //atlasDao.getAssay(atlasAssayQuery);
        
    }
}
