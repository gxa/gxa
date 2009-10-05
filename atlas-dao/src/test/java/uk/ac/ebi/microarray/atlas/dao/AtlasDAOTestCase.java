package uk.ac.ebi.microarray.atlas.dao;

import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abstract TestCase useful for setting up an in memory (hypersonic) database,
 * and creating a DBUnit environment for testing DAO methods and other stuff.
 *
 * @author Tony Burdett
 * @date 05-Oct-2009
 */
public abstract class AtlasDAOTestCase extends DBTestCase {
  private static final String ATLAS_DATA_RESOURCE = "atlas-db.xml";

  private static final String DRIVER = "org.hsqldb.jdbcDriver";
  private static final String URL = "jdbc:hsqldb:test:atlas";
  private static final String USER = "sa";
  private static final String PASSWD = "";

  private DataSource testDataSource;
  private AtlasDAO atlasDAO;

  public AtlasDAO getAtlasDAO() {
    if (atlasDAO != null) {
      return atlasDAO;
    }
    else {
      fail();
      return null;
    }
  }

  protected IDataSet getDataSet() throws Exception {
    InputStream in = this.getClass().getClassLoader().
        getResourceAsStream(ATLAS_DATA_RESOURCE);

    return new FlatXmlDataSet(in);
  }

  /**
   * This sets up an in-memory database using Hypersonic, and uses DBUnit to
   * dump sample data form atlas-db.xml into this in-memory database.  It then
   * configures a SingleConnectionDataSource from spring to provide access to
   * the underlying DB connection.  Finally, it initialises a JdbcTemplate using
   * this datasource, and an AtlasDAO using this template.  After setup, you
   * should be able to use atlasDAO to test method calls against the data
   * configured in the sample dataset, or add to it and check the resulting
   * data.
   */
  protected void setUp() throws Exception {
    // set system properties for hsqldb
    System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS,
                       DRIVER);
    System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL,
                       URL);
    System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME,
                       USER);
    System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD,
                       PASSWD);

    // do dbunit setup
    super.setUp();

    // do our setup
    testDataSource = new SingleConnectionDataSource(
        getConnection().getConnection(), false);
    atlasDAO = new AtlasDAO();
    atlasDAO.setJdbcTemplate(new JdbcTemplate(testDataSource));
  }

  protected void tearDown() throws Exception {
    // do our teardown
    testDataSource = null;
    atlasDAO = null;

    // do dbunit teardown
    super.tearDown();

    // do our teardown
    System.clearProperty(
        PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS);
    System.clearProperty(
        PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL);
    System.clearProperty(
        PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME);
    System.clearProperty(
        PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @BeforeClass
  private void createDatabase() throws SQLException, ClassNotFoundException {
    // Load the HSQL Database Engine JDBC driver
    Class.forName("org.hsqldb.jdbcDriver");

    // get a database connection, that will create the DB if it doesn't exist yet
    Connection conn = DriverManager.getConnection(URL, USER, PASSWD);

    // equivalent of a2_experiment.sql for hsqldb
    runStatement(conn,
                 "  CREATE TABLE A2_EXPERIMENT " +
                     "(EXPERIMENTID INTEGER IDENTITY, " +
                     "ACCESSION CHAR, " +
                     "DESCRIPTION CHAR, " +
                     "PERFORMER CHAR, " +
                     "LAB CHAR, " +
                     "LOADDATE DATE, " +
                     "CONSTRAINT SYS_C008053 PRIMARY KEY (EXPERIMENTID)) ;");
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @AfterClass
  private void destroyDatabase() throws SQLException {
    Connection conn = DriverManager.getConnection(URL, USER, PASSWD);

    runStatement(conn, "SHUTDOWN");
  }

  private void runStatement(Connection conn, String sql) throws SQLException {
    Statement st = conn.createStatement();
    st.executeUpdate(sql);
    st.close();
  }
}
