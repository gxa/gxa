/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 27, 2009
 * Time: 1:56:37 PM
 * To change this template use File | Settings | File Templates.
 */

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.microarray.atlas.db.utils.AtlasDB;
import uk.ac.ebi.gxa.model.impl.ExpressionStatDao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleConnection;
import org.apache.commons.lang.StringUtils;

public class AtlasDao implements Dao {

    private OracleConnection connection;
    private ExpressionStatDao expressionStatDao = new ExpressionStatDao();

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesign atlasArrayDesignQuery) throws GxaException{
        throw new GxaException("not implemented");
    };

    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accession) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
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

        QueryResultSet<Assay> result = new QueryResultSet<Assay>();

        CallableStatement stmt = null;

        try{
          //1  AssayID int
          //2  Accession varchar2
          //3  Properties PropertyTable
          //4  OUT Assays
          //5  OUT SampleIDs
          //6  OUT Properties

          //fierce nesting
          Object[] PropertyIDs = null;
          if(null != atlasAssayQuery.getPropertyQuery()){
              PropertyIDs = this.getPropertyIDs(atlasAssayQuery.getPropertyQuery());
          }

          stmt = connection.prepareCall("{call a2_AssayGet(?,?,?,?,?,?)}");

          stmt.setString(1, atlasAssayQuery.getId());
          stmt.setString(2, atlasAssayQuery.getAccession());
          stmt.setArray (3, AtlasDB.toSqlArray(connection, "TBLINTEGER", PropertyIDs));

          stmt.registerOutParameter(4, oracle.jdbc.OracleTypes.CURSOR); //samples
          stmt.registerOutParameter(5, oracle.jdbc.OracleTypes.CURSOR); //properties

          stmt.execute();

          ArrayList<Assay> assays = new ArrayList<Assay>();

          ResultSet rsAssays = (ResultSet) stmt.getObject(4);
          ResultSet rsSamples = (ResultSet) stmt.getObject(5);
          ResultSet rsProperties = (ResultSet) stmt.getObject(6);

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
                sampleAccessions.add(rsSamples.getString("SampleAssay"));

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

                rsSamples.next();
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
        throw new GxaException("not implemented");
    };

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery) throws GxaException{
        throw new GxaException("not implemented");
    };

    //return list of int w/o paging - this is public for javadocs only
    public Integer[] getPropertyIDs(PropertyQuery atlasPropertyQuery) throws GxaException{
        CallableStatement stmt = null;

        List<Integer> result = new ArrayList<Integer>();

        try{

          stmt = connection.prepareCall("{call a2_PropertyGet(?,?,?)}");

          stmt.setString(1, atlasPropertyQuery.getId());
          stmt.setString(2, StringUtils.join(atlasPropertyQuery.getFullTextQueries(), " "));

          stmt.registerOutParameter(3, oracle.jdbc.OracleTypes.CURSOR); //samples

          stmt.execute();

          ResultSet rsProperties = (ResultSet) stmt.getObject(3);

          while(rsProperties.next()){
            Integer PropertyID =  rsProperties.getInt("PropertyId");

            result.add(PropertyID);
          }

          return new Integer[3];

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

    public <T extends ExpressionStat> QueryResultSet<T> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery, pageSortParams);
    }

    public <T extends ExpressionStat> QueryResultSet<T> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException{
        return expressionStatDao.getExpressionStat(atlasExpressionStatQuery);
    }

}
