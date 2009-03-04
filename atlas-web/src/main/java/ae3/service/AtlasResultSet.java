package ae3.service;

import java.util.*;
import java.lang.IllegalArgumentException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
* User: ostolop
* Date: 06-Apr-2008
* Time: 18:57:29
* To change this template use File | Settings | File Templates.
*/
public class AtlasResultSet implements Serializable {
    protected final Log log = LogFactory.getLog(getClass());
    private static final String insert_query = "insert into atlas (idkey, experiment_id, experiment_accession, experiment_description, gene_id, gene_name, gene_identifier, gene_species, ef, efv, updn, updn_pvaladj, gene_highlights) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private String idkey;
    private String searchkey;
    private int eltCount = 0;
    private static final String omittedEFs = "age,individual,time,dose,V1";
    public AtlasResultSet(final String searchkey) {
        if(null== searchkey || searchkey.equals("")) {
            throw new IllegalArgumentException ("Must be constructed with a non-empty cache string key");	
        }

        this.searchkey = searchkey;

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement pst = conn.prepareStatement("SELECT RANDOM_UUID()");
            ResultSet idrs = pst.executeQuery();
            idrs.next();
            idkey = idrs.getString(1);
            idrs.close();
            pst.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        if (idkey == null) throw new IllegalStateException("Failed to obtain a UUID for AtlasResultSet");
    }

    public List<HashMap> getAtlasEfvCounts() {
        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT efv || ' (' || ef || ')' as efvef, efv, ef," +
                            "count(distinct experiment_id) AS ce, " +
                            "group_concat(distinct experiment_accession separator ', ') AS experiments, " + 
                            "group_concat(distinct experiment_id separator ', ' ) AS exp_ids, "+
                            "ifnull(abs(sum(nullif(updn,-1))),0) AS sumup, " +
                            "ifnull(abs(sum(nullif(updn,1))) ,0) AS sumdn, " +
                            "avg(updn_pvaladj) as mpv " +
                            "FROM ATLAS " +
                            "WHERE idkey=? " +
                            "GROUP BY efv || ' (' || ef || ')'  " +
//                            "HAVING count(distinct experiment_id) > 1" +
                            "ORDER BY ce DESC, mpv, sum(abs(updn)) DESC, efvef");

            stmt.setString(1, idkey);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
            	String efv = rs.getString("efv");
            	String ef = rs.getString("ef");
            	if(!omittedEFs.contains(efv) && !omittedEFs.contains(ef)){
            		HashMap<String,String> h = new HashMap<String,String>();
                    String efvShort = efv.length() > 30 ? efv.substring(0,30)+"..." : efv;
                    h.put("efv_short",efvShort );
                    h.put("efv",efv);
                    h.put("ef", rs.getString("ef"));
                    h.put("efvef", rs.getString("efvef"));
                    h.put("experiment_count", rs.getString("ce"));
                    h.put("experiments", rs.getString("experiments"));
                    h.put("exp_ids", rs.getString("exp_ids"));
                    h.put("up_count", rs.getString("sumup"));
                    h.put("dn_count", rs.getString("sumdn"));

                    ars.add(h);
            	}
                
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return ars;
    }

    public List<HashMap> getAtlasExperimentsForEfv(String efv, String ef) {
        Vector v = new Vector();
        v.add(efv); v.add(ef);

        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT experiment_accession, experiment_description, min(updn_pvaladj) as mpv " +
                    "FROM ATLAS " +
                    "WHERE idkey=? " +
                    "AND efv=? " +
                    "GROUP BY experiment_accession, experiment_description " +
                    "ORDER BY mpv ASC");

            stmt.setString(1, idkey);
            stmt.setString(2, efv);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                HashMap<String,String> h = new HashMap<String,String>();
                h.put("experiment_accession", rs.getString("experiment_accession"));
                h.put("experiment_description", rs.getString("experiment_description"));
                h.put("mpv", rs.getString("mpv"));

                ars.add(h);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return ars;
    }

    public List<HashMap> getAtlasResultsForEfvAndExpt(final String efv, final String experiment_accession) {
        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT gene_name, gene_identifier, updn, updn_pvaladj " +
                    "FROM ATLAS " +
                    "WHERE idkey=? " +
                    "AND efv=? " +
                    "AND experiment_accession=? " +
                    "ORDER BY updn_pvaladj ASC");

            stmt.setString(1, idkey);
            stmt.setString(2, efv);
            stmt.setString(3, experiment_accession);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                HashMap<String,String> h = new HashMap<String,String>();
                h.put("gene_name", rs.getString("gene_name"));
                h.put("gene_identifier", rs.getString("gene_identifier"));
                h.put("updn", rs.getString("updn"));
                h.put("updn_pvaladj", rs.getString("updn_pvaladj"));

                ars.add(h);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return ars;
    }

    /**
     * Clean-up of AtlasResultSet: delete all relevant records from the in-memory (H2) ARS database.
     */
    public void cleanup () {
        log.info("Cleaning up AtlasResultSet: " + idkey);

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM ATLAS WHERE idkey=?");
            stmt.setString(1, idkey);

            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        assert !isAvailableInDB() : "Should be cleaned up from the database!";
    }

    public List<HashMap> getAtlasResultGenes() {
        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT gene_name, gene_identifier,gene_id, avg(updn_pvaladj) as mpv " +
                    "FROM ATLAS " +
                    "WHERE idkey=? " +
                    "GROUP BY gene_name, gene_identifier " +
                    "ORDER BY mpv ASC");

            stmt.setString(1, idkey);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                HashMap<String,String> h = new HashMap<String,String>();
                h.put("gene_name", rs.getString("gene_name"));
                h.put("gene_identifier", rs.getString("gene_identifier"));
                h.put("mpv", rs.getString("mpv"));
                h.put("gene_id", rs.getString("gene_id"));

                ars.add(h);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return ars;
    }

    public HashMap<String,HashMap<String,String>> getAtlasResultAllGenesByEfv() {
        HashMap<String,HashMap<String,String>> ars = new HashMap<String,HashMap<String,String>>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT " +
                    " gene_identifier, " +
                    " efv || ' (' || ef || ')' as efvef, " +
                    " group_concat(distinct experiment_accession separator ', ') AS experiment_count, " +
                    " avg(casewhen(updn=1,updn_pvaladj,null))  as mpvup, " +
                    " avg(casewhen(updn=-1,updn_pvaladj,null)) as mpvdn, " +
                    " sum(casewhen(updn= 1,1,0)) as sumup, " +
                    " sum(casewhen(updn=-1,1,0)) as sumdn, " +
                    " count(distinct (casewhen(updn=1,'1'||experiment_id,null))) as countup, "+
                    " count(distinct (casewhen(updn=-1,'1'||experiment_id,null))) as countdn "+
                    " FROM ATLAS " +
                    " WHERE idkey=? " +
//                    "AND gene_identifier=? " +
//                    "AND efv=?" +
                    " GROUP by gene_identifier, efv || ' (' || ef || ')'");

            stmt.setString(1, idkey);
