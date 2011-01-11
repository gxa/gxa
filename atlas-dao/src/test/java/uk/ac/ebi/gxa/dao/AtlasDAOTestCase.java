/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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
import java.sql.*;

/**
 * Abstract TestCase useful for setting up an in memory (hypersonic) database, and creating a DBUnit environment for
 * testing DAO methods and other stuff.
 *
 * @author Tony Burdett
 */
public abstract class AtlasDAOTestCase extends DBTestCase {
    private static final String ATLAS_DATA_RESOURCE = "atlas-db.xml";

    private static final String DRIVER = "org.hsqldb.jdbcDriver";
    private static final String URL = "jdbc:hsqldb:mem:atlas";
    private static final String USER = "sa";
    private static final String PASSWD = "";

    private DataSource atlasDataSource;
    private AtlasDAO atlasDAO;

    public AtlasDAO getAtlasDAO() {
        if (atlasDAO != null) {
            return atlasDAO;
        } else {
            fail("atlasDataSource wasn't set up");
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
        createDatabase();

        // do dbunit setup
        super.setUp();

        // do our setup
        atlasDataSource = new SingleConnectionDataSource(
                getConnection().getConnection(), false);
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

    @BeforeClass
    private void createDatabase() throws SQLException, ClassNotFoundException {
        // Load the HSQL Database Engine JDBC driver
        Class.forName("org.hsqldb.jdbcDriver");

        // get a database connection, that will create the DB if it doesn't exist yet
        Connection conn = DriverManager.getConnection(URL, USER, PASSWD);
        System.out.print("Creating test database tables...");

        runStatement(conn,
                "CREATE TABLE A2_EXPERIMENT " +
                        "(EXPERIMENTID NUMERIC(22) NOT NULL, " +
                        "ABSTRACT VARCHAR(2000), " +
                        "ACCESSION VARCHAR(255), " +
                        "DESCRIPTION VARCHAR(2000), " +
                        "PERFORMER VARCHAR(2000), " +
                        "LAB VARCHAR(2000), " +
                        "LOADDATE DATE, " +
                        "RELEASEDATE DATE, " +
                        "PMID VARCHAR(255), " +
                        "CONSTRAINT SYS_C008053 PRIMARY KEY (EXPERIMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_EXPERIMENTASSET " +
                        "(EXPERIMENTASSETID NUMERIC NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "DESCRIPTION VARCHAR(2000), " +
                        "FILENAME VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C009999 PRIMARY KEY (EXPERIMENTASSETID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ARRAYDESIGN " +
                        "(ARRAYDESIGNID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "TYPE VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "PROVIDER VARCHAR(255), " +
                        "CONSTRAINT SYS_C008062 PRIMARY KEY (ARRAYDESIGNID))");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTY " +
                        "(PROPERTYID NUMERIC NOT NULL, " +
                        "NAME VARCHAR(255), " +
                        "ACCESSION VARCHAR(255), " +
                        "AE1TABLENAME_ASSAY VARCHAR(255), " +
                        "AE1TABLENAME_SAMPLE VARCHAR(255), " +
                        "ASSAYPROPERTYID NUMERIC, " +
                        "SAMPLEPROPERTYID NUMERIC, " +
                        "CONSTRAINT SYS_C008064 PRIMARY KEY (PROPERTYID));");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTYVALUE " +
                        "(PROPERTYVALUEID NUMERIC NOT NULL, " +
                        "PROPERTYID NUMERIC, " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008066 PRIMARY KEY (PROPERTYVALUEID));");

        runStatement(conn,
                "CREATE TABLE A2_ASSAY " +
                        "(ASSAYID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "ARRAYDESIGNID NUMERIC NOT NULL, " +
                        "CONSTRAINT SYS_C008055 PRIMARY KEY (ASSAYID), " +
                        "CONSTRAINT FKA2_ASSAY856724 FOREIGN KEY (ARRAYDESIGNID) " +
                        "REFERENCES A2_ARRAYDESIGN (ARRAYDESIGNID), " +
                        "CONSTRAINT FKA2_ASSAY169476 FOREIGN KEY (EXPERIMENTID) " +
                        "REFERENCES A2_EXPERIMENT (EXPERIMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ASSAYPV " +
                        "(ASSAYPVID NUMERIC NOT NULL, " +
                        "ASSAYID NUMERIC, " +
                        "PROPERTYVALUEID NUMERIC, " +
                        "ISFACTORVALUE INTEGER, " +
                        "CONSTRAINT SYS_C008058 PRIMARY KEY (ASSAYPVID));");

        runStatement(conn,
                "CREATE TABLE A2_SAMPLE " +
                        "(SAMPLEID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "SPECIES VARCHAR(255), " +
                        "CHANNEL VARCHAR(255), " +
                        "CONSTRAINT SYS_C008059 PRIMARY KEY (SAMPLEID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_SAMPLEPV " +
                        "(SAMPLEPVID NUMERIC NOT NULL, " +
                        "SAMPLEID NUMERIC NOT NULL, " +
                        "PROPERTYVALUEID NUMERIC, " +
                        "ISFACTORVALUE INTEGER, " +
                        "CONSTRAINT SYS_C008061 PRIMARY KEY (SAMPLEPVID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ASSAYSAMPLE " +
                        "(ASSAYSAMPLEID NUMERIC NOT NULL, " +
                        "ASSAYID NUMERIC, " +
                        "SAMPLEID NUMERIC, " +
                        "CONSTRAINT SYS_C008067 PRIMARY KEY (ASSAYSAMPLEID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_GENE " +
                        "(GENEID NUMERIC, " +
                        "ORGANISMID NUMERIC NOT NULL, " +
                        "IDENTIFIER VARCHAR(255), " +
                        "NAME VARCHAR(255)) ;");

        runStatement(conn,
                "CREATE TABLE A2_GENEPROPERTY " +
                        "(GENEPROPERTYID NUMERIC NOT NULL, " +
                        "NAME VARCHAR(255), " +
                        "AE2TABLENAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008045 PRIMARY KEY (GENEPROPERTYID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEGPV " +
                        "(GENEGPVID NUMERIC NOT NULL," +
                        "GENEID NUMERIC, " +
                        "GENEPROPERTYVALUEID NUMERIC, " +
                        "VALUE VARCHAR(255), " +
                        "CONSTRAINT SYS_C008049 PRIMARY KEY (GENEGPVID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEPROPERTYVALUE " +
                        "(GENEPROPERTYVALUEID NUMERIC, " +
                        "GENEPROPERTYID NUMERIC, " +
                        "VALUE VARCHAR(255)," +
                        "CONSTRAINT PK_GENEPROPERTYVALUE PRIMARY KEY (GENEPROPERTYVALUEID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ORGANISM " +
                        "(ORGANISMID NUMERIC NOT NULL, " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008043 PRIMARY KEY (ORGANISMID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_DESIGNELEMENT " +
                        "(DESIGNELEMENTID NUMERIC NOT NULL, " +
                        "ARRAYDESIGNID NUMERIC, " +
                        "GENEID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "TYPE VARCHAR(255), " +
                        "ISCONTROL INTEGER, " +
                        "CONSTRAINT SYS_C008063 PRIMARY KEY (DESIGNELEMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_EXPRESSIONVALUE " +
                        "(EXPRESSIONVALUEID NUMERIC NOT NULL, " +
                        "DESIGNELEMENTID NUMERIC NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "ASSAYID NUMERIC NOT NULL, " +
                        "VALUE FLOAT, " +
                        "CONSTRAINT SYS_C008076 PRIMARY KEY (EXPRESSIONVALUEID), " +
                        "CONSTRAINT FKA2_EXPRESS543264 FOREIGN KEY (DESIGNELEMENTID) " +
                        "REFERENCES A2_DESIGNELEMENT (DESIGNELEMENTID));");

        runStatement(conn,
                "CREATE TABLE A2_EXPRESSIONANALYTICS " +
                        "(EXPRESSIONID NUMERIC NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "PROPERTYVALUEID NUMERIC NOT NULL, " +
                        "GENEID NUMERIC, " +
                        "TSTAT FLOAT, " +
                        "PVALADJ FLOAT, " +
                        "FPVAL FLOAT, " +
                        "FPVALADJ FLOAT, " +
                        "DESIGNELEMENTID NUMERIC NOT NULL, " +
                        "CONSTRAINT SYS_C008033 PRIMARY KEY (EXPRESSIONID));");

        runStatement(conn,
                "CREATE TABLE A2_ONTOLOGYMAPPING " +
                        "(EXPERIMENTID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "PROPERTY VARCHAR(255), " +
                        "PROPERTYVALUE VARCHAR(255), " +
                        "ONTOLOGYTERM VARCHAR(255), " +
                        "ONTOLOGYTERMNAME VARCHAR(255), " +
                        "ONTOLOGYTERMID NUMERIC, " +
                        "ONTOLOGYNAME VARCHAR(255), " +
                        "ISSAMPLEPROPERTY BOOLEAN, " +
                        "ISASSAYPROPERTY BOOLEAN, " +
                        "ISFACTORVALUE BOOLEAN)");

        runStatement(conn,
                "CREATE TABLE LOAD_MONITOR " +
                        "(ID NUMERIC NOT NULL, " +
                        "ACCESSION VARCHAR(255), " +
                        "STATUS VARCHAR(255), " +
                        "NETCDF VARCHAR(255), " +
                        "SIMILARITY VARCHAR(255), " +
                        "RANKING VARCHAR(255), " +
                        "SEARCHINDEX VARCHAR(255), " +
                        "LOAD_TYPE VARCHAR(255), " +
                        "CONSTRAINT TABLE1_PK PRIMARY KEY (ID))");

        runStatement(conn,
                "CREATE TABLE VWEXPRESSIONANALYTICSBYGENE " +
                        "(GENEID NUMERIC NOT NULL, " +
                        "EF VARCHAR(255) NOT NULL, " +
                        "EFV VARCHAR(255) NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "PVALADJ FLOAT NOT NULL, " +
                        "TSTAT FLOAT NOT NULL, " +
                        "EFID NUMERIC NOT NULL, " +
                        "EFVID NUMERIC NOT NULL, " +
                        "DESIGNELEMENTID NUMERIC NOT NULL) ");

        runStatement(conn, "CREATE SCHEMA ATLASLDR AUTHORIZATION sa");

        runStatement(conn, "CREATE PROCEDURE A2_EXPERIMENTSET(" +
                "    IN Accession VARCHAR(255), IN Description VARCHAR(255)," +
                "    IN Performer VARCHAR(255), IN Lab VARCHAR(255)," +
                "    IN PMID VARCHAR(255), IN Abstract VARCHAR(255))\n" +
                "   READS SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.a2ExperimentSet'");

        runStatement(conn, "CREATE PROCEDURE A2_ASSAYSET(\n" +
                "   IN Accession VARCHAR(255), IN ExperimentAccession  VARCHAR(255),\n" +
                "   IN ArrayDesignAccession VARCHAR(255),\n" +
                "   IN Properties CHAR ARRAY, IN ExpressionValues CHAR ARRAY)\n" +
                "  READS SQL DATA\n" +
                "  LANGUAGE JAVA PARAMETER STYLE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.assaySet'");

        runStatement(conn, "CREATE PROCEDURE A2_SAMPLESET(\n" +
                "    IN ExperimentAccession VARCHAR(255), IN SampleAccession VARCHAR(255), " +
                "    IN Assays INT ARRAY, IN Properties INT ARRAY, IN Channel VARCHAR(255))\n" +
                "  READS SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.a2SampleSet'");

        runStatement(conn, "CREATE PROCEDURE ATLASLDR.LOAD_PROGRESS(\n" +
                " IN experiment_accession VARCHAR(255), IN stage VARCHAR(255), " +
                " IN status VARCHAR(255), IN load_type VARCHAR(255))\n" +
                "  READS SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.loadProgress'");

        runStatement(conn, "CREATE FUNCTION A2_SampleOrganism(IN sample_id INT) RETURNS VARCHAR(255) \n" +
                "  NO SQL \n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.a2SampleOrganism'");

        runStatement(conn,
                "CREATE AGGREGATE FUNCTION wm_concat(" +
                        "    IN val VARCHAR(255), IN flag BOOLEAN, " +
                        "    INOUT register VARCHAR(255), INOUT counter INT)\n" +
                        "  RETURNS VARCHAR(512)\n" +
                        "  NO SQL\n" +
                        "  LANGUAGE JAVA\n" +
                        "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.wmConcat'");

        System.out.println("...done!");
        conn.close();
    }

    @SuppressWarnings("unused")
    public static String a2SampleOrganism(int id) {
        return "Sample Organism Placeholder: " + id;
    }

    @SuppressWarnings("unused")
    public static void a2ExperimentSet(Connection conn,
                                       String accession, String description,
                                       String performer, String lab, String pmid, String anAbstract) throws SQLException {
        // this mimics the stored procedure A2_EXPERIMENTSET in the actual DB
        Statement stmt = conn.createStatement();

        // create an experimentid - no oracle id generators here!
        long experimentid = System.currentTimeMillis();

        stmt.executeQuery(
                "INSERT INTO A2_EXPERIMENT(experimentid, accession, description, performer, lab) " +
                        "values (" + experimentid + ", '" + accession + "', '" +
                        description + "', '" + performer + "', '" + lab + "');");
    }

    @SuppressWarnings("unused")
    public static void assaySet(Connection conn,
                                String accession, String experimentAccession,
                                String arrayDesignAccession,
                                Array properties, Array expressionValues)
            throws SQLException {
        // this mimics the stored procedure A2_ASSAYSET in the actual DB

        // lookup ids from accession first
        Statement stmt = conn.createStatement();

        long experimentID = -1;
        long arrayDesignID = -1;
        ResultSet rs = stmt.executeQuery("SELECT e.experimentid " +
                "FROM a2_experiment e " +
                "WHERE e.accession = '" + experimentAccession + "';");
        while (rs.next()) {
            experimentID = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT d.arraydesignid " +
                "FROM a2_arraydesign d " +
                "WHERE d.accession = '" + arrayDesignAccession + "';");
        while (rs.next()) {
            arrayDesignID = rs.getLong(1);
        }
        rs.close();

        // create an assayid - no oracle id generators here!
        long assayid = System.currentTimeMillis();

        stmt.executeQuery(
                "INSERT INTO A2_ASSAY(assayid, accession, experimentid, arraydesignid) " +
                        "values (" + assayid + ", '" + accession + "', '" +
                        experimentID + "', '" + arrayDesignID + "');");

        stmt.close();
    }

    @SuppressWarnings("unused")
    public static void a2SampleSet(Connection conn,
                                   String experimentAccession,
                                   String sampleAccession,
                                   Array assays, Array properties,
                                   String channel) throws SQLException {
        // this mimics the stored procedure A2_SAMPLESET in the actual DB
        Statement stmt = conn.createStatement();

        // create an sampleid - no oracle id generators here!
        long sampleid = System.currentTimeMillis();

        stmt.executeQuery(
                "INSERT INTO A2_SAMPLE(sampleid, accession, channel) " +
                        "values (" + sampleid + ", '" + sampleAccession +
                        "', '" + channel + "');");
    }

    @SuppressWarnings("unused")
    public static String wmConcat(String in, Boolean flag,
                                  String[] register, Integer[] counter) {
        if (flag) {
            if (register[0] == null) {
                return null;
            }
            return register[0];
        }
        if (in == null) {
            return null;
        }
        if (register[0] == null) {
            register[0] = in;
            counter[0] = 1;
        } else {
            register[0] += "," + in;
            counter[0]++;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void loadProgress(Connection conn,
                                    String accession,
                                    String stage,
                                    String status,
                                    String load_type)
            throws Exception {
    }


    @SuppressWarnings("unused")
    @AfterClass
    private void destroyDatabase() throws SQLException, ClassNotFoundException {
        // Load the HSQL Database Engine JDBC driver
        Class.forName("org.hsqldb.jdbcDriver");

        // get a database connection, that will create the DB if it doesn't exist yet
        Connection conn = DriverManager.getConnection(URL, USER, PASSWD);

        runStatement(conn, "SHUTDOWN");
        conn.close();
    }

    private void runStatement(Connection conn, String sql) throws SQLException {
        // just using raw sql here, prior to any dao/jdbctemplate setup
        Statement st = conn.createStatement();
        st.execute(sql);
        st.close();
    }
}