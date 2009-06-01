package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.IndexField;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;


public class GeneAtlasIndexBuilder extends IndexBuilderService {
    protected final Log log = LogFactory.getLog(getClass());

    private BasicDataSource dataSource;
    private PreparedStatement listAtlasGenesStmt;
    private PreparedStatement countGeneEfoStmt;
    private Efo efo;

    public GeneAtlasIndexBuilder() throws ParserConfigurationException, IOException, SAXException
    {
        efo = Efo.getEfo();
    }      

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static short shorten(double d) {
        d = d * 256;
        if(d > Short.MAX_VALUE)
            return Short.MAX_VALUE;
        if(d < Short.MIN_VALUE)
            return Short.MIN_VALUE;
        return (short)d;
    }

    private class UpDnSet {
        Set<Integer> up = new HashSet<Integer>();
        Set<Integer> dn = new HashSet<Integer>();
        Set<Integer> childrenUp = new HashSet<Integer>();
        Set<Integer> childrenDn = new HashSet<Integer>();
        boolean processed = false;
        double minpvalUp = 1;
        double minpvalDn = 1;
        double minpvalChildrenUp = 1;
        double minpvalChildrenDn = 1;

        void addChild(UpDnSet child) {
            childrenUp.addAll(child.childrenUp);
            childrenDn.addAll(child.childrenDn);
            childrenUp.addAll(child.up);
            childrenDn.addAll(child.dn);
            minpvalChildrenDn = Math.min(Math.min(minpvalChildrenDn, child.minpvalChildrenDn), child.minpvalDn);
            minpvalChildrenUp = Math.min(Math.min(minpvalChildrenUp, child.minpvalChildrenUp), child.minpvalUp);
        }
    }

    private class UpDn {
        int cup = 0;
        int cdn = 0;
        double pup = 1;
        double pdn = 1;
    }

    protected void createIndexDocs() throws Exception {
        prepareStatements();

        log.info("Querying genes...");
        ResultSet genes = listAtlasGenesStmt.executeQuery();
        while(genes.next()) {
            SolrInputDocument solrDoc = new SolrInputDocument();
            String geneId = genes.getString(1);

            oracle.sql.CLOB clob = (oracle.sql.CLOB)genes.getClob(2);
            String xml = clob.getSubString(1, (int)clob.length());
            Document xmlDoc = DocumentHelper.parseText(xml);
            Element el = xmlDoc.getRootElement();

            @SuppressWarnings("unchecked")
            List<Element> fields = el.elements("field");
            for(Element field : fields) {
                String fieldName = field.attribute("name").getValue();
                if(!fieldName.equals("gene_experiment"))
                    solrDoc.addField(fieldName, field.getText());
            }

            addEfoCounts(solrDoc, geneId);

            getSolrEmbeddedIndex().addDoc(solrDoc);
        }
        genes.close();
        closeStatements();
        log.info("Finished, committing");
    }

    private void calcChildren(String currentId, Map<String, UpDnSet> efoupdn) {
        UpDnSet current = efoupdn.get(currentId);
        if(current == null) {
            current = new UpDnSet();
            efoupdn.put(currentId, current);
        } else if(current.processed)
            return;

        for(Efo.Term child : efo.getTermChildren(currentId)) {
            calcChildren(child.getId(), efoupdn);
            current.addChild(efoupdn.get(child.getId()));
        }
        
        current.processed = true;
    }

    private <T> Set<T> union(Set<T> a, Set<T> b)
    {
        Set<T> x = new HashSet<T>();
        if(a != null)
            x.addAll(a);
        if(b != null)
            x.addAll(b);
        return x;
    }

