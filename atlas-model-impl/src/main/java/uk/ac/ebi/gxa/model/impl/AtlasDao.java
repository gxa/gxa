package uk.ac.ebi.gxa.model.impl; /**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 27, 2009
 * Time: 1:56:37 PM
 * To change this template use File | Settings | File Templates.
 */

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.db.utils.AtlasDB;
import uk.ac.ebi.gxa.model.impl.ExpressionStatDao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import oracle.jdbc.OracleConnection;
import oracle.sql.ARRAY;
import org.apache.commons.lang.StringUtils;

public class AtlasDao implements Dao {

    private OracleConnection connection;
    private ExpressionStatDao expressionStatDao = new ExpressionStatDao();

    public void Connect(String connectionString, String userName, String password) throws Exception
    {
        try{

            DriverManager.registerDriver (new oracle.jdbc.OracleDriver());
            
            connection = (OracleConnection) DriverManager.getConnection(connectionString,userName,password);
        }
        catch(Exception ex){
            throw ex;
        }
    }

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException{
        QueryResultSet<ArrayDesign> result = new QueryResultSet<ArrayDesign>();

        CallableStatement stmt = null;

        try{
          stmt = connection.prepareCall("{call a2_ArrayDesignGet(?,?,?)}");

          AtlasDB.setArrayDesignQuery(stmt,1,atlasArrayDesignQuery);

          AtlasDB.setPageSortParams(stmt,2,pageSortParams);
            
          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //assays

          stmt.execute();

          return null;
        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
        finally {
              if (stmt != null) {
                // close statement
                  try{
                stmt.close();
                  }
                  catch(Exception ex){
                      throw new GxaException(ex.getMessage());
                  }
              }
        }
    };

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException{
        return getArrayDesign(atlasArrayDesignQuery,new PageSortParams());
    };

    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException{
        QueryResultSet<Assay> result = new QueryResultSet<Assay>();

        CallableStatement stmt = null;

        try{
          //fierce nesting

          stmt = connection.prepareCall("{call a2_AssayGet(?,?,?,?,?)}");

          AtlasDB.setAssayQuery(stmt,1,atlasAssayQuery, this); //pass ref to DAO, method pulls list of PropertyDs
          AtlasDB.setPageSortParams(stmt,2,pageSortParams);

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //assays
          stmt.registerOutParameter(4, oracle.jdbc.OracleTypes.CURSOR); //samples
          stmt.registerOutParameter(5, oracle.jdbc.OracleTypes.CURSOR); //properties

          stmt.execute();

          ArrayList<Assay> assays = new ArrayList<Assay>();

          ResultSet rsAssays = (ResultSet) stmt.getObject(3);
          ResultSet rsSamples = (ResultSet) stmt.getObject(4);
          ResultSet rsProperties = (ResultSet) stmt.getObject(5);

          rsSamples.next();
          rsProperties.next();

          while(rsAssays.next()){
            AtlasAssay a = new AtlasAssay();

            int AssayID = rsAssays.getInt("AssayId");
                                                                                                              
            a.setid(AssayID);
            a.setAccession(rsAssays.getString("Accession"));
            a.setExperimentAccession(rsAssays.getString("ExperimentAccession"));

            ArrayList<String> sampleAccessions = new ArrayList<String>();

            while(AssayID == rsSamples.getInt("AssayId")){
                sampleAccessions.add(rsSamples.getString("SampleAccession"));

                rsSamples.next();
            }

            a.setSampleAccessions(sampleAccessions);

            ArrayList<Property> assayproperties = new ArrayList<Property>();

            while(AssayID == rsProperties.getInt("AssayId")){

                AtlasProperty atlasProperty = new AtlasProperty();
                atlasProperty.setName(rsProperties.getString("Property"));

                ArrayList<String> values = new ArrayList<String>();
                values.add(rsProperties.getString("PropertyValue"));

                atlasProperty.setValues(values);

                assayproperties.add(atlasProperty);

                rsProperties.next();
            }

            a.setProperties(new AtlasPropertyCollection(assayproperties));
            assays.add(a);
        }

        result.setItems(assays);
        return result;
        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
        finally {
              if (stmt != null) {
                // close statement
                  try{
                stmt.close();
                  }
                  catch(Exception ex){
                      throw new GxaException(ex.getMessage());
                  }
              }
        }

    };


    class ProperyValuePair {
        public String property;
        public String value;
        public ProperyValuePair(String property, String value){
            this.property = property;
            this.value = value;
        }
    }
    
public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException{

    PageSortParams pageSortParams = new PageSortParams(); //default paging-sorting

    return getAssay(atlasAssayQuery, pageSortParams);

    };

    public Assay                 getAssayByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery) throws GxaException{
        throw new GxaException("not implemented");
    };

    public Sample                 getSampleByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery) throws GxaException{
        throw new GxaException("not implemented");
    };                                                                                                         

