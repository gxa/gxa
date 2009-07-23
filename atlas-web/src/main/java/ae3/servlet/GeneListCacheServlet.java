package ae3.servlet;

import ae3.util.AtlasProperties;
import ae3.service.ArrayExpressSearchService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.GeneProperties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.lucene.index.IndexReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.sun.org.apache.xpath.internal.NodeSet;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Jul 21, 2009
 * Time: 3:35:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class GeneListCacheServlet extends HttpServlet{

    public static final int PageSize = 1000;

    final private Logger log = LoggerFactory.getLogger(getClass());

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    protected String basePath;

    private static String getFileName(){
        String BasePath = System.getProperty("java.io.tmpdir");

        final String geneListFileName = BasePath + File.separator + "geneNames.xml";

        return geneListFileName;
    }

    public static boolean done = false;

    @Override
    public void init() throws ServletException {

        new Thread() { public void run() {

            BufferedOutputStream bfind = null;

            try {

                bfind = new BufferedOutputStream(new FileOutputStream(getFileName()));

                String letters = "0abcdefghigklmnopqrstuvwxyz";

                bfind.write("<r>".getBytes());

                for(int i=0; i!= letters.length(); i++ )
                {
                    String prefix = String.valueOf(letters.charAt(i));

                    Collection<AutoCompleteItem> Genes = QueryIndex(prefix, PageSize); 

                    if(prefix.equals("0"))
                        prefix = "num";

                    for(AutoCompleteItem j : Genes){
                         String geneName = j.getValue();

                        bfind.write(String.format("<%1$s>%2$s</%1$s>\n", prefix, geneName).getBytes());
                    }
                }
                
                bfind.write("</r>".getBytes());
            }
            catch(Exception ex){
                log.info("ERROR creating gene names cache:"+ ex.getMessage());
                
            }
            finally {
                if(null!=bfind)
                try{
                    bfind.close();
                    done = true;
                }
                catch(Exception Ex)
                {
                    //no op
                }
            }
        } }.start();
    }

    public static Collection<AutoCompleteItem> getGenes(String prefix, Integer recordCount) throws Exception{

        try{

            if ((!done)|(recordCount > PageSize))
                return QueryIndex(prefix, recordCount);

            else{
                Collection<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();

                XPath xpath = XPathFactory.newInstance().newXPath();

                    if(prefix.equals("0"))
                        prefix = "num";

                String expression = "/r/" +prefix;
                InputSource inputSource = new InputSource(getFileName()); //

                Object nodes1 = xpath.evaluate(expression, inputSource, XPathConstants.NODESET);

                NodeList nodes = (NodeList) nodes1;

                    for(int i=0; i!= nodes.getLength(); i++)
                    {
                        Node n = nodes.item(i);

                        String name = n.getTextContent();

                        Long count = 1L;

                        AutoCompleteItem ai = new AutoCompleteItem(name,name,name,count);

                        result.add(ai);
                    }

               return result;
            }

        }
        catch(Exception ex)
        {
           ///for breakpoint only
            throw ex;
        }
    }

    private static Collection<AutoCompleteItem> QueryIndex(String prefix, Integer recordCount) throws Exception{

        AtlasStructuredQueryService service = ae3.service.ArrayExpressSearchService.instance().getStructQueryService();

        Collection<AutoCompleteItem> Genes = service.getGeneListHelper().autoCompleteValues(GeneProperties.GENE_PROPERTY_NAME,prefix,recordCount,null);

        //AZ:2008-07-07 "0" means all numbers
        if(prefix.equals("0"))
        {
            for(int i =1; i!=10; i++ )
            {
                Genes.addAll((service.getGeneListHelper().autoCompleteValues(GeneProperties.GENE_PROPERTY_NAME,String.valueOf(i) ,recordCount,null)));
            }
            
        }

        return new LinkedHashSet<AutoCompleteItem>(Genes);
    }
}