    private void addEfoCounts(SolrInputDocument solrDoc, String geneId) throws SQLException {
        Map<String, UpDnSet> efoupdn = new HashMap<String, UpDnSet>();
        Map<String, UpDn> efvupdn = new HashMap<String, UpDn>();
        Set<Integer> upexp = new HashSet<Integer>();
        Set<Integer> dnexp = new HashSet<Integer>();
        Map<String,Set<String>> upefv = new HashMap<String,Set<String>>();
        Map<String,Set<String>> dnefv = new HashMap<String,Set<String>>();

        ExperimentsTable expTable = new ExperimentsTable();

        countGeneEfoStmt.setString(1, geneId);
        ResultSet efos = countGeneEfoStmt.executeQuery();
        while(efos.next()) {
            boolean isUp = efos.getInt("updn") > 0;
            String accessions = efos.getString("accession");
            String[] accession = accessions != null ? accessions.split("[,;]") : null;
            
            Integer experimentId = efos.getInt("experiment_id_key");
            if(experimentId == 0) {
                log.error("Found experimentId=0 for gene " + geneId);
                continue;
            }

            double pval = efos.getDouble("updn_pvaladj");
            final String ef = efos.getString("ef");
            final String efv = efos.getString("efv");

            if(true || accession == null) {
                String efvid = IndexField.encode(ef, efv);
                if(!efvupdn.containsKey(efvid))
                    efvupdn.put(efvid, new UpDn());
                if(isUp) {
                    efvupdn.get(efvid).cup += 1;
                    efvupdn.get(efvid).pup = Math.min(efvupdn.get(efvid).pup, pval);
                    if(!upefv.containsKey(ef))
                        upefv.put(ef, new HashSet<String>());
                    upefv.get(ef).add(efv);
                } else {
                    efvupdn.get(efvid).cdn += 1;
                    efvupdn.get(efvid).pdn = Math.min(efvupdn.get(efvid).pdn, pval);
                    if(!dnefv.containsKey(ef))
                        dnefv.put(ef, new HashSet<String>());
                    dnefv.get(ef).add(efv);
                }
            }

            if(accession != null)
                for(String acc : accession) {
                    String accId = IndexField.encode(acc);

                    if(!efoupdn.containsKey(accession))
                        efoupdn.put(accId, new UpDnSet());
                    if(isUp) {
                        efoupdn.get(accId).up.add(experimentId);
                        efoupdn.get(accId).minpvalUp = Math.min(efoupdn.get(accId).minpvalUp, pval);
                    } else {
                        efoupdn.get(accId).dn.add(experimentId);
                        efoupdn.get(accId).minpvalDn = Math.min(efoupdn.get(accId).minpvalDn, pval);
                    }
                }

            if(isUp) {
                upexp.add(experimentId);
            } else {
                dnexp.add(experimentId);
            }

            expTable.add(ef, efv, accession, experimentId.toString(), isUp, pval);
        }
        efos.close();


        solrDoc.addField("exp_info", expTable.serialize());

        for(String rootId : efo.getRootIds()) {
            calcChildren(rootId, efoupdn);
        }

        for(Map.Entry<String,UpDnSet> e : efoupdn.entrySet()) {
            String accession = e.getKey();
            UpDnSet ud = e.getValue();

            ud.childrenUp.addAll(ud.up);
            ud.childrenDn.addAll(ud.dn);

            int cup = ud.childrenUp.size();
            int cdn = ud.childrenDn.size();

            double pup = Math.min(ud.minpvalChildrenUp, ud.minpvalUp);
            double pdn = Math.min(ud.minpvalChildrenDn, ud.minpvalDn);

            if(cup > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_up", cup);
                solrDoc.addField("minpval_efo_" + accession + "_up", pup);
            }
            if(cdn > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_dn", cdn);
                solrDoc.addField("minpval_efo_" + accession + "_dn", pdn);
            }
            if(ud.up.size() > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_s_up", ud.up.size());
                solrDoc.addField("minpval_efo_" + accession + "_s_up", ud.minpvalUp);
            }
            if(ud.dn.size() > 0) {
                solrDoc.addField("cnt_efo_" + accession + "_s_dn", ud.dn.size());
                solrDoc.addField("minpval_efo_" + accession + "_s_dn", ud.minpvalDn);
            }

            if(cup > 0)
                solrDoc.addField("s_efo_" + accession + "_up", shorten(cup * (1.0 - pup) - cdn * (1.0 - pdn)));
            if(cdn > 0)
                solrDoc.addField("s_efo_" + accession + "_dn", shorten(cdn * (1.0 - pdn) - cup * (1.0 - pup)));
            if(cup + cdn > 0)
                solrDoc.addField("s_efo_" + accession + "_ud", shorten(cup * (1.0 - pup) + cdn * (1.0 - pdn)));

            if(cup > 0)
                solrDoc.addField("efos_up", accession);
            if(cdn > 0)
                solrDoc.addField("efos_dn", accession);
            if(cup + cdn > 0)
                solrDoc.addField("efos_ud", accession);
        }

        for(Map.Entry<String,UpDn> e : efvupdn.entrySet()) {
            String efvid = e.getKey();
            UpDn ud = e.getValue();

            int cup = ud.cup;
            int cdn = ud.cdn;
            double pvup = ud.pup;
            double pvdn = ud.pdn;

            if(cup != 0) {
                solrDoc.addField("cnt_" + efvid + "_up", cup);
                solrDoc.addField("minpval_" + efvid + "_up", pvup);
            }
            if(cdn != 0) {
                solrDoc.addField("cnt_" + efvid + "_dn", cdn);
                solrDoc.addField("minpval_" + efvid + "_dn", pvdn);
            }

            solrDoc.addField("s_" + efvid + "_up", shorten(cup * (1.0 - pvup) - cdn * (1.0 - pvdn)));
            solrDoc.addField("s_" + efvid + "_dn", shorten(cdn * (1.0 - pvdn) - cup * (1.0 - pvup)));
            solrDoc.addField("s_" + efvid + "_ud", shorten(cup * (1.0 - pvup) + cdn * (1.0 - pvdn)));

        }

        for(Integer i : upexp)
            solrDoc.addField("exp_up_ids", i.toString());
        for(Integer i : dnexp)
            solrDoc.addField("exp_dn_ids", i.toString());

        for(Integer i : union(upexp,dnexp))
            solrDoc.addField("exp_ud_ids", i.toString());

        for(Map.Entry<String,Set<String>> e : upefv.entrySet())
            for(String i : e.getValue())
                solrDoc.addField("efvs_up_" + IndexField.encode(e.getKey()), i);

        for(Map.Entry<String,Set<String>> e : dnefv.entrySet())
            for(String i : e.getValue())
                solrDoc.addField("efvs_dn_" + IndexField.encode(e.getKey()), i);

        for(String factor : union(upefv.keySet(), dnefv.keySet()))
            for(String i : union(upefv.get(factor), dnefv.get(factor)))
                solrDoc.addField("efvs_ud_" + IndexField.encode(factor), i);
    }

