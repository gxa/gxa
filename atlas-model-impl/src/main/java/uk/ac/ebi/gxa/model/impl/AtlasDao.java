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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import oracle.jdbc.OracleConnection;
import oracle.sql.ARRAY;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class AtlasDao implements Dao {

    private OracleConnection connection;
    private ExpressionStatDao expressionStatDao = new ExpressionStatDao();

    class ProperyValuePair {
        public String property;
        public String value;
        public ProperyValuePair(String property, String value){
            this.property = property;
            this.value = value;
        }
    }

    public OracleConnection getConnection(){
        return connection;
    }

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

    /////ArrayDesign

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException{
        QueryResultSet<ArrayDesign> result = new QueryResultSet<ArrayDesign>();

        CallableStatement stmt = null;

        try{
          stmt = connection.prepareCall("{call AtlasAPI.a2_ArrayDesignGet(?,?,?,?)}");

          AtlasDB.setArrayDesignQuery(stmt,1,atlasArrayDesignQuery);

          AtlasDB.setPageSortParams(stmt,2,pageSortParams);
            
          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //arrayDesigns
          stmt.registerOutParameter(4, oracle.jdbc.OracleTypes.CURSOR); //designElements

          stmt.execute();

          /*
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
          }*/
            

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

    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accessionQuery) throws GxaException{

        QueryResultSet<ArrayDesign> result = this.getArrayDesign(new ArrayDesignQuery(accessionQuery));

        return result.getItem();
    };

    ////Assay

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException{
        QueryResultSet<Assay> result = new QueryResultSet<Assay>();

        CallableStatement stmt = null;

        try{
          //fierce nesting

          stmt = connection.prepareCall("{call AtlasAPI.a2_AssayGet(?,?,?,?,?)}");

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

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException{

    PageSortParams pageSortParams = new PageSortParams(); //default paging-sorting

    return getAssay(atlasAssayQuery, pageSortParams);

    };

    public Assay                 getAssayByAccession(AccessionQuery accessionQuery) throws GxaException{
        QueryResultSet<Assay> result = this.getAssay(new AssayQuery(accessionQuery));
        return result.getItem();
    };

    ////Sample

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams) throws GxaException{

        QueryResultSet<Sample> result = new QueryResultSet<Sample>();
        CallableStatement stmt = null;

        try{
          stmt = connection.prepareCall("{call AtlasAPI.a2_SampleGet(?,?,?,?,?)}");

          AtlasDB.setSampleQuery(stmt,1,atlasSampleQuery, this); //pass ref to DAO, method pulls list of PropertyDs
          AtlasDB.setPageSortParams(stmt,2,pageSortParams);

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //assays
          stmt.registerOutParameter(4, oracle.jdbc.OracleTypes.CURSOR); //samples
          stmt.registerOutParameter(5, oracle.jdbc.OracleTypes.CURSOR); //properties

          stmt.execute();

          ArrayList<Sample> assays = new ArrayList<Sample>();

          ResultSet rsSamples = (ResultSet) stmt.getObject(3);
          ResultSet rsAssays = (ResultSet) stmt.getObject(4);
          ResultSet rsProperties = (ResultSet) stmt.getObject(5);

          rsAssays.next();
          rsProperties.next();

          while(rsSamples.next()){
            AtlasSample a = new AtlasSample();

            int SampleID = rsSamples.getInt("SampleId");

            a.setid(SampleID);
            a.setAccession(rsSamples.getString("Accession"));
            //a.setExperimentAccession(rsAssays.getString("ExperimentAccession"));

            ArrayList<String> assayAccessions = new ArrayList<String>();

            while(SampleID == rsAssays.getInt("SampleId")){
                assayAccessions.add(rsAssays.getString("AssayAccession"));

                if(!rsAssays.next())
                    break;
            }

            a.setAssayAccessions(assayAccessions);

            ArrayList<Property> sampleproperties = new ArrayList<Property>();

            while(SampleID == rsProperties.getInt("SampleId")){

                AtlasProperty atlasProperty = new AtlasProperty();
                atlasProperty.setName(rsProperties.getString("Property"));

                ArrayList<String> values = new ArrayList<String>();
                values.add(rsProperties.getString("PropertyValue"));

                atlasProperty.setValues(values);

                sampleproperties.add(atlasProperty);

                if(!rsProperties.next())
                    break;
            }

            a.setProperties(new AtlasPropertyCollection(sampleproperties));
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

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery) throws GxaException{
        return getSample(atlasSampleQuery, new PageSortParams());
    };

    public Sample                 getSampleByAccession(AccessionQuery accessionQuery) throws GxaException{
        QueryResultSet<Sample> result = this.getSample(new SampleQuery(accessionQuery));
        return result.getItem();
    };


    ////Experiment

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery) throws GxaException{
        return getExperiment(atlasExperimentQuery, new PageSortParams());
    };                                                                                                         

    public Experiment                 getExperimentByAccession(AccessionQuery accessionQuery) throws GxaException{
        QueryResultSet<Experiment> result = this.getExperiment(new ExperimentQuery(accessionQuery));
        return result.getItem();
    };

    ////Property

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams) throws GxaException{
        CallableStatement stmt = null;

        ArrayList<Property> properties = new ArrayList<Property>();

        try{

          stmt = connection.prepareCall("{call AtlasAPI.a2_PropertyGet(?,?,?)}");

          AtlasDB.setPropertyQuery(stmt,1,atlasPropertyQuery, this); //pass ref to DAO, method pulls list of PropertyDs
          AtlasDB.setPageSortParams(stmt,2,pageSortParams);

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //assays

          stmt.execute();

          ResultSet rsProperties = (ResultSet) stmt.getObject(3);

          String currentPropertyID = "0";

          while(rsProperties.next()){
            AtlasProperty a = new AtlasProperty();

            if(!currentPropertyID.equals(rsProperties.getString("PropertyID"))){
                AtlasProperty property = new AtlasProperty();

                property.setid(rsProperties.getInt("PropertyID"));
                property.setName(rsProperties.getString("PropertyName"));

                property.setValues(new ArrayList<String>());

                properties.add(property);
            }

            currentPropertyID = rsProperties.getString("PropertyID");

            properties.get(properties.size()-1).getValues().add(rsProperties.getString("Value"));
         }

         QueryResultSet<Property> result = new QueryResultSet<Property>();

         result.setItems(properties);

         return result;


        }
        catch(Exception ex){
            throw new GxaException(ex);
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

    public Property                 getPropertyByAccession(AccessionQuery accessionQuery) throws GxaException{
        QueryResultSet<Property> result = this.getProperty(new PropertyQuery(accessionQuery));
        return result.getItem();
    };

    ///Gene

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams) throws GxaException{
        QueryResultSet<Gene> result = new QueryResultSet<Gene>();
        CallableStatement stmt = null;

        try{
          stmt = connection.prepareCall("{call AtlasAPI.a2_GeneGet(?,?,?,?)}");

          AtlasDB.setGeneQuery(stmt,1,atlasGeneQuery, this); //pass ref to DAO, method pulls list of PropertyDs
          AtlasDB.setPageSortParams(stmt,2,pageSortParams);

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //genes
          stmt.registerOutParameter(4, oracle.jdbc.OracleTypes.CURSOR); //properties

          stmt.execute();

          ArrayList<Gene> genes = new ArrayList<Gene>();

          ResultSet rsGenes = (ResultSet) stmt.getObject(3);
          ResultSet rsProperties = (ResultSet) stmt.getObject(4);

          rsProperties.next();

          while(rsGenes.next()){
            AtlasGene a = new AtlasGene();

            int GeneID = rsGenes.getInt("GeneId");

            a.setid(GeneID);
            a.setAccession(rsGenes.getString("Identifier"));

            ArrayList<Property> geneproperties = new ArrayList<Property>();

            while(GeneID == rsProperties.getInt("GeneId")){

                AtlasProperty atlasProperty = new AtlasProperty();
                atlasProperty.setName(rsProperties.getString("Property"));

                ArrayList<String> values = new ArrayList<String>();
                values.add(rsProperties.getString("PropertyValue"));

                atlasProperty.setValues(values);

                geneproperties.add(atlasProperty);

                if(!rsProperties.next())
                    break;
            }

            a.setProperties(new AtlasPropertyCollection(geneproperties));
            genes.add(a);
        }

        result.setItems(genes);

        return result;
        }
        catch(Exception ex){
            throw new GxaException(ex);
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

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery) throws GxaException{
        return getGene(atlasGeneQuery, new PageSortParams());
    };

    public Gene                 getGeneByAccession(AccessionQuery accessionQuery) throws GxaException{
        QueryResultSet<Gene> result = this.getGene(new GeneQuery(accessionQuery));
        return result.getItem();
    };

    ///Get IDs - utility functions

    private class RequestByID{
        private CallableStatement stmt = null;
        public RequestByID(String sp_name) throws Exception{
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

         RequestByID request = new RequestByID("AtlasAPI.a2_PropertyGet_ID");
         AtlasDB.setPropertyQuery(request.getSatatement(),2,atlasPropertyQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getSampleIDs(SampleQuery atlasSampleQuery) throws GxaException{
        try{

         RequestByID request = new RequestByID("AtlasAPI.a2_SampleGet_ID");
         AtlasDB.setSampleQuery(request.getSatatement(),2,atlasSampleQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getAssayIDs(AssayQuery atlasAssayQuery) throws GxaException{
        try{

         RequestByID request = new RequestByID("AtlasAPI.a2_AssayGet_ID");
         AtlasDB.setAssayQuery(request.getSatatement(),2,atlasAssayQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getExperimentIDs(ExperimentQuery atlasExperimentQuery) throws GxaException{
        try{

         RequestByID request = new RequestByID("AtlasAPI.a2_ExperimentGet_ID");
         AtlasDB.setExperimentQuery(request.getSatatement(),2,atlasExperimentQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    public Integer[] getGeneIDs(GeneQuery atlasGeneQuery) throws GxaException{
        try{

         RequestByID request = new RequestByID("AtlasAPI.a2_GeneGet_ID");
         AtlasDB.setGeneQuery(request.getSatatement(),2,atlasGeneQuery,this);
         return request.execute();

        }
        catch(Exception ex){
            throw new GxaException(ex.getMessage());
        }
    }

    /***
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
    ****/


    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery, pageSortParams);
    }

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException {
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery, new PageSortParams());
    }

    public void testSome() throws Exception{
        CallableStatement stmt = null;

        stmt = connection.prepareCall("{call ATLASLDR.SomeProcedure(?)}");

        Object[] arr = new Object[3];
        arr[0] = 1;
        arr[1] = "hello";

        stmt.setObject(1, AtlasDB.toSqlStruct(stmt.getConnection(), "ATLASLDR.SomeQuery", arr));

        stmt.execute();


    }

        public void displayDbProperties(){
            java.sql.DatabaseMetaData dm = null;
            java.sql.ResultSet rs = null;
            try{
                if(connection!=null){
                dm = connection.getMetaData(); 
                    System.out.println("\nDriver Information");
                    System.out.println("\tDriver Name: "+ dm.getDriverName());
                    System.out.println("\tDriver Version: "+ dm.getDriverVersion ());
                    System.out.println("\nDatabase Information ");
                    System.out.println("\tDatabase Name: "+ dm.getDatabaseProductName());
                    System.out.println("\tDatabase Version: "+ dm.getDatabaseProductVersion());
                    System.out.println("\tMaximum Connection (If zero--> no limit): "+dm.getMaxConnections());
                    System.out.println("\tNumeric Functions: "+dm.getNumericFunctions());
                    System.out.println("Avalilable Catalogs ");
                    rs = dm.getCatalogs();
                    while(rs.next()){
                        System.out.println("\tcatalog: "+ rs.getString(1));
                    }
                    rs.close();
                    rs = null;
                }else
                    System.out.println("Error: No active Connection");
            }catch(Exception e){
                e.printStackTrace();
            } dm=null;
        }
    }
 
