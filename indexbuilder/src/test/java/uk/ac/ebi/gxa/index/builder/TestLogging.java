package uk.ac.ebi.gxa.index.builder;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 12-Jan-2010
 */
public class TestLogging extends TestCase {
    private Log log = LogFactory.getLog("test-logger");

    public void testLog() {
        log.info("A test statement to JCL logger");
    }
}