    private void closeStatements() throws SQLException {
        Connection sql = getDataSource().getConnection();
        PreparedStatement efoTableStmt = sql.prepareStatement("drop table ontology_mapping");
        try {
            efoTableStmt.execute();
            efoTableStmt.close();
        } catch (SQLException e) {
            // it's ok
        }
        listAtlasGenesStmt.close();
    }

    private void prepareStatements() throws SQLException {
        Connection sql = getDataSource().getConnection();

        listAtlasGenesStmt = sql.prepareStatement("select agenes.gene_id_key, xml.solr_xml.GETCLOBVAL() " +
                "from (select distinct gene_id_key from atlas where gene_id_key<>0 and updn<>0) agenes, gene_xml xml " +
                "where " +
//                "xml.gene_id_key in (170031719,169992680,169990246,153072733,153070758,170037497,160591323,221532282,531073730,220879651,153080879,153073444, 153078307, 153080131, 153080879, 153080879) and " +
                "agenes.gene_id_key = xml.gene_id_key" +
                (isUpdateMode() ? " and xml.status<>'fresh' and xml.status is not null" : ""));

        countGeneEfoStmt = sql.prepareStatement(
                "SELECT o.accession, a.experiment_id_key, a.updn_pvaladj, a.updn, a.ef, a.efv" +
                        " FROM " +
                        "  (SELECT ef, efv, updn, experiment_id_key, updn_pvaladj, " +
                        "     dense_rank() over(PARTITION BY gene_id_key, experiment_id_key, ef, efv ORDER BY updn_pvaladj) AS r " +
                        "   FROM atlas " +
                        "   WHERE gene_id_key = ?) a left outer join (select ef, efv, experiment_id_key, string_agg(accession) as accession from ontology_mapping group by ef, efv, experiment_id_key) o " +
                        "   on o.ef = LOWER(a.ef) AND o.efv = a.efv AND a.efv <> 'V1' AND a.experiment_id_key > 0 AND o.experiment_id_key = a.experiment_id_key " +
                        " WHERE r=1"
                );

//        PreparedStatement efoTableStmt = sql.prepareStatement("drop table ontology_mapping");
//        try {
//            efoTableStmt.execute();
//            efoTableStmt.close();
//        } catch (SQLException e) {
//            // it's ok
//        }
//
//        efoTableStmt = sql.prepareStatement(
//                "create global temporary table ontology_mapping ON COMMIT PRESERVE ROWS as " +
//                        "SELECT DISTINCT s.experiment_id_key," +
//                        "     LOWER(SUBSTR(oa.orig_value_src,    instr(oa.orig_value_src,    '_',    1,    3) + 1,    instr(oa.orig_value_src,    '__DM',    1,    1) -instr(oa.orig_value_src,    '_',    1,    3) -1)) ef," +
//                        "     oa.orig_value AS efv," +
//                        "     oa.accession," +
//                        "     oa.term" +
//                        "   FROM ontology_annotation oa," +
//                        "     ae1__sample__main s" +
//                        "   WHERE(s.sample_id_key = oa.sample_id_key OR s.assay_id_key = oa.assay_id_key)" +
//                        "   AND oa.ontology_id_key = 575119145");
//        efoTableStmt.execute();
//        efoTableStmt.close();
    }
}