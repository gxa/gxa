package uk.ac.ebi.microarray.atlas.db.utils;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.model.ArrayDesignQuery;
import uk.ac.ebi.gxa.model.PageSortParams;

import java.sql.*;
import java.util.Map;

/**
 * Utils for writing atlas loader API objects (see also {@link
 * uk.ac.ebi.microarray.atlas.model}) to a database.  Should be supplied a
 * connection
 *
 * @author Andrey Zorin
 * @date Aug 26, 2009 Time: 5:14:19 PM
 */
public class AtlasDB {
  // fixme: this connection object MUST be an OracleConnection, else we'll get a ClassCastException from ArrayDescriptor.createDescriptor()
  public static Array toSqlArray(Connection connection,
                                 String typeName,
                                 Object[] value)
      throws SQLException {
    ArrayDescriptor adExpressionValueTable =
        ArrayDescriptor.createDescriptor(typeName, connection);

    return new ARRAY(adExpressionValueTable, connection, value);
  }

  // fixme: this connection object MUST be an OracleConnection, else we'll get a ClassCastException from StructDescriptor.createDescriptor()
  public static STRUCT toSqlStruct(Connection connection, String typeName,
                                   Object[] value)
      throws SQLException {
    StructDescriptor sdExpressionValue =
        StructDescriptor.createDescriptor(typeName, connection);

    return new STRUCT(sdExpressionValue, connection, value);
  }

  // bind arrayDesignQuery to query parameter
  public static void setArrayDesignQuery(CallableStatement stmt, int ordinal, ArrayDesignQuery arrayDesignQuery) throws SQLException{

      //nested accession query
      Object[] acc = new Object[2];

      acc[0] = arrayDesignQuery.getId();
      acc[1] = arrayDesignQuery.getAccession();
                                                                                                    
      //array design query
      Object[] arr = new Object[3];
      arr[0] = AtlasDB.toSqlStruct(stmt.getConnection(),"ACCESSIONQUERY",acc);  //AccessionQuery
      arr[1] = arrayDesignQuery.getName();
      arr[2] = arrayDesignQuery.getType();
      arr[3] = arrayDesignQuery.getProvider();

      stmt.setObject(ordinal,AtlasDB.toSqlStruct(stmt.getConnection(),"ARRAYDESIGNQUERY", arr));
  }

    // bind PageSortParam to query parameter
    public static void setPageSortParams(CallableStatement stmt, int ordinal, PageSortParams pageSortParams) throws SQLException{

        //PageSortParams
        Object[] arr = new Object[3];
        arr[0] = pageSortParams.getStart();
        arr[1] = pageSortParams.getRows();
        arr[2] = pageSortParams.getOrderBy();

        stmt.setObject(ordinal,AtlasDB.toSqlStruct(stmt.getConnection(),"PAGESORTPARAMS", arr));
    }
                                                                                       
  public static void writeExperiment(Connection connection, Experiment value)
      throws SQLException {
    CallableStatement stmt = null;
    try {
      //1  TheAccession varchar2
      //2  TheDescription varchar2
      //3  ThePerformer varchar2
      //4  TheLab varchar2
      stmt = connection.prepareCall("{call a2_ExperimentSet(?,?,?,?)}");

      stmt.setString(1, value.getAccession());
      stmt.setString(2, value.getDescription());
      stmt.setString(3, value.getPerformer());
      stmt.setString(4, value.getLab());  //properties

      // execute statement
      stmt.execute();
    }
    finally {
      if (stmt != null) {
        // close statement
        stmt.close();
      }
    }
  }

