package uk.ac.ebi.gxa.dao.procedures;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A simple test class designed for setting up a stored procedure in the
 * in-memory hypersonic database.  This stored procedure ultimately mimics the
 * A2_ASSAYSET procedure that should be found in the "real" atlas2 database
 *
 * @author Tony Burdett
 * @date 08-Oct-2009
 */
public class AssaySetter {
  public static void call(Connection conn,
                          String accession, String experimentAccession,
                          String arrayDesignAccession,
                          Object properties, Object expressionValues)
      throws Exception {

      if(1==1)throw new Exception("obsolete call");

    // this mimics the stored procedure A2_ASSAYSET in the actual DB

    // lookup ids from accession first
    Statement stmt = conn.createStatement();

    int experimentID = -1;
    int arrayDesignID = -1;
    ResultSet rs = stmt.executeQuery("SELECT e.experimentid " +
        "FROM a2_experiment e " +
        "WHERE e.accession = '" + experimentAccession + "';");
    while(rs.next()) {
      experimentID = rs.getInt(1);
    }
    rs.close();

    rs = stmt.executeQuery("SELECT d.arraydesignid " +
        "FROM a2_arraydesign d " +
        "WHERE d.accession = '" + arrayDesignAccession + "';");
    while(rs.next()) {
      arrayDesignID = rs.getInt(1);
    }
    rs.close();

    // create an assayid - no oracle id generators here!
    int assayid = (int) System.currentTimeMillis();

    stmt.executeQuery(
        "INSERT INTO A2_ASSAY(assayid, accession, experimentid, arraydesignid) " +
            "values (" + assayid + ", '" + accession + "', '" +
            experimentID + "', '" + arrayDesignID + "');");

    stmt.close();
  }
}