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
    	String keyword=req.getParameter("keyword");
        PrintWriter out=resp.getWriter();
        resp.setContentType("text/xml");       
	   
	        	
    }
}
