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

import org.dbunit.DataSourceBasedDBTestCase;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abstract TestCase useful for setting up an in memory (hypersonic) database, and creating a DBUnit environment for
 * testing DAO methods and other stuff.
 *
 * @author Tony Burdett
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(transactionManager = "atlasTxManager", defaultRollback = false)
@Transactional
public abstract class AtlasDAOTestCase extends DataSourceBasedDBTestCase {
    private static final String ATLAS_DATA_RESOURCE = "atlas-be-db.xml";

    @Autowired(required = true)
    @Qualifier("atlasDataSource")
    protected DataSource atlasDataSource;
    @Autowired
    protected AtlasDAO atlasDAO;
    @Autowired
    protected ArrayDesignDAO arrayDesignDAO;
    @Autowired
    protected BioEntityDAO bioEntityDAO;
    @Autowired
    protected ExperimentDAO experimentDAO;
    @Autowired
    protected SessionFactory sessionFactory;

    protected IDataSet getDataSet() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_DATA_RESOURCE);

        return new FlatXmlDataSet(in);
    }

    @Override
    protected DataSource getDataSource() {
        return atlasDataSource;
    }

    /**
     * This sets up an in-memory database using Hypersonic, and uses DBUnit to dump sample data form atlas-be-db.xml into
     * this in-memory database.  It then configures a SingleConnectionDataSource from spring to provide access to the
     * underlying DB connection.  Finally, it initialises a JdbcTemplate using this datasource, and an AtlasDAO using
     * this template.  After setup, you should be able to use atlasDAO to test method calls against the data configured
     * in the sample dataset, or add to it and check the resulting data.
     */
    @Before
    public void setUp() throws Exception {
        createDatabase();
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        destroyDatabase();
    }

    public void createDatabase() throws SQLException, ClassNotFoundException {
        // get a database connection, that will create the DB if it doesn't exist yet
        Connection conn = atlasDataSource.getConnection("sa", "");
        System.out.print("Creating test database tables...");

        runStatement(conn,
                "CREATE TABLE DUAL " +
                        "(DUMMY VARCHAR(1) );");

        runStatement(conn,
                "CREATE TABLE A2_ORGANISM " +
                        "(ORGANISMID bigint not null, " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008043 PRIMARY KEY (ORGANISMID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_EXPERIMENT " +
                        "(EXPERIMENTID bigint NOT NULL, " +
                        "ABSTRACT VARCHAR(2000), " +
                        "ACCESSION VARCHAR(255), " +
                        "DESCRIPTION VARCHAR(2000), " +
                        "PERFORMER VARCHAR(2000), " +
                        "LAB VARCHAR(2000), " +
                        "LOADDATE timestamp, " +
                        "PMID VARCHAR(255)," +
                        "PRIVATE bit," +
                        "CURATED bit, " +
                        "CONSTRAINT SYS_C008053 PRIMARY KEY (EXPERIMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_EXPERIMENTASSET " +
                        "(EXPERIMENTASSETID bigint not null, " +
                        "EXPERIMENTID bigint not null, " +
                        "DESCRIPTION VARCHAR(2000), " +
                        "FILENAME VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C009999 PRIMARY KEY (EXPERIMENTASSETID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ARRAYDESIGN " +
                        "(ARRAYDESIGNID bigint not null, " +
                        "ACCESSION VARCHAR(255), " +
                        "TYPE VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "PROVIDER VARCHAR(255), " +
                        "MAPPINGSWID bigint, " +
                        "CONSTRAINT SYS_C008062 PRIMARY KEY (ARRAYDESIGNID))");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTY " +
                        "(PROPERTYID bigint not null, " +
                        "NAME VARCHAR(255), " +
                        "ACCESSION VARCHAR(255), " +
                        "CONSTRAINT SYS_C008064 PRIMARY KEY (PROPERTYID));");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTYVALUE " +
                        "(PROPERTYVALUEID bigint not null, " +
                        "PROPERTYID bigint, " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008066 PRIMARY KEY (PROPERTYVALUEID));");

        runStatement(conn,
                "CREATE TABLE A2_ASSAY " +
                        "(ASSAYID bigint not null, " +
                        "ACCESSION VARCHAR(255), " +
                        "EXPERIMENTID bigint not null, " +
                        "ARRAYDESIGNID bigint not null, " +
                        "CONSTRAINT SYS_C008055 PRIMARY KEY (ASSAYID), " +
                        "CONSTRAINT FKA2_ASSAY856724 FOREIGN KEY (ARRAYDESIGNID) " +
                        "REFERENCES A2_ARRAYDESIGN (ARRAYDESIGNID), " +
                        "CONSTRAINT FKA2_ASSAY169476 FOREIGN KEY (EXPERIMENTID) " +
                        "REFERENCES A2_EXPERIMENT (EXPERIMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ASSAYPV " +
                        "(ASSAYPVID bigint not null, " +
                        "ASSAYID bigint, " +
                        "PROPERTYVALUEID bigint, " +
                        "CONSTRAINT SYS_C008058 PRIMARY KEY (ASSAYPVID));");

        runStatement(conn,
                "CREATE TABLE A2_SAMPLE " +
                        "(SAMPLEID bigint not null, " +
                        "EXPERIMENTID bigint not null, " +
                        "ACCESSION VARCHAR(255), " +
                        "ORGANISMID bigint, " +
                        "CHANNEL VARCHAR(255), " +
                        "CONSTRAINT SYS_C008059 PRIMARY KEY (SAMPLEID)," +
                        "CONSTRAINT FKA2_SAMPLE12345 FOREIGN KEY (ORGANISMID) " +
                        "REFERENCES A2_ORGANISM (ORGANISMID));");

        runStatement(conn,
                "  CREATE TABLE A2_SAMPLEPV " +
                        "(SAMPLEPVID bigint not null, " +
                        "SAMPLEID bigint not null, " +
                        "PROPERTYVALUEID bigint, " +
                        "CONSTRAINT SYS_C008061 PRIMARY KEY (SAMPLEPVID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ASSAYSAMPLE " +
                        "(ASSAYSAMPLEID bigint not null, " +
                        "ASSAYID bigint, " +
                        "SAMPLEID bigint, " +
                        "CONSTRAINT SYS_C008067 PRIMARY KEY (ASSAYSAMPLEID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_GENE " +
                        "(GENEID bigint, " +
                        "ORGANISMID bigint not null, " +
                        "IDENTIFIER VARCHAR(255), " +
                        "NAME VARCHAR(255)) ;");

        runStatement(conn,
                "CREATE TABLE A2_GENEPROPERTY " +
                        "(GENEPROPERTYID bigint not null, " +
                        "NAME VARCHAR(255), " +
                        "AE2TABLENAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008045 PRIMARY KEY (GENEPROPERTYID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEGPV " +
                        "(GENEGPVID bigint not null," +
                        "GENEID bigint, " +
                        "GENEPROPERTYVALUEID bigint, " +
                        "VALUE VARCHAR(255), " +
                        "CONSTRAINT SYS_C008049 PRIMARY KEY (GENEGPVID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_GENEPROPERTYVALUE " +
                        "(GENEPROPERTYVALUEID bigint, " +
                        "GENEPROPERTYID bigint, " +
                        "VALUE VARCHAR(255)," +
                        "CONSTRAINT PK_GENEPROPERTYVALUE PRIMARY KEY (GENEPROPERTYVALUEID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_SOFTWARE " +
                        "(SOFTWAREID bigint, " +
                        "NAME VARCHAR(255) NOT NULL, " +
                        "ISACTIVE bit NOT NULL, " +
                        "VERSION VARCHAR(255) NOT NULL) ;");

        runStatement(conn,
                "CREATE TABLE A2_BIOENTITY " +
                        "(BIOENTITYID bigint, " +
                        "ORGANISMID bigint not null, " +
                        "NAME VARCHAR(255), " +
                        "BIOENTITYTYPEID bigint not null, " +
                        "IDENTIFIER VARCHAR(255)) ;");

        runStatement(conn,
                "CREATE TABLE A2_BIOENTITYPROPERTY " +
                        "(BIOENTITYPROPERTYID bigint not null, " +
                        "NAME VARCHAR(255), " +
                        "CONSTRAINT SYS_C008070 PRIMARY KEY (BIOENTITYPROPERTYID)) ;");

        runStatement(conn,
                "  CREATE TABLE A2_BIOENTITYBEPV " +
                        "(BIOENTITYBEPVID bigint not null," +
                        "BIOENTITYID bigint, " +
                        "BEPROPERTYVALUEID bigint, " +
                        "SOFTWAREID bigint, " +
                        "CONSTRAINT SYS_C008071 PRIMARY KEY (BIOENTITYBEPVID )) ;");

        runStatement(conn,
                "  CREATE TABLE A2_BIOENTITYPROPERTYVALUE " +
                        "(BEPROPERTYVALUEID bigint, " +
                        "BIOENTITYPROPERTYID bigint, " +
                        "VALUE VARCHAR(255) );");

        runStatement(conn,
                "  CREATE TABLE A2_BIOENTITYTYPE " +
                        "(BIOENTITYTYPEID bigint, " +
                        "NAME VARCHAR(255), " +
                        "ID_FOR_INDEX int, " +
                        "ID_FOR_ANALYTICS int, " +
                        "identifierPropertyID bigint, " +
                        "namePropertyID bigint, " +
                        "PROP_FOR_INDEX int );");

        runStatement(conn,
                "  CREATE TABLE A2_BERELATIONTYPE " +
                        "(BERELATIONTYPEID bigint, " +
                        "NAME VARCHAR(255));");

        runStatement(conn,
                "CREATE TABLE A2_BIOENTITY2BIOENTITY " +
                        "(BE2BEID bigint, " +
                        "BIOENTITYIDFROM bigint not null, " +
                        "BIOENTITYIDTO bigint not null, " +
                        "SOFTWAREID bigint not null, " +
                        "BERELATIONTYPEID bigint not null);");

        runStatement(conn,
                "CREATE TABLE A2_DESIGNELTBIOENTITY " +
                        "(DEBEID bigint, " +
                        "DESIGNELEMENTID bigint not null, " +
                        "SOFTWAREID bigint not null, " +
                        "BIOENTITYID bigint not null);");

        runStatement(conn,
                "CREATE TABLE VWDESIGNELEMENTGENELINKED " +
                        "(designelementid bigint not null, " +
                        "accession VARCHAR(255) NOT NULL, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "arraydesignid bigint not null, " +
                        "bioentityid bigint not null, " +
                        "identifier VARCHAR(255) NOT NULL, " +
                        "organismid bigint not null, " +
                        "mappingswid bigint not null, " +
                        "annotationswid bigint not null) ");

        runStatement(conn,
                "CREATE TABLE VWDESIGNELEMENTGENEDIRECT " +
                        "(designelementid bigint not null, " +
                        "accession VARCHAR(255) NOT NULL, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "arraydesignid bigint not null, " +
                        "bioentityid bigint not null, " +
                        "identifier VARCHAR(255) NOT NULL, " +
                        "organismid bigint not null) ");

        runStatement(conn,
                "CREATE TABLE A2_ANNOTATIONSRC(\n" +
                        "  annotationsrcid bigint NOT NULL\n" +
                        "  , SOFTWAREID bigint NOT NULL\n" +
                        "  , ORGANISMID bigint NOT NULL\n" +
                        "  , url VARCHAR(512)\n" +
                        "  , biomartorganismname VARCHAR(255)\n" +
                        "  , databaseName VARCHAR(255)\n" +
                        "  , annsrctype VARCHAR(255) NOT NULL\n" +
                        "  , LOADDATE DATE\n" +
                        ");");

        runStatement(conn,
                "CREATE TABLE A2_ANNSRC_BIOENTITYTYPE(\n" +
                        "  annotationsrcid bigint NOT NULL\n" +
                        "  , BIOENTITYTYPEID bigint NOT NULL\n" +
                        "  );");

        runStatement(conn,
                "CREATE TABLE A2_BIOMARTPROPERTY (\n" +
                        "  BIOMARTPROPERTYID bigint NOT NULL\n" +
                        ", annotationsrcid bigint NOT NULL\n" +
                        ", BIOENTITYPROPERTYID bigint NOT NULL\n" +
                        ", NAME VARCHAR(255) NOT NULL\n" +
                        ");");

        runStatement(conn,
                "CREATE TABLE A2_BIOMARTARRAYDESIGN (\n" +
                        "  BIOMARTARRAYDESIGNID bigint NOT NULL\n" +
                        ", annotationsrcid bigint NOT NULL\n" +
                        ", ARRAYDESIGNID bigint NOT NULL\n" +
                        ", NAME VARCHAR(255) NOT NULL\n" +
                        ");");


        runStatement(conn,
                "CREATE TABLE A2_DESIGNELEMENT " +
                        "(DESIGNELEMENTID bigint not null, " +
                        "ARRAYDESIGNID bigint, " +
                        "GENEID bigint not null, " +
                        "ACCESSION VARCHAR(255), " +
                        "NAME VARCHAR(255), " +
                        "TYPE VARCHAR(255), " +
                        "ISCONTROL INTEGER, " +
                        "CONSTRAINT SYS_C008063 PRIMARY KEY (DESIGNELEMENTID)) ;");

        runStatement(conn,
                "CREATE TABLE A2_ONTOLOGYMAPPING " +
                        "(EXPERIMENTID bigint not null, " +
                        "ACCESSION VARCHAR(255), " +
                        "PROPERTY VARCHAR(255), " +
                        "PROPERTYVALUE VARCHAR(255), " +
                        "ONTOLOGYTERM VARCHAR(255), " +
                        "ONTOLOGYTERMNAME VARCHAR(255), " +
                        "ONTOLOGYTERMID bigint, " +
                        "ONTOLOGYNAME VARCHAR(255), " +
                        "ISSAMPLEPROPERTY BOOLEAN, " +
                        "ISASSAYPROPERTY BOOLEAN);");

        runStatement(conn,
                "CREATE TABLE A2_ONTOLOGYTERM (\n" +
                        "    ONTOLOGYTERMID bigint not null\n" +
                        "  , ONTOLOGYID bigint not null\n" +
                        "  , TERM VARCHAR(4000)\n" +
                        "  , ACCESSION VARCHAR(255) NOT NULL\n" +
                        "  , DESCRIPTION VARCHAR(4000))");

        runStatement(conn,
                "CREATE TABLE A2_ONTOLOGY (\n" +
                        "    ONTOLOGYID bigint not null\n" +
                        "  , name VARCHAR(255) NOT NULL\n" +
                        "  , SOURCE_URI VARCHAR(255) NOT NULL\n" +
                        "  , version VARCHAR(255) NOT NULL\n" +
                        "  , DESCRIPTION VARCHAR(4000))");

        runStatement(conn,
                "  CREATE TABLE A2_ASSAYPVONTOLOGY (\n" +
                        "    ASSAYPVONTOLOGYID bigint not null\n" +
                        "  , ONTOLOGYTERMID bigint not null\n" +
                        "  , ASSAYPVID bigint not null)");

        runStatement(conn,
                "  CREATE TABLE A2_SAMPLEPVONTOLOGY (\n" +
                        "    SAMPLEPVONTOLOGYID bigint not null\n" +
                        "  , ONTOLOGYTERMID bigint not null\n" +
                        "  , SAMPLEPVID bigint not null)");

        runStatement(conn, "CREATE SCHEMA ATLASLDR AUTHORIZATION sa");

        runStatement(conn, "CREATE PROCEDURE A2_EXPERIMENTSET(" +
                "    IN Accession VARCHAR(255), IN Description VARCHAR(255)," +
                "    IN Performer VARCHAR(255), IN Lab VARCHAR(255)," +
                "    IN PMID VARCHAR(255), IN Abstract VARCHAR(255))\n" +
                "   MODIFIES SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.a2ExperimentSet'");

        runStatement(conn, "CREATE PROCEDURE A2_ASSAYSET(\n" +
                "   IN Accession VARCHAR(255), IN ExperimentAccession  VARCHAR(255),\n" +
                "   IN ArrayDesignAccession VARCHAR(255),\n" +
                "   IN Properties CHAR ARRAY)\n" +
                "   MODIFIES SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.assaySet'");

        runStatement(conn, "CREATE PROCEDURE ATLASLDR.A2_SAMPLESET(\n" +
                "    IN ExperimentAccession VARCHAR(255), IN SampleAccession VARCHAR(255), " +
                "    IN Assays INT ARRAY, IN Properties INT ARRAY, IN Channel VARCHAR(255))\n" +
                "   MODIFIES SQL DATA\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.a2SampleSet'");

        runStatement(conn, "CREATE PROCEDURE ATLASLDR.LOAD_PROGRESS(\n" +
                " IN experiment_accession VARCHAR(255), IN stage VARCHAR(255), " +
                " IN status VARCHAR(255), IN load_type VARCHAR(255))\n" +
                "  NO SQL\n" +
                "  LANGUAGE JAVA\n" +
                "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.loadProgress'");

        runStatement(conn,
                "CREATE AGGREGATE FUNCTION wm_concat(" +
                        "    IN val VARCHAR(255), IN flag BOOLEAN, " +
                        "    INOUT register VARCHAR(255), INOUT counter INT)\n" +
                        "  RETURNS VARCHAR(512)\n" +
                        "  NO SQL\n" +
                        "  LANGUAGE JAVA\n" +
                        "  EXTERNAL NAME 'CLASSPATH:uk.ac.ebi.gxa.dao.AtlasDAOTestCase.wmConcat'");

        runStatement(conn, "CREATE SEQUENCE A2_ARRAYDESIGN_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ASSAY_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ASSAYPV_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ASSET_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ANNOTATIONSRC_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_BIOMARTPROPERTY_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_EXPERIMENT_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ONTOLOGY_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ONTOLOGYTERM_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_ORGANISM_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_PROPERTY_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_PROPERTYVALUE_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_SAMPLE_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_SAMPLEPV_SEQ");
        runStatement(conn, "CREATE SEQUENCE A2_SOFTWARE_SEQ");

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

        stmt.executeUpdate(
                "INSERT INTO A2_EXPERIMENT(experimentid, accession, description, performer, lab) " +
                        "values (" + experimentid + ", '" + accession + "', '" +
                        description + "', '" + performer + "', '" + lab + "');");
    }

    @SuppressWarnings("unused")
    public static void assaySet(String accession, String experimentAccession,
                                String arrayDesignAccession,
                                Array properties)
            throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:default:connection");
        // this mimics the stored procedure A2_ASSAYSET in the actual DB

        // lookup ids from accession first
        Statement stmt = con.createStatement();

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
    public static void a2SampleSet(String experimentAccession,
                                   String sampleAccession,
                                   Array assays, Array properties,
                                   String channel) throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:default:connection");
        // this mimics the stored procedure A2_SAMPLESET in the actual DB
        Statement stmt = con.createStatement();

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
    public static void loadProgress(String accession,
                                    String stage,
                                    String status,
                                    String load_type)
            throws Exception {
    }


    public void destroyDatabase() throws SQLException, ClassNotFoundException {
        Connection conn = atlasDataSource.getConnection();
        runStatement(conn, "SHUTDOWN");
        conn.close();
    }

    private static void runStatement(Connection conn, String sql) throws SQLException {
        // just using raw sql here, prior to any dao/jdbctemplate setup
        Statement st = conn.createStatement();
        st.execute(sql);
        st.close();
    }
}
