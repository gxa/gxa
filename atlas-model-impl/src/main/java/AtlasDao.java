/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 27, 2009
 * Time: 1:56:37 PM
 * To change this template use File | Settings | File Templates.
 */

import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.model.ArrayDesign;
import uk.ac.ebi.gxa.model.Assay;
import uk.ac.ebi.gxa.model.Experiment;
import uk.ac.ebi.gxa.model.Gene;
import uk.ac.ebi.gxa.model.Property;
import uk.ac.ebi.gxa.model.Sample;

import java.sql.CallableStatement;

import oracle.jdbc.OracleConnection;

public class AtlasDao implements Dao {

    private OracleConnection connection;

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

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException{

        /*
        CallableStatement stmt = null;

        try{
        String accession = atlasAssayQuery.getAccession();
        String id = atlasAssayQuery.getId();
        
          //1  Accession varchar2
          //2  Assays AccessionTable
          //3  Properties PropertyTable
          //4  Species varchar2
          //5  Channel varchar2
          stmt = connection.prepareCall("{call a2_SampleSet(?,?,?,?,?)}");

          Object[] properties =
              new Object[null == value.getProperties() ? 0
                  : value.getProperties().size()];
          Object[] members =
              new Object[4]; //placeholders for all properties of ExpressionValue structure

          if (value.getProperties() != null) {
            int i = 0;
            for (uk.ac.ebi.microarray.atlas.model.Property v : value.getProperties()) {
              members[0] = v.getAccession(); //accession
              members[1] = v.getName();
              members[2] = v.getValue();
              members[3] = v.isFactorValue();

              properties[i++] = toSqlStruct(connection, "PROPERTY", members);
            }
          }

          Object[] assayAccessions = new Object[null == value.getAssayAccessions()
              ? 0
              : value.getAssayAccessions().size()];
          if (value.getAssayAccessions() != null) {
            int i = 0;
            for (String v : value.getAssayAccessions()) {
              assayAccessions[i++] = v;
            }
          }

          stmt.setString(1, value.getAccession());
          stmt.setArray(2,
                        toSqlArray(connection, "ACCESSIONTABLE", assayAccessions));
          stmt.setArray(3, toSqlArray(connection, "PROPERTYTABLE", properties));
          stmt.setString(4, value.getSpecies());  //properties
          stmt.setString(5, value.getChannel());

          // execute statement
          stmt.execute();

        }
        catch(Exception){


        }
        finally {
              if (stmt != null) {
                // close statement
                stmt.close();
              }
            }*/

        throw new GxaException("not implemented");
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

    public QueryResultSet<ExpressionStat> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException{
        throw new GxaException("not implemented");
    };

    public QueryResultSet<ExpressionStat> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException{
        throw new GxaException("not implemented");
    };

}
