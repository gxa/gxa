/**
 * 
 */   
package ae3.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexException;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.service.ArrayExpressSearchService;
import ae3.service.XmlHelper;

import com.Ostermiller.util.StringTokenizer;
/**
 * 
 * Class description goes here.
 * Examples
 * 1) return the XML document which contains only kyeword elements and counjt attribute
 * http://localhost:8082/ae3/expQuery?keywords=cancer&option=count * 
 * 2) Returns XML document which contains experiment  *  
 * http://localhost:8082/ae3/expQuery?keywords=cancer&option=get&start=0&rows=10
 * 
 * @version 	1.0 2008-04-02
 * @author 	Miroslaw Dylag
 */
public class ExpQueryServlet extends HttpServlet {

    final Logger log = Logger.getLogger(ExpQueryServlet.class.getName());
    private final static String PAR_KEYWORDS="keywords";
	public enum OptionValue { CLUBS, DIAMONDS, HEARTS, SPADES };
	private OptionValue option;

    private String multicoredir;
    public ExpQueryServlet() 
    {
	
    }
    @Override
    @Deprecated
    public void init() throws ServletException {
        super.init();
        log.info("Init servlet " + getServletName());        
    }
    
    @Override
    @Deprecated
    public void destroy() {
        // TODO Auto-generated method stub
        super.destroy();
        log.info("Destroy servlet " + getServletName());
        
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        log.info("Invoking doGet method");
        request(req, resp);
        
    }
    
    private void request(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
    {
    	String keyword=req.getParameter("keywords");
    	String option = req.getParameter("option");
    	StringTokenizer tok = new StringTokenizer(keyword," ");
    	String keywords[]=tok.toArray();
    	if (option.equalsIgnoreCase("get"))
    	{
        	String start = req.getParameter("start");
        	String rows = req.getParameter("rows");
    		doSearchGet(req,resp,keywords,start,rows);
    	}
    	//count
    	else
    	{
    		doSearchCount(req, resp, keywords);
    	}
    
        System.out.println("Keyword:" + keyword);
        System.out.println("Option:" + keyword);
    	
	        	
    }
    
    private void doSearchCount(HttpServletRequest req, HttpServletResponse resp, String[] keywords) throws IOException
    {
    	try
        {
            PrintWriter out=resp.getWriter();
            resp.setContentType("text/xml");
            AtlasDao dao = new AtlasDao();
        	long count = dao.getExperimentsCount(keywords);
        	Document doc = XmlHelper.createXmlDoc(keywords, Long.toString(count));
            out.write(doc.asXML());
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        //run searching
        //invoke create xml document
	   
    	
    }
    
    private void doSearchGet(HttpServletRequest req, HttpServletResponse resp, String[] keywords, String start, String rows)
    {
    	try
        {
            PrintWriter out=resp.getWriter();
            resp.setContentType("text/xml");
            //AtlasDao dao = new AtlasDao();
        	long count = AtlasDao.getExperimentsCount(keywords);
        	//get experiments
        	int _start = Integer.parseInt(start);
        	int _rows = Integer.parseInt(rows);        	
        	List <AtlasExperiment> exps=AtlasDao.getExperiments(keywords, _start, _rows);
        	Document doc=XmlHelper.createXmlDoc(exps, keywords, Long.toString(count), start, rows);
            out.write(doc.asXML());
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        //run searching
    	
    }
}
