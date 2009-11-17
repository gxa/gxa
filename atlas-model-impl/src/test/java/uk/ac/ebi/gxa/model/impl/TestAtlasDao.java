package uk.ac.ebi.gxa.model.impl;

import junit.framework.TestCase;

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.model.impl.AtlasDao;

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

    public void testGetArrayDesign(){
        try{
            ArrayDesignQuery query = new ArrayDesignQuery();

            query.hasProvider("affy%"); // %metrix

            QueryResultSet<ArrayDesign> result = atlasDao.getArrayDesign(query);

            assertNotNull(result);
        }
        catch(GxaException ex){
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
