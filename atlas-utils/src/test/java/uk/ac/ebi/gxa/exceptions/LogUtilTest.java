package uk.ac.ebi.gxa.exceptions;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class LogUtilTest {
    @Test
    public void testStackTrace() {
        BasicConfigurator.configure();
        LogUtil.createUnexpected("OMG!");
    }
}
