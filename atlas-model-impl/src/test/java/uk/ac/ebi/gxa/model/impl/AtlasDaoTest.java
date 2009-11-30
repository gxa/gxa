package uk.ac.ebi.gxa.model.impl;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertNotNull;
import uk.ac.ebi.gxa.model.*;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 30, 2009
 * Time: 11:58:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDaoTest {

    private Dao dao = null;

    @Before
    public void SetDao() throws Exception{
        
        AtlasDao ao = new AtlasDao();

        ao.Connect("jdbc:oracle:thin:@apu.ebi.ac.uk:1521:AEDWT", "Atlas2",  "Atlas2");

        dao = ao;

    }

    @Test
    public void test_getProperty() throws Exception{
        try{
        PropertyQuery query = new PropertyQuery();

        query.isAssayProperty(true);

        QueryResultSet<Property> result = dao.getProperty(query);

        assertNotNull(result);
        assertNotNull(result.getItems());    

        }
        catch (Exception ex){
            throw ex;        
        }
    }

    @Test
    public void test_getGene() throws Exception{
        Gene gene = dao.getGene(new GeneQuery().hasId("170040868")).getItem();  //? ????-?? ?? ????????. ge.getGene=170040868,

        System.out.print(gene.getAccession());
        
    }
}