  public static void writeAssay(Connection connection, Assay value)
      throws SQLException {
    CallableStatement stmt = null;
    try {
      //1  Accession varchar2
      //2  ExperimentAccession  varchar2
      //3  ArrayDesignAccession varchar2
      //4  Properties PropertyTable
      //5  ExpressionValues ExpressionValueTable
      stmt = connection.prepareCall("{call a2_AssaySet(?,?,?,?,?)}");

//      Object[] expressionValues =
//          new Object[null == value.getExpressionValues() ? 0
//              : value.getExpressionValues().size()];
//      //placeholders for all properties of ExpressionValue structure
//      Object[] members = new Object[2];
//
//      if (value.getExpressionValues() != null) {
//        int i = 0;
//        for (ExpressionValue v : value.getExpressionValues()) {
//          members[0] = v.getDesignElementAccession();
//          members[1] = v.getValue();
//
//          expressionValues[i++] =
//              toSqlStruct(connection, "EXPRESSIONVALUE", members);
//        }
//      }

      // replacing expression value lookup with mapped values lookup
      Object[] expressionValues =
          new Object[null == value.getExpressionValuesByAccession() ? 0
              : value.getExpressionValuesByAccession().keySet().size()];
      //placeholders for all properties of ExpressionValue structure
      Object[] members = new Object[2];

      if (value.getExpressionValuesByAccession() != null) {
        int i = 0;
        for (Map.Entry<String, Float> expressionValue :
            value.getExpressionValuesByAccession().entrySet()) {
          members[0] = expressionValue.getKey();
          members[1] = expressionValue.getValue();

          expressionValues[i++] =
              toSqlStruct(connection, "EXPRESSIONVALUE", members);
        }
      }

      Object[] properties = new Object[null == value.getProperties()
          ? 0
          : value.getProperties().size()];
      //placeholders for all properties of ExpressionValue structure
      Object[] members1 = new Object[4];

      if (value.getProperties() != null) {
        int i = 0;
        for (Property v : value.getProperties()) {
          members1[0] = v.getAccession(); //accession
          members1[1] = v.getName();
          members1[2] = v.getValue();
          members1[3] = v.isFactorValue();

          properties[i++] = toSqlStruct(connection, "PROPERTY", members1);
        }
      }

      stmt.setString(1, value.getAccession());
      stmt.setString(2, value.getExperimentAccession());
      stmt.setString(3, value.getArrayDesignAccession());
      stmt.setArray(4, toSqlArray(connection, "PROPERTYTABLE", properties));
      stmt.setArray(5, toSqlArray(connection, "EXPRESSIONVALUETABLE",
                                  expressionValues));

      // execute statement
      stmt.execute();
    }
    finally {
      if (stmt != null) {
        // close statement
        stmt.close();
      }
    }
  }

  public static void writeSample(Connection connection, Sample value)
      throws SQLException {
    CallableStatement stmt = null;
    try {
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
        for (Property v : value.getProperties()) {
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
    finally {
      if (stmt != null) {
        // close statement
        stmt.close();
      }
    }
  }

  public static void ExperimentDel(String accession) {

  }

  /**
   * A convenience method for querying for the presence of an experiment in the
   * atlas database.  This method returns true if the experiment already exists
   * in the database, and false if it could not be found.  You should not reload
   * an experiment that already exists without first deleting the old one.
   *
   * @param connection the connection to the atlas database
   * @param accession  the accession number of the experiment to query for
   * @return true if thie experiment already exists in the database, false
   *         otherwise
   * @throws java.sql.SQLException if there was a problem communicating with the
   *                               database
   */
  public static boolean experimentExists(Connection connection,
                                         String accession) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.prepareStatement(
          "select count(*) from A2_EXPERIMENT " +
              "where accession='?'");

      // execute the query
      ResultSet rs = stmt.executeQuery(accession);

      int size = (rs.next() ? rs.getInt(0) : 0);

      return size > 0;
    }
    finally {
      if (stmt != null) {
        // close statement
        stmt.close();
      }
    }
  }

//  public static Set<String> fetchDesignElementsByArrayDesign(
//      Connection connection, String arrayDesignAccession) throws SQLException {
//    PreparedStatement stmt = null;
//    try {
//      stmt = connection.prepareStatement(
//          "select de.accession from A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
//              "where ad.accession=? " +
//              "and de.arraydesignid=ad.arraydesignid");
//
//      // execute the query
//      stmt.setString(1, arrayDesignAccession);
//      ResultSet rs = stmt.executeQuery();
//
//      Set<String> results = new HashSet<String>();
//      while (rs.next()) {
//        results.add(rs.getString(1));
//      }
//
//      return results;
//    }
//    finally {
//      if (stmt != null) {
//        // close statement
//        stmt.close();
//      }
//    }
//  }
}
