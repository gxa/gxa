import junit.framework.TestCase;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Apr 26, 2010
 * Time: 1:17:20 PM
 * To change this template use File | Settings | File Templates.
 */

public class AtlasInstallerTest {

    private static AtlasInstaller atlasInstaller;

    @BeforeClass
    public static void setUpGlobal() throws Exception{

        BeanFactory factory =
                new ClassPathXmlApplicationContext("applicationContext.xml");

        AtlasInstallerTest.atlasInstaller = (AtlasInstaller) factory.getBean("atlasInstaller");
    }


    @Before
    public void setUp() throws Exception {
        atlasInstaller.install();
    }

    @After
    public void tearDown() throws Exception {


    }

    @Test
    public void testInstall() throws Exception {
        String sql = "select count(1) from a2_experiment";

        int i = atlasInstaller.getJdbcTemplate().queryForInt(sql);

        Assert.assertEquals("experiment count", 1, i);
    }

    @Test
    public void testVwOntology()  throws Exception{
        atlasInstaller.getJdbcTemplate().execute("update CUR_PropertyValue set Value = 'pseudoheart' where Value = 'heart'");
        atlasInstaller.getJdbcTemplate().execute("commit");

        String sql = "select PropertyValue from vwassayproperty where assayid=2 and property='organismpart'";

        String result = (String) atlasInstaller.getJdbcTemplate().queryForObject(sql, String.class);

        Assert.assertEquals("property value", "hertz" , result);
    }
}
