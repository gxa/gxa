package uk.ac.ebi.gxa.dao;

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
 * Abstract TestCase useful for setting up an in memory (hypersonic) database, and creating a DBUnit environment for
 * testing DAO methods and other stuff.
 *
 * @author Tony Burdett
 * @date 05-Oct-2009
 */
public abstract class AtlasDAOTestCase extends DBTestCase {
    private static final String ATLAS_DATA_RESOURCE = "atlas-db.xml";

    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";//"org.hsqldb.jdbcDriver";
    private static final String URL = "jdbc.oracle.thin:@apu.ebi.ac.uk:1521:AEDWT"; //jdbc:hsqldb:mem:atlas";
    private static final String USER = "atlas2";//"sa";
    private static final String PASSWD = "atlas2";//"";

    private DataSource atlasDataSource;
    private AtlasDAO atlasDAO;

    public DataSource getDataSource() {
        if (atlasDataSource != null) {
            return atlasDataSource;
        }
        else {
            fail();
            return null;
        }
    }

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
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_DATA_RESOURCE);

        return new FlatXmlDataSet(in);
    }

    /**
     * This sets up an in-memory database using Hypersonic, and uses DBUnit to dump sample data form atlas-db.xml into
     * this in-memory database.  It then configures a SingleConnectionDataSource from spring to provide access to the
     * underlying DB connection.  Finally, it initialises a JdbcTemplate using this datasource, and an AtlasDAO using
     * this template.  After setup, you should be able to use atlasDAO to test method calls against the data configured
     * in the sample dataset, or add to it and check the resulting data.
     */
    protected void setUp() throws Exception {
        // set system properties for hsqldb
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, DRIVER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, URL);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, USER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, PASSWD);

        // create a new database
        // createDatabase();

        // do dbunit setup
        // super.setUp();

        // do our setup
        atlasDataSource = new SingleConnectionDataSource(
                getOracleConnection(), false);
                //getConnection().getConnection(), false);
        atlasDAO = new AtlasDAO();
        atlasDAO.setJdbcTemplate(new JdbcTemplate(atlasDataSource));
    }

    protected void tearDown() throws Exception {
        // do our teardown
        atlasDataSource = null;
        atlasDAO = null;

        // do dbunit teardown
        super.tearDown();

        // destroy the old DB
        destroyDatabase();

        // do our teardown
        System.clearProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS);
        System.clearProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL);
        System.clearProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME);
        System.clearProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD);
    }

    private Connection getOracleConnection() throws Exception{
        String connectionString="jdbc:oracle:thin:atlas2/atlas2@barney.ebi.ac.uk:1521:AE2TST";
        DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver());
        Connection conn = DriverManager.getConnection(connectionString);

        return conn;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @BeforeClass
    private void createDatabase() throws SQLException, ClassNotFoundException {

        Connection conn = null;
        try{
            conn = getOracleConnection();
        }
        catch(Exception ex){
            conn = null;
        }

        if(1==1)
        return;

        // Load the HSQL Database Engine JDBC driver
        //Class.forName(DRIVER);

        // get a database connection, that will create the DB if it doesn't exist yet
        //Connection conn = DriverManager.getConnection(URL, USER, PASSWD);
        System.out.print("Creating test database tables...");

        runStatement(conn,
                     "CREATE TABLE A2_EXPERIMENT " +
                             "(EXPERIMENTID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "DESCRIPTION CHAR, " +
                             "PERFORMER CHAR, " +
                             "LAB CHAR, " +
                             "LOADDATE DATE, " +
                             "CONSTRAINT SYS_C008053 PRIMARY KEY (EXPERIMENTID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_ARRAYDESIGN " +
                             "(ARRAYDESIGNID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "TYPE CHAR, " +
                             "NAME CHAR, " +
                             "PROVIDER CHAR, " +
                             "CONSTRAINT SYS_C008062 PRIMARY KEY (ARRAYDESIGNID))");

        runStatement(conn,
                     "CREATE TABLE A2_PROPERTY " +
                             "(PROPERTYID INTEGER NOT NULL, " +
                             "NAME CHAR, " +
                             "ACCESSION CHAR, " +
                             "AE1TABLENAME_ASSAY CHAR, " +
                             "AE1TABLENAME_SAMPLE CHAR, " +
                             "ASSAYPROPERTYID INTEGER, " +
                             "SAMPLEPROPERTYID INTEGER, " +
                             "CONSTRAINT SYS_C008064 PRIMARY KEY (PROPERTYID));");

        runStatement(conn,
                     "CREATE TABLE A2_PROPERTYVALUE " +
                             "(PROPERTYVALUEID INTEGER NOT NULL, " +
                             "PROPERTYID INTEGER, " +
                             "NAME CHAR, " +
                             "CONSTRAINT SYS_C008066 PRIMARY KEY (PROPERTYVALUEID));");

        runStatement(conn,
                     "CREATE TABLE A2_ASSAY " +
                             "(ASSAYID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "EXPERIMENTID INTEGER NOT NULL, " +
                             "ARRAYDESIGNID INTEGER NOT NULL, " +
                             "CONSTRAINT SYS_C008055 PRIMARY KEY (ASSAYID), " +
                             "CONSTRAINT FKA2_ASSAY856724 FOREIGN KEY (ARRAYDESIGNID) " +
                             "REFERENCES A2_ARRAYDESIGN (ARRAYDESIGNID), " +
                             "CONSTRAINT FKA2_ASSAY169476 FOREIGN KEY (EXPERIMENTID) " +
                             "REFERENCES A2_EXPERIMENT (EXPERIMENTID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_ASSAYPROPERTYVALUE " +
                             "(ASSAYPROPERTYVALUEID INTEGER NOT NULL, " +
                             "ASSAYID INTEGER, " +
                             "PROPERTYVALUEID INTEGER, " +
                             "ISFACTORVALUE INTEGER, " +
                             "CONSTRAINT SYS_C008058 PRIMARY KEY (ASSAYPROPERTYVALUEID));");

        runStatement(conn,
                     "CREATE TABLE A2_SAMPLE " +
                             "(SAMPLEID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "SPECIES CHAR, " +
                             "CHANNEL CHAR, " +
                             "CONSTRAINT SYS_C008059 PRIMARY KEY (SAMPLEID)) ;");

        runStatement(conn,
                     "  CREATE TABLE A2_SAMPLEPROPERTYVALUE " +
                             "(SAMPLEPROPERTYVALUEID INTEGER NOT NULL, " +
                             "SAMPLEID INTEGER NOT NULL, " +
                             "PROPERTYVALUEID INTEGER, " +
                             "ISFACTORVALUE INTEGER, " +
                             "CONSTRAINT SYS_C008061 PRIMARY KEY (SAMPLEPROPERTYVALUEID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_ASSAYSAMPLE " +
                             "(ASSAYSAMPLEID INTEGER NOT NULL, " +
                             "ASSAYID INTEGER, " +
                             "SAMPLEID INTEGER, " +
                             "CONSTRAINT SYS_C008067 PRIMARY KEY (ASSAYSAMPLEID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_GENE " +
                             "(GENEID INTEGER, " +
                             "SPECID INTEGER NOT NULL, " +
                             "IDENTIFIER CHAR, " +
                             "NAME CHAR) ;");

        runStatement(conn,
                     "CREATE TABLE A2_GENEPROPERTY " +
                             "(GENEPROPERTYID INTEGER NOT NULL, " +
                             "NAME CHAR, " +
                             "AE2TABLENAME CHAR, " +
                             "CONSTRAINT SYS_C008045 PRIMARY KEY (GENEPROPERTYID)) ;");

        runStatement(conn,
                     "  CREATE TABLE A2_GENEPROPERTYVALUE " +
                             "(GENEPROPERTYVALUEID INTEGER NOT NULL," +
                             "GENEID INTEGER, " +
                             "GENEPROPERTYID INTEGER, " +
                             "VALUE CHAR, " +
                             "CONSTRAINT SYS_C008049 PRIMARY KEY (GENEPROPERTYVALUEID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_SPEC " +
                             "(SPECID INTEGER NOT NULL, " +
                             "NAME CHAR, " +
                             "CONSTRAINT SYS_C008043 PRIMARY KEY (SPECID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_DESIGNELEMENT " +
                             "(DESIGNELEMENTID INTEGER NOT NULL, " +
                             "ARRAYDESIGNID INTEGER, " +
                             "GENEID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "NAME CHAR, " +
                             "TYPE CHAR, " +
                             "ISCONTROL INTEGER, " +
                             "CONSTRAINT SYS_C008063 PRIMARY KEY (DESIGNELEMENTID)) ;");

        runStatement(conn,
                     "CREATE TABLE A2_EXPRESSIONVALUE " +
                             "(EXPRESSIONVALUEID INTEGER NOT NULL, " +
                             "DESIGNELEMENTID INTEGER NOT NULL, " +
                             "EXPERIMENTID INTEGER NOT NULL, " +
                             "ASSAYID INTEGER NOT NULL, " +
                             "VALUE FLOAT, " +
                             "CONSTRAINT SYS_C008076 PRIMARY KEY (EXPRESSIONVALUEID), " +
                             "CONSTRAINT FKA2_EXPRESS543264 FOREIGN KEY (DESIGNELEMENTID) " +
                             "REFERENCES A2_DESIGNELEMENT (DESIGNELEMENTID));");

        runStatement(conn,
                     "CREATE TABLE A2_EXPRESSIONANALYTICS " +
                             "(EXPRESSIONID INTEGER NOT NULL, " +
                             "EXPERIMENTID INTEGER NOT NULL, " +
                             "PROPERTYVALUEID INTEGER NOT NULL, " +
                             "GENEID INTEGER, " +
                             "TSTAT FLOAT, " +
                             "PVALADJ FLOAT, " +
                             "FPVAL FLOAT, " +
                             "FPVALADJ FLOAT, " +
                             "DESIGNELEMENTID INTEGER NOT NULL, " +
                             "CONSTRAINT SYS_C008033 PRIMARY KEY (EXPRESSIONID));");

        runStatement(conn,
                     "CREATE TABLE A2_ONTOLOGYMAPPING " +
                             "(ACCESSION CHAR, " +
                             "PROPERTY CHAR, " +
                             "PROPERTYVALUE CHAR, " +
                             "ONTOLOGYTERM CHAR, " +
                             "ONTOLOGYTERMNAME CHAR, " +
                             "ONTOLOGYTERMID INTEGER, " +
                             "ONTOLOGYNAME CHAR, " +
                             "ISSAMPLEPROPERTY BOOLEAN, " +
                             "ISASSAYPROPERTY BOOLEAN, " +
                             "ISFACTORVALUE BOOLEAN)");

        runStatement(conn,
                     "CREATE TABLE LOAD_MONITOR " +
                             "(ID INTEGER NOT NULL, " +
                             "ACCESSION CHAR, " +
                             "STATUS CHAR, " +
                             "NETCDF CHAR, " +
                             "SIMILARITY CHAR, " +
                             "RANKING CHAR, " +
                             "SEARCHINDEX CHAR, " +
                             "LOAD_TYPE CHAR, " +
                             "CONSTRAINT TABLE1_PK PRIMARY KEY (ID))");

        // testing adding stored procedures
        runStatement(conn,
                     "CREATE ALIAS SQRT FOR \"java.lang.Math.sqrt\"");

        // add real stored procedures
        runStatement(conn,
                     "CREATE ALIAS A2_EXPERIMENTSET FOR " +
                             "\"uk.ac.ebi.gxa.dao.procedures.ExperimentSetter.call\"");
        runStatement(conn,
                     "CREATE ALIAS A2_ASSAYSET FOR " +
                             "\"uk.ac.ebi.gxa.dao.procedures.AssaySetter.call\"");
        runStatement(conn,
                     "CREATE ALIAS A2_SAMPLESET FOR " +
                             "\"uk.ac.ebi.gxa.dao.procedures.SampleSetter.call\"");

        runStatement(conn,
                     "CREATE ALIAS load_progress FOR " +
                             "\"uk.ac.ebi.gxa.dao.procedures.LoadProgress.call\"");

        System.out.println("...done!");
        conn.close();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @AfterClass
    private void destroyDatabase() throws SQLException, ClassNotFoundException {
        // Load the HSQL Database Engine JDBC driver
        //Class.forName("org.hsqldb.jdbcDriver");

        // get a database connection, that will create the DB if it doesn't exist yet
        //Connection conn = DriverManager.getConnection(URL, USER, PASSWD);

        //runStatement(conn, "SHUTDOWN");
        //conn.close();
    }

    private void runStatement(Connection conn, String sql) throws SQLException {
        // just using raw sql here, prior to any dao/jdbctemplate setup
        Statement st = conn.createStatement();
        st.executeUpdate(sql);
        st.close();
    }
}