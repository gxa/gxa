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
    public static final boolean FALSE = false;

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
                        "(EXPERIMENTID NUMERIC NOT NULL, " +
                        "ABSTRACT CHAR, " +
                        "ACCESSION CHAR, " +
                        "DESCRIPTION CHAR, " +
                        "PERFORMER CHAR, " +
                        "LAB CHAR, " +
                        "LOADDATE DATE, " +
                        "PMID CHAR, " +
                        "CONSTRAINT SYS_C008053 PRIMARY KEY (EXPERIMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_EXPERIMENTASSET " +
                        "(EXPERIMENTASSETID NUMERIC NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "DESCRIPTION CHAR, " +
                        "FILENAME CHAR, " +
                        "NAME CHAR, " +
                        "CONSTRAINT SYS_C009999 PRIMARY KEY (EXPERIMENTASSETID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ARRAYDESIGN " +
                        "(ARRAYDESIGNID NUMERIC NOT NULL, " +
                        "ACCESSION CHAR, " +
                        "TYPE CHAR, " +
                        "NAME CHAR, " +
                        "PROVIDER CHAR, " +
                        "CONSTRAINT SYS_C008062 PRIMARY KEY (ARRAYDESIGNID))");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTY " +
                        "(PROPERTYID NUMERIC NOT NULL, " +
                        "NAME CHAR, " +
                        "ACCESSION CHAR, " +
                        "AE1TABLENAME_ASSAY CHAR, " +
                        "AE1TABLENAME_SAMPLE CHAR, " +
                        "ASSAYPROPERTYID NUMERIC, " +
                        "SAMPLEPROPERTYID NUMERIC, " +
                        "CONSTRAINT SYS_C008064 PRIMARY KEY (PROPERTYID));");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTYVALUE " +
                        "(PROPERTYVALUEID NUMERIC NOT NULL, " +
                        "PROPERTYID NUMERIC, " +
                        "NAME CHAR, " +
                        "CONSTRAINT SYS_C008066 PRIMARY KEY (PROPERTYVALUEID));");

        runStatement(conn,
                "CREATE TABLE A2_ASSAY " +
                        "(ASSAYID NUMERIC NOT NULL, " +
                        "ACCESSION CHAR, " +
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
                        "ACCESSION CHAR, " +
                        "SPECIES CHAR, " +
                        "CHANNEL CHAR, " +
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
                        "IDENTIFIER CHAR, " +
                        "NAME CHAR) ;");

        runStatement(conn,
                "CREATE TABLE A2_GENEPROPERTY " +
                        "(GENEPROPERTYID NUMERIC NOT NULL, " +
                        "NAME CHAR, " +
                        "AE2TABLENAME CHAR, " +
                        "CONSTRAINT SYS_C008045 PRIMARY KEY (GENEPROPERTYID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEGPV " +
                        "(GENEGPVID NUMERIC NOT NULL," +
                        "GENEID NUMERIC, " +
                        "GENEPROPERTYVALUEID NUMERIC, " +
                        "VALUE CHAR, " +
                        "CONSTRAINT SYS_C008049 PRIMARY KEY (GENEGPVID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEPROPERTYVALUE " +
                        "(GENEPROPERTYVALUEID NUMERIC, " +
                        "GENEPROPERTYID NUMERIC, " +
                        "VALUE CHAR," +
                        "CONSTRAINT PK_GENEPROPERTYVALUE PRIMARY KEY (GENEPROPERTYVALUEID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ORGANISM " +
                        "(ORGANISMID NUMERIC NOT NULL, " +
                        "NAME CHAR, " +
                        "CONSTRAINT SYS_C008043 PRIMARY KEY (ORGANISMID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_DESIGNELEMENT " +
                        "(DESIGNELEMENTID NUMERIC NOT NULL, " +
                        "ARRAYDESIGNID NUMERIC, " +
                        "GENEID NUMERIC NOT NULL, " +
                        "ACCESSION CHAR, " +
                        "NAME CHAR, " +
                        "TYPE CHAR, " +
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
                        "ACCESSION CHAR, " +
                        "PROPERTY CHAR, " +
                        "PROPERTYVALUE CHAR, " +
                        "ONTOLOGYTERM CHAR, " +
                        "ONTOLOGYTERMNAME CHAR, " +
                        "ONTOLOGYTERMID NUMERIC, " +
                        "ONTOLOGYNAME CHAR, " +
                        "ISSAMPLEPROPERTY BOOLEAN, " +
                        "ISASSAYPROPERTY BOOLEAN, " +
                        "ISFACTORVALUE BOOLEAN)");

        runStatement(conn,
                "CREATE TABLE LOAD_MONITOR " +
                        "(ID NUMERIC NOT NULL, " +
                        "ACCESSION CHAR, " +
                        "STATUS CHAR, " +
                        "NETCDF CHAR, " +
                        "SIMILARITY CHAR, " +
                        "RANKING CHAR, " +
                        "SEARCHINDEX CHAR, " +
                        "LOAD_TYPE CHAR, " +
                        "CONSTRAINT TABLE1_PK PRIMARY KEY (ID))");

        runStatement(conn,
                "CREATE TABLE VWEXPRESSIONANALYTICSBYGENE " +
                        "(GENEID NUMERIC NOT NULL, " +
                        "EF CHAR NOT NULL, " +
                        "EFV CHAR NOT NULL, " +
                        "EXPERIMENTID NUMERIC NOT NULL, " +
                        "PVALADJ FLOAT NOT NULL, " +
                        "TSTAT FLOAT NOT NULL, " +
                        "EFID NUMERIC NOT NULL, " +
                        "EFVID NUMERIC NOT NULL, " +
                        "DESIGNELEMENTID NUMERIC NOT NULL) ");

        // testing adding stored procedures
        if (FALSE)
            sqrt(1);
        createProcedure(conn, "SQRT", "sqrt");

        if (FALSE)
            a2ExperimentSet(conn, "", "", "", "");
        createProcedure(conn, "A2_EXPERIMENTSET", "a2ExperimentSet");

        if (FALSE)
            try {
                a2AssaySet(conn, "", "", "", new Object(), new Object());
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        createProcedure(conn, "A2_ASSAYSET", "a2AssaySet");

        if (FALSE)
            a2SampleSet(conn, "", "", new Object(), new Object(), "");
        createProcedure(conn, "A2_SAMPLESET", "a2SampleSet");

        if (FALSE)
            try {
                loadProgress(conn, "", "", "", "");
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        createProcedure(conn, "load_progress", "loadProgress");

        if (FALSE)
            a2SampleOrganism(1);
        createProcedure(conn, "A2_SampleOrganism", "a2SampleOrganism");


        System.out.println("...done!");
        conn.close();
    }

    private void createProcedure(Connection conn, String procedure, String method) throws SQLException {
        runStatement(conn, String.format("CREATE ALIAS %s FOR \"uk.ac.ebi.gxa.dao.AtlasDAOTestCase.%s\"",
                procedure, method));
    }

    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    public static String a2SampleOrganism(int id) {
        return "Sample Organism Placeholder: " + id;
    }

    public static void a2ExperimentSet(Connection conn,
                                       String accession, String description,
                                       String performer, String lab) throws SQLException {
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
    public static void a2AssaySet(Connection conn,
                                  String accession, String experimentAccession,
                                  String arrayDesignAccession,
                                  Object properties, Object expressionValues)
            throws Exception {
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
        long assayid = (long) System.currentTimeMillis();

        stmt.executeQuery(
                "INSERT INTO A2_ASSAY(assayid, accession, experimentid, arraydesignid) " +
                        "values (" + assayid + ", '" + accession + "', '" +
                        experimentID + "', '" + arrayDesignID + "');");

        stmt.close();
    }

    public static void a2SampleSet(Connection conn,
                                   String experimentAccession,
                                   String sampleAccession,
                                   Object assays, Object properties,
                                   String channel) throws SQLException {
        // this mimics the stored procedure A2_SAMPLESET in the actual DB
        Statement stmt = conn.createStatement();

        // create an sampleid - no oracle id generators here!
        long sampleid = (long) System.currentTimeMillis();

        stmt.executeQuery(
                "INSERT INTO A2_SAMPLE(sampleid, accession, channel) " +
                        "values (" + sampleid + ", '" + sampleAccession +
                        "', '" + channel + "');");
    }

    public static void loadProgress(Connection conn,
                                    String accession,
                                    String stage,
                                    String status,
                                    String load_type)
            throws Exception {
        // this mimics the stored procedure load_progress in the actual DB

        // todo
//        // lookup ids from accession first
//        Statement stmt = conn.createStatement();
//
//        stmt.executeUpdate(
//                "INSERT INTO LOAD_MONITOR(accession, stage, status);");
//
//        stmt.close();
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
        st.executeUpdate(sql);
        st.close();
    }
}