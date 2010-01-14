package uk.ac.ebi.gxa.dao.procedures;

import java.sql.Connection;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 26-Nov-2009
 */
public class LoadProgress {
    public static void call(Connection conn,
                            String accession,
                            String stage,
                            String status,
                            String load_type)
            throws Exception {
        // this mimics the stored procedure load_progress in the actual DB

        // todo
//        // lookup ids from accession first
//        Statement stmt = conn.createStatement();
//
//        stmt.executeUpdate(
//                "INSERT INTO LOAD_MONITOR(accession, stage, status);");
//
//        stmt.close();
    }
}