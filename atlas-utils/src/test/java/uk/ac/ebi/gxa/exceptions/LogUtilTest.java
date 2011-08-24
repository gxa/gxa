package uk.ac.ebi.gxa.exceptions;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class LogUtilTest {
    @Test(expected = UnexpectedException.class)
    public void testStackTrace() {
        BasicConfigurator.configure();
        throw LogUtil.createUnexpected("OMG!");
    }
}
