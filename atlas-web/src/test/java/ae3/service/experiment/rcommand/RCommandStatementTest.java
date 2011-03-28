package ae3.service.experiment.rcommand;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;

/**
 * @author Olga Melnichuk
 *         Date: 28/03/2011
 */
public class RCommandStatementTest {
    @Test
    public void rCommandStatementSyntaxTest() {
        assertEquals("test()", new RCommandStatement("test").toString());
        assertEquals("test(5)", new RCommandStatement("test").addParam(5).toString());
        assertEquals("test('a string')", new RCommandStatement("test").addParam("a string").toString());
        assertEquals("test(c())", new RCommandStatement("test").addParam(Collections.<Object>emptyList()).toString());
        assertEquals("test(c(1))", new RCommandStatement("test").addParam(Arrays.asList(1)).toString());
        assertEquals("test(c(1,2))", new RCommandStatement("test").addParam(Arrays.asList(1, 2)).toString());
        assertEquals("test(c('one'))", new RCommandStatement("test").addParam(Arrays.asList("one")).toString());
        assertEquals("test(c('one','two'))", new RCommandStatement("test").addParam(Arrays.asList("one", "two")).toString());
        assertEquals("test(5,1)", new RCommandStatement("test").addParam(5).addParam(1).toString());
        assertEquals("test(5,'one')", new RCommandStatement("test").addParam(5).addParam("one").toString());
        assertEquals("test(5,'one',c(1,2))", new RCommandStatement("test").addParam(5).addParam("one").addParam(Arrays.asList(1,2)).toString());
    }
}
