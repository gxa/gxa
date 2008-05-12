/**
 * 
 */   
package uk.ac.ebi.ae3.web;

import java.io.IOException;
import java.io.PrintWriter;
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
import org.dom4j.DocumentHelper;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexException;

import com.Ostermiller.util.StringTokenizer;
/**
 * 
 * Class description goes here.
 * @deprecated
 * @version 	1.0 2008-04-02
 * @author 	Miroslaw Dylag
 */
public class ExpQueryServlet extends HttpServlet {

    final Logger log = Logger.getLogger(ExpQueryServlet.class.getName());
    private final static String PAR_KEYWORDS="keywords";
    private IndexQueryService indexQueryService;
    private String multicoredir;
    public ExpQueryServlet() 
    {
	
    }
    @Override
    @Deprecated
    public void init() throws ServletException {
        // TODO Auto-generated method stub
        super.init();
        log.info("Init servlet " + getServletName());
        this.multicoredir = getInitParameter("multicoredir");
        this.indexQueryService = new IndexQueryService(this.multicoredir);
        try {
	    this.indexQueryService.init();
	} catch (ParserConfigurationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SAXException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IndexException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
        //init of SolrServlet
        
    }
    
    @Override
    @Deprecated
    public void destroy() {
        // TODO Auto-generated method stub
        super.destroy();
        log.info("Destroy servlet " + getServletName());
        this.indexQueryService.dispose();
        
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doPost(req, resp);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        log.info("Invoking doGet method");
        request(req, resp);
        
    }
    
    private void request(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
    {
	String keyword=req.getParameter("keyword");
        PrintWriter out=resp.getWriter();
        resp.setContentType("text/xml");       
	try
	{
        //Create table
	    if (!StringUtils.isEmpty(keyword))
	    {
	       StringTokenizer tok = new StringTokenizer(keyword,"+");
	       String keywords[]=tok.toArray();
	       log.info("Keward is " + keywords.toString());
	       this.indexQueryService.printExperiments(keywords, out);
	    }
	    else
	    {
		out.println("<xml></xml>");
	    }
	 } catch (SolrServerException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    finally
	    {
	      
	       out.close();            
		
	    }
	   
	        	
    }
}
