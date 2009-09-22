package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * An {@link IndexBuilderService} that generates index documents from the
 * experiments in the Atlas database.
 *
 * @author Tony Burdett
 * @date 22-Sep-2009
 */
public class ExperimentAtlasIndexBuilder extends IndexBuilderService {
  public ExperimentAtlasIndexBuilder(AtlasDAO atlasDAO,
                                     EmbeddedSolrServer solrServer) {
    super(atlasDAO, solrServer);
  }

  protected void createIndexDocs() throws IndexBuilderException {
    try {
      // fetch experiments - check if we want all or only the pending ones
      List<Experiment> experiments = getPendingOnly()
          ? getAtlasDAO().getAllPendingExperiments()
          : getAtlasDAO().getAllExperiments();

      for (Experiment experiment : experiments) {
        // Create a new solr document
        SolrInputDocument solrInputDoc = new SolrInputDocument();

        // Add field "exp_in_dw" = true, to show this experiment is present
        solrInputDoc.addField(Constants.FIELD_EXP_IN_DW, true);

        // then, add all the fields we need from the experiment to the solr document
        // old way was to fetch XML blob from DB and scan this, to generate index data
        // todo - just find out what fields are present in the XML and duplicate from database?
        String exptXML = convertExperimentToXML(experiment);
        Document exptXMLDoc = DocumentHelper.parseText(exptXML);
        List<Element> fields = exptXMLDoc.getRootElement().elements("field");
        for (Element field : fields) {
          solrInputDoc.addField(
              Constants.PREFIX_DWE + field.attribute("name").getValue(),
              field.getText());
        }

        // next, get the genes relating to this experiment
        List<Object> genes = getAtlasDAO()
            .getGenesByExperimentAccession(experiment.getAccession());

        // update the document for each gene
        for (Object gene : genes) {
          // get properties and property values
        }

        // finally, add the document to the solr server
        getSolrServer().add(solrInputDoc);

//        while (gcRS.next()) {
//          String ef = gcRS.getString("EF");
//          String efv = gcRS.getString("EFV");
//          if (efv.equals("V1") || ef.equals("V1")) {
//            continue;
//          }
//          String updn = gcRS.getString("updn");
//          int count = gcRS.getInt("GC");
//          String efvid = encodeStringAsUTF8(ef) + "_" +
//              encodeStringAsUTF8(efv);
//          String expr = updn.equals("-1") ? "_dn" : "_up";
//          doc.addField("cnt_efv_" + efvid + expr, count);
//        }
        getSolrEmbeddedIndex().addDoc(solrInputDoc);
      }
    }
    catch (Exception e) {
      throw new IndexBuilderException(e);
    }
  }

  private static String encodeStringAsUTF8(String unencodedStr)
      throws UnsupportedEncodingException {
    byte[] utf8Bytes = unencodedStr.getBytes("UTF8");
    return new String(utf8Bytes, "UTF8");
  }

  private static String convertExperimentToXML(Experiment experiment) {
    // todo - see getExperimentAsXml() on ExperimentDwJdbcDao, this replaces the fetch from DB
    return "";
  }
}
