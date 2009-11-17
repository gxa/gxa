import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.net.URL;
import java.io.IOException;

import uk.ac.ebi.gxa.model.*;

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
           atlasDao.Connect("jdbc:oracle:thin:@apu.ebi.ac.uk:1521:AEDWT", "Atlas2",  "Atlas2");
       }
       catch (Exception e) {
         fail();
       }
     }
    

    public void testGetAssay(){
        try{

        AssayQuery assayQuery = new AssayQuery();

        PropertyQuery propertyQuery = new PropertyQuery();

        propertyQuery.fullTextQuery("%Gata4%");

        assayQuery.hasProperty(propertyQuery);

        QueryResultSet<Assay> result = atlasDao.getAssay(assayQuery);

        assertNotNull(result);

        }
        catch(GxaException ex){
            fail();
        }
    }

    public void testGetProperty(){
        try{

        PropertyQuery propertyQuery = new PropertyQuery();

        propertyQuery.fullTextQuery("%Gata4%");

        QueryResultSet<Property> result = atlasDao.getProperty(propertyQuery);

        assertNotNull(result);

        }
        catch(GxaException ex){
            fail();
        }


    }
}