    public Experiment                 getExperimentByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams) throws GxaException{
        CallableStatement stmt = null;

        ArrayList<Property> properties = new ArrayList<Property>();

        try{

          stmt = connection.prepareCall("{call a2_PropertyGet(?,?,?)}");

          AtlasDB.setPropertyQuery(stmt,1,atlasPropertyQuery, this); //pass ref to DAO, method pulls list of PropertyDs
          AtlasDB.setPageSortParams(stmt,2,pageSortParams);

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //assays

          stmt.execute();

          ResultSet rsProperties = (ResultSet) stmt.getObject(3);

          Integer currentPropertyID = 0;

          while(rsProperties.next()){
            AtlasProperty a = new AtlasProperty();

            if(!currentPropertyID.equals(rsProperties.getString("PropertyID"))){
                AtlasProperty property = new AtlasProperty();

                property.setid(rsProperties.getInt("PropertyID"));
                property.setName(rsProperties.getString("PropertyName"));

                property.setValues(new ArrayList<String>());

                properties.add(property);
            }

            properties.get(properties.size()-1).getValues().add(rsProperties.getString("Value"));
         }

         QueryResultSet<Property> result = new QueryResultSet<Property>();

         result.setItems(properties);

         return result;


        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
        finally {
              if (stmt != null) {
                // close statement
                  try{
                       stmt.close();
                  }
                  catch(Exception ex){
                      throw new GxaException(ex.getMessage());
                  }
              }
        }
    };

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery) throws GxaException{
        return getProperty(atlasPropertyQuery, new PageSortParams());
    };


    private class requestByID{
        private CallableStatement stmt = null;
        public requestByID(String sp_name) throws Exception{
          stmt = connection.prepareCall("{? = call " + sp_name + "(?)}");
        }
        public Integer[] execute() throws Exception{
            List<Integer> result = new ArrayList<Integer>();

            try{
               stmt.registerOutParameter(1, oracle.jdbc.OracleTypes.ARRAY, "TBLINT"); //samples
               stmt.execute();

               ARRAY rsProperties = (ARRAY) stmt.getArray(1);

               Object[] values = (Object[])rsProperties.getArray();

               for(int i=0;i!=values.length;i++){
                 java.sql.Struct intrecord = (java.sql.Struct)values[i];
                 BigDecimal db = (BigDecimal)intrecord.getAttributes()[0];
                 result.add(db.toBigIntegerExact().intValue());
               }
               return result.toArray(new Integer[]{});
             }
             finally {
                   if (stmt != null) {
                     // close statement
                            stmt.close();
                   }
             }
        }
        public CallableStatement getSatatement(){
            return stmt;
        }
    }


    //return list of int w/o paging - this is public for javadocs only
    public Integer[] getPropertyIDs(PropertyQuery atlasPropertyQuery) throws GxaException{
        try{

         requestByID request = new requestByID("a2_PropertyGet_ID");
         AtlasDB.setPropertyQuery(request.getSatatement(),2,atlasPropertyQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getSampleIDs(SampleQuery atlasSampleQuery) throws GxaException{
        try{

         requestByID request = new requestByID("a2_SampleGet_ID");
         AtlasDB.setSampleQuery(request.getSatatement(),2,atlasSampleQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getAssayIDs(AssayQuery atlasAssayQuery) throws GxaException{
        try{

         requestByID request = new requestByID("a2_AssayGet_ID");
         AtlasDB.setAssayQuery(request.getSatatement(),2,atlasAssayQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getExperimentIDs(ExperimentQuery atlasExperimentQuery) throws GxaException{
        try{

         requestByID request = new requestByID("a2_ExperimentGet_ID");
         AtlasDB.setExperimentQuery(request.getSatatement(),2,atlasExperimentQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getGeneIDs(GeneQuery atlasGeneQuery) throws GxaException{
        try{

         requestByID request = new requestByID("a2_GeneGet_ID");
         AtlasDB.setGeneQuery(request.getSatatement(),2,atlasGeneQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    //return list of int w/o paging - this is public for javadocs only
    public Integer[] getExperimentIDs(PropertyQuery atlasPropertyQuery) throws GxaException{
        CallableStatement stmt = null;

        List<Integer> result = new ArrayList<Integer>();

        try{

          stmt = connection.prepareCall("{? = call a2_PropertyGet_ID(?,?)}");

          stmt.registerOutParameter(1, oracle.jdbc.OracleTypes.ARRAY, "TBLINT"); //samples
          stmt.setString(2, atlasPropertyQuery.getId());
          stmt.setString(3, StringUtils.join(atlasPropertyQuery.getFullTextQueries(), " "));

          stmt.execute();

          ARRAY rsProperties = (ARRAY) stmt.getArray(1);

          Object[] values = (Object[])rsProperties.getArray();

          for(int i=0;i!=values.length;i++){
            java.sql.Struct intrecord = (java.sql.Struct)values[i];

            BigDecimal db = (BigDecimal)intrecord.getAttributes()[0];

            result.add(db.toBigIntegerExact().intValue());
          }

          return result.toArray(new Integer[]{});

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
        finally {
              if (stmt != null) {
                // close statement
                  try{
                       stmt.close();
                  }
                  catch(Exception ex){
                      throw new GxaException(ex.getMessage());
                  }
              }
        }

       // return null; <- why unreachable?
    }


    public Property                 getPropertyByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery) throws GxaException{
        throw new GxaException("not implemented");
    };

    public Gene                 getGeneByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery, pageSortParams);
    }

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException {
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery, new PageSortParams());
    }

}
