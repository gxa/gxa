package uk.ac.ebi.gxa.dao.procedures;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A simple test class designed for setting up a stored procedure in the
 * in-memory hypersonic database.  This stored procedure ultimately mimics the
 * A2_SAMPLESET procedure that should be found in the "real" atlas2 database
 *
 * @author Tony Burdett
 * @date 08-Oct-2009
 */
public class SampleSetter {
  public static void call(Connection conn,
                          String accession,
                          Object assays, Object properties,
                          String species, String channel) throws SQLException {
    // this mimics the stored procedure A2_SAMPLESET in the actual DB
    Statement stmt = conn.createStatement();

    // create an sampleid - no oracle id generators here!
    int sampleid = (int) System.currentTimeMillis();

    stmt.executeQuery(
        "INSERT INTO A2_SAMPLE(sampleid, accession, species, channel) " +
            "values (" + sampleid + ", '" + accession + "', '" + species +
            "', '" + channel + "');");
  }
}