//            stmt.setString(2, efv);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                HashMap<String,String> h = new HashMap<String,String>();
                h.put("experiment_count", rs.getString("experiment_count"));
                h.put("mpvup", rs.getString("mpvup"));
                h.put("mpvdn", rs.getString("mpvdn"));
                h.put("countup", rs.getString("countup"));
                h.put("countdn", rs.getString("countdn"));

                ars.put(rs.getString("gene_identifier") + rs.getString("efvef"), h);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return ars;
    }

    public long getFullRecordCount() {
        long count = 0;

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT count(*) from ATLAS WHERE idkey=?");

            stmt.setString(1, idkey);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()) {
                count = rs.getLong(1);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return count;
    }

    public Vector<HashMap> getAllAtlasResults(final String sortby) {
        Vector<HashMap> lm = new Vector<HashMap>();

        String sort_clause = "efv, experiment_accession";

        if (sortby != null) {
            if (sortby.equals("experiment"))
                sort_clause = "experiment_accession";
            else if (sortby.equals("ef"))
                sort_clause = "ef";
            else if (sortby.equals("efv"))
                sort_clause = "efv";
            else if (sortby.equals("gene"))
                sort_clause = "gene_name";
        }


        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT experiment_id, experiment_accession, experiment_description, gene_id, gene_name, gene_identifier, gene_species, ef, efv, updn, updn_pvaladj, gene_highlights " +
                    " FROM ATLAS WHERE idkey=? ORDER BY updn_pvaladj ASC, " + sort_clause + ", experiment_accession");

            stmt.setString(1, idkey);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                HashMap h = new HashMap();
                for (String s : new String[] {"experiment_id", "experiment_accession", "experiment_description", "gene_id", "gene_name", "gene_identifier", "gene_species", "ef", "efv", "gene_highlights"} ) {
                    h.put(s, rs.getString(s));
                }
                h.put("updn", rs.getInt("updn"));
                h.put("updn_pvaladj", rs.getDouble("updn_pvaladj"));

                lm.add(h);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return lm;
    }

    public void add(AtlasResult atlasResult) {
        Connection conn = null;

        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement memstm = conn.prepareStatement(insert_query);

            memstm.setString(1, idkey);
            memstm.setLong(2, atlasResult.getExperiment().getDwExpId());
            memstm.setString(3, atlasResult.getExperiment().getDwExpAccession());
            memstm.setString(4, atlasResult.getExperiment().getDwExpDescription());
            memstm.setString(5, atlasResult.getGene().getGeneId());
            memstm.setString(6, atlasResult.getGene().getGeneName());
            memstm.setString(7, atlasResult.getGene().getGeneIdentifier());
            memstm.setString(8, atlasResult.getGene().getGeneSpecies());
            memstm.setString(9, atlasResult.getAtuple().getEf());
            memstm.setString(10, atlasResult.getAtuple().getEfv());
            memstm.setInt   (11, atlasResult.getAtuple().getUpdn());
            memstm.setDouble(12, atlasResult.getAtuple().getPval());
            memstm.setString(13, atlasResult.getGene().getGeneHighlightStringForHtml());

            memstm.execute();
            eltCount++;
            memstm.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }
    }

    public String getIdkey() {
        return this.idkey;
    }

    public boolean isAvailableInDB() {
        boolean isAvailableInDB = false;

        Connection conn = null;

        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM ATLAS where idkey=?");
            stmt.setString(1, idkey);

            ResultSet rs = stmt.executeQuery();

            if(null != rs && rs.next() && rs.getInt(1) != 0 )
                isAvailableInDB = true;

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        return isAvailableInDB;
    }

//    public void setFullTextGenes(QueryResponse geneHitsResponse) {
//        this.geneHitsResponse = geneHitsResponse;
//    }
//
//    public void setFullTextExpts(QueryResponse exptHitsResponse) {
//        this.exptHitsResponse = exptHitsResponse;
//    }

    public int size() {
        return eltCount;
    }

    public boolean equals(AtlasResultSet obj) {
        return obj.getIdkey().equals(idkey);
    }

    public String getSearchKey() {
        return searchkey;
    }

    public void setIdkey(String idkey) {
        this.idkey = idkey;
    }
}
