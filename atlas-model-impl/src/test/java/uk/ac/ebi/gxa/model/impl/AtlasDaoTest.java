package uk.ac.ebi.gxa.model.impl;

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.db.utils.AtlasDB;

import java.sql.CallableStatement;
import java.sql.Connection;

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

        setTrace(ao.getConnection(), true);

        dao = ao;
    }

    private static void setTrace(Connection con, Boolean traceOn) throws Exception{

        CallableStatement stmt = null;

        stmt = con.prepareCall("ALTER SESSION SET SQL_TRACE = false");   //TODO

        stmt.execute();
    }


    @Test
    public void test_getArrayDesign() throws Exception{
        ArrayDesignQuery query = new ArrayDesignQuery();

        query.hasProvider("affy%"); // %metrix

        QueryResultSet<ArrayDesign> result = dao.getArrayDesign(query);

        assertNotNull(result);
    }

    @Test
    public void test_getAssay() throws Exception{
        for(int i=0;i!=1000;i++){
        AssayQuery assayQuery = new AssayQuery();

        PropertyQuery propertyQuery = new PropertyQuery();

        propertyQuery.fullTextQuery("%Gata4%");

        //assayQuery.hasProperty(propertyQuery);
         assayQuery.hasId("884197637");

        QueryResultSet<Assay> result = dao.getAssay(assayQuery);

        assertNotNull(result);
        }

    }

    @Test
    public void test_getSample() throws Exception{

        //ExperimentQuery q = new ExperimentQuery();
        //q.hasId("206548223");

        PropertyQuery q = new PropertyQuery();

        q.hasValue("memory-impaired");
        //q.hasValue("")
        
        SampleQuery q1 = new SampleQuery();
        q1.hasProperty(q);

        QueryResultSet<Sample> result = dao.getSample(q1);

        Assert.assertEquals(8, result.getItems().size());

    }

    @Test
    public void test_getExperiment() throws Exception{

        ExperimentQuery q = new ExperimentQuery();

        q.hasAccession("E-GEOD-3440");

        QueryResultSet<Experiment> result = dao.getExperiment(q);

        Assert.assertEquals("E-GEOD-3440",result.getItem().getAccession());

    }

    @Test
    public void test_getProperty() throws Exception{

        /*
        {
        PropertyQuery query = new PropertyQuery();

        query.isAssayProperty(true);

        QueryResultSet<Property> result = dao.getProperty(query);

        assertNotNull(result);
        assertNotNull(result.getItems());
        }


        {
        PropertyQuery propertyQuery2 = new PropertyQuery();

        propertyQuery2.fullTextQuery("%Gata4%");

        QueryResultSet<Property> result2 = dao.getProperty(propertyQuery2);

        assertNotNull(result2);
        }

        {
        PropertyQuery propertyQuery1 = new PropertyQuery();

        propertyQuery1.hasValue("memory-impaired");

        QueryResultSet<Property> result1 = dao.getProperty(propertyQuery1);

        assertNotNull(result1);

        Assert.assertEquals(2, result1.getItems().size());    
        }

        {
        PropertyQuery propertyQuery2 = new PropertyQuery();

        propertyQuery2.fullTextQuery("%Gata4%");

        QueryResultSet<Property> result2 = dao.getProperty(propertyQuery2);

        assertNotNull(result2);
        } */

        {
        QueryResultSet<Property> result = dao.getProperty(new PropertyQuery().hasValue("liver"));

        Assert.assertEquals(2, result.getItems().size());
        }

        {
        QueryResultSet<Property> result = dao.getProperty(new PropertyQuery().fullTextQuery("%liver%"));

        Assert.assertEquals(17, result.getItems().size());    
        }
    }

    @Test
    public void test_getGene() throws Exception{
        Gene gene = dao.getGene(new GeneQuery().hasId("170040868")).getItem();  //? ????-?? ?? ????????. ge.getGene=170040868,

        assertNotNull(gene);

        Assert.assertEquals(gene.getId(),170040868);
    }

    @Test
    public void getExpressionStat() throws Exception{
             //((AtlasDao)dao).testSome();
             ((AtlasDao)dao).displayDbProperties();
    }


    
}
