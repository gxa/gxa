package ae3.service;

import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * Created by IntelliJ IDEA.
* User: ostolop
* Date: 06-Apr-2008
* Time: 18:57:29
* To change this template use File | Settings | File Templates.
*/
public class AtlasResultSet implements Serializable {
    private static final Log log = LogFactory.getLog(AtlasResultSet.class);
    private static final String insert_query = "insert into atlas (idkey, experiment_id, experiment_accession, experiment_description, gene_id, gene_name, gene_identifier, gene_species, ef, efv, updn, updn_pvaladj, gene_highlights) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private String idkey;
    private boolean isAvailableInDB;
    private int eltCount = 0;

//    private QueryResponse geneHitsResponse;
//    private QueryResponse exptHitsResponse;

    public AtlasResultSet() {
        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            ResultSet idrs = conn.prepareStatement("SELECT RANDOM_UUID()").executeQuery();
            idrs.next();
            idkey = idrs.getString(1);
            idrs.close();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        if (idkey == null) throw new ExceptionInInitializerError();
    }

    public List<HashMap> getAtlasEfvCounts() {
        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT efv || ' (' || ef || ')' as efvef, " +
                            "count(distinct experiment_id) AS ce, " +
                            "group_concat(distinct experiment_accession separator ', ') AS experiments, " +                            
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
                HashMap<String,String> h = new HashMap<String,String>();
                h.put("efv", rs.getString("efvef"));
                h.put("experiment_count", rs.getString("ce"));
                h.put("experiments", rs.getString("experiments"));
                h.put("up_count", rs.getString("sumup"));
                h.put("dn_count", rs.getString("sumdn"));

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

        setAvailableInDB(false);
    }

    public List<HashMap> getAtlasResultGenes() {
        List<HashMap> ars = new Vector<HashMap>();

        Connection conn = null;
        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT gene_name, gene_identifier, avg(updn_pvaladj) as mpv " +
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
                    " sum(casewhen(updn=-1,1,0)) as sumdn " +
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
                h.put("sumup", rs.getString("sumup"));
                h.put("sumdn", rs.getString("sumdn"));

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
        return isAvailableInDB;
    }

    public void setAvailableInDB(boolean availableInDB) {
        isAvailableInDB = availableInDB;
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
}
