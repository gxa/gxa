import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AtlasInstallerTest {

    private static AtlasInstaller atlasInstaller;

    @BeforeClass
    public static void setUpGlobal() throws Exception {

        BeanFactory factory =
                new ClassPathXmlApplicationContext("applicationContext.xml");

        AtlasInstallerTest.atlasInstaller = (AtlasInstaller) factory.getBean("atlasInstaller");
    }


    @Before
    public void setUp() throws Exception {
        atlasInstaller.install();
    }

    @Test
    public void testInstall() throws Exception {
        String sql = "select count(1) from a2_experiment";

        int i = atlasInstaller.getJdbcTemplate().queryForInt(sql);

        Assert.assertEquals("experiment count", 1, i);
    }

    @Test
    public void testVwOntology() throws Exception {
        atlasInstaller.getJdbcTemplate().execute("update CUR_PropertyValue set Value = 'pseudoheart' where Property = 'organismpart'");
        atlasInstaller.getJdbcTemplate().execute("commit");

        String sql = "select PropertyValue from vwassayproperty where assayid=2 and property='organismpart'";

        String result = atlasInstaller.getJdbcTemplate().queryForObject(sql, String.class);

        Assert.assertEquals("property value", "pseudoheart", result);

        atlasInstaller.getJdbcTemplate().execute("update CUR_PropertyValue set Value = 'heart' where Property = 'organismpart'");
        atlasInstaller.getJdbcTemplate().execute("commit");
    }
}
