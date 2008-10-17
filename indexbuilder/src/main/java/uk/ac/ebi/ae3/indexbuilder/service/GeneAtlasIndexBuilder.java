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
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;


public class GeneAtlasIndexBuilder extends IndexBuilderService {
    protected final Log log = LogFactory.getLog(getClass());

    private BasicDataSource dataSource;

    public GeneAtlasIndexBuilder() throws ParserConfigurationException, IOException, SAXException
    {

    }      

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static String encodeEfv(String v) {
        try {
            StringBuffer r = new StringBuffer();
            for(char x : v.toCharArray())
            {
                if(Character.isJavaIdentifierPart(x))
                    r.append(x);
                else
                    for(byte b : Character.toString(x).getBytes("UTF-8"))
                        r.append("_").append(String.format("%x", b));
            }
            return r.toString();
        } catch(UnsupportedEncodingException e){
            throw new IllegalArgumentException("Unable to encode EFV in UTF-8", e);
        }
    }

    protected void createIndexDocs() throws Exception {
        Connection sql = getDataSource().getConnection();

        PreparedStatement listAtlasGenesStmt = sql.prepareStatement("select agenes.gene_id_key, xml.solr_xml.GETCLOBVAL() " +
                "from (select distinct gene_id_key from atlas where gene_id_key<>0 and updn<>0) agenes, gene_xml xml where agenes.gene_id_key = xml.gene_id_key");
        PreparedStatement countGeneEfvStmt = sql.prepareStatement("select ef,efv,updn," +
                "avg(round(updn_pvaladj,100)) as avgpval," +
                "count(distinct experiment_id_key) as cnt " +
                "from atlas where gene_id_key = ? and updn<>0  group by ef,efv,updn having count(distinct experiment_id_key) > 0");
        PreparedStatement countGeneExpStmt = sql.prepareStatement("select experiment_id_key,updn," +
                "avg(round(updn_pvaladj,100)) as avgpval " +
                "from atlas where gene_id_key = ? and updn<>0 group by experiment_id_key,updn");

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
            List<Element> fields = xmlDoc.getRootElement().elements("field");
            for(Element field : fields) {
                solrDoc.addField(field.attribute("name").getValue(), field.getText());
            }

            countGeneEfvStmt.setString(1, geneId);
            ResultSet efvs = countGeneEfvStmt.executeQuery();
            while(efvs.next()) {
                String ef = efvs.getString(1);
                String efv = efvs.getString(2);
                if(efv.equals("V1") || ef.equals("V1"))
                    continue;
                String updn = efvs.getString(3).equals("-1") ? "dn" : "up";
                String efvid = encodeEfv(ef) + "_" + encodeEfv(efv) + "_" + updn;
                solrDoc.addField("cnt_efv_" + efvid, efvs.getInt(5));
                solrDoc.addField("avgpval_efv_" + efvid, efvs.getDouble(4));
                solrDoc.addField("efvs_" + updn + "_" + encodeEfv(ef), efv);
            }
            efvs.close();

            countGeneExpStmt.setString(1, geneId);
            ResultSet exps = countGeneExpStmt.executeQuery();
            while(exps.next()) {
                String expId = exps.getString(1);
                String updn = exps.getString(2).equals("-1") ? "dn" : "up";
                solrDoc.addField("exp_" + updn + "_ids", expId);
                solrDoc.addField("avgpval_exp_" + expId + "_" + updn, exps.getDouble(3));
            }
            exps.close();
            getSolrEmbeddedIndex().addDoc(solrDoc);
        }
        genes.close();
        listAtlasGenesStmt.close();
        countGeneEfvStmt.close();
        countGeneExpStmt.close();
        log.info("Finished, committing");
    }
}