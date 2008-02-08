package ae3.servlet; /**
 * User: ostolop
 * Date: 07-Feb-2008
 *
 * EBI Microarray Informatics Team (c) 2007 
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.solr.servlet.DirectSolrConnection;

public class ApplicationListener implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {

    private Log log = LogFactory.getLog("ae3");

    // Public constructor is required by servlet spec
    public ApplicationListener() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {
        /* This method is called when the servlet context is
           initialized(when the Web application is deployed).
           You can initialize servlet context related data here.
        */

        ServletContext sc = sce.getServletContext();

        String solr_gene_instance = sc.getInitParameter("gene_idx");
        String solr_expt_instance = sc.getInitParameter("expt_idx");

        try {
            DirectSolrConnection solr_gene = new DirectSolrConnection(solr_gene_instance, solr_gene_instance + "/data", null);
            DirectSolrConnection solr_expt = new DirectSolrConnection(solr_expt_instance, solr_expt_instance + "/data", null);

            sc.setAttribute("solr_gene", solr_gene);
            sc.setAttribute("solr_expt", solr_expt);
        } catch (Exception e) {
            log.error("Error creating direct SOLR connections", e);
        }

        // put datasource in servlet context
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            DataSource ds = (DataSource) envContext.lookup("jdbc/AEDWD");

            sc.setAttribute("aewds", ds);
        } catch (NamingException ne) {
            log.error(ne);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context
           (the Web application) is undeployed or 
           Application Server shuts down.
        */

        ServletContext sc = sce.getServletContext();

        DirectSolrConnection solr_gene = (DirectSolrConnection) sc.getAttribute("solr_gene");
        DirectSolrConnection solr_expt = (DirectSolrConnection) sc.getAttribute("solr_expt");

        try {
            if (solr_gene != null)
                solr_gene.close();

            if (solr_expt != null)
                solr_expt.close();
        } catch (Exception e) {
            log.error("Error closing SOLR indexes", e);
        }


        sc.removeAttribute("solr_gene");
        sc.removeAttribute("solr_expt");
        sc.removeAttribute("aewds");
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
        /* Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        /* Session is destroyed. */
    }

    // -------------------------------------------------------
    // HttpSessionAttributeListener implementation
    // -------------------------------------------------------

    public void attributeAdded(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute 
           is added to a session.
        */
    }

    public void attributeRemoved(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute
           is removed from a session.
        */
    }

    public void attributeReplaced(HttpSessionBindingEvent sbe) {
        /* This method is invoked when an attibute
           is replaced in a session.
        */
    }
}
