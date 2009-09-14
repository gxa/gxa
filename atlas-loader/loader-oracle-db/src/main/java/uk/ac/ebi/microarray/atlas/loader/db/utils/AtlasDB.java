package uk.ac.ebi.microarray.atlas.loader.db.utils;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import uk.ac.ebi.microarray.atlas.loader.model.*;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utils for writing atlas loader API objects (see also {@link
 * uk.ac.ebi.microarray.atlas.loader.model}) to a database.  Should be supplied
 * a connection
 *
 * @author Andrey Zorin
 * @date Aug 26, 2009 Time: 5:14:19 PM
 */
public class AtlasDB {
  public static Array toSqlArray(Connection connection,
                                 String typeName,
                                 Object[] value)
      throws SQLException {
    ArrayDescriptor adExpressionValueTable =
        ArrayDescriptor.createDescriptor(typeName, connection);

    return new ARRAY(adExpressionValueTable, connection, value);
  }

  public static STRUCT toSqlStruct(Connection connection, String typeName,
                                   Object[] value)
      throws SQLException {
    StructDescriptor sdExpressionValue =
        StructDescriptor.createDescriptor(typeName, connection);

    return new STRUCT(sdExpressionValue, connection, value);
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

      Object[] expressionValues =
          new Object[null == value.getExpressionValues() ? 0
              : value.getExpressionValues().size()];
      //placeholders for all properties of ExpressionValue structure
      Object[] members = new Object[2];

      if (value.getExpressionValues() != null) {
        int i = 0;
        for (ExpressionValue v : value.getExpressionValues()) {
          members[0] = v.getDesignElementAccession();
          members[1] = v.getValue();

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
      stmt.setString(3, value.getArrayDesignAcession());
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
}
