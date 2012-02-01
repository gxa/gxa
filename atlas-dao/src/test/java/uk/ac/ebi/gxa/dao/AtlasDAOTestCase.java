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

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;

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
public abstract class AtlasDAOTestCase {
    private static final String ATLAS_DATA_RESOURCE = "atlas-be-db.xml";

    @Autowired(required = true)
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
    protected AssayDAO assayDAO;
    @Autowired
    protected SampleDAO sampleDAO;
    @Autowired
    protected OntologyDAO ontologyDAO;
    @Autowired
    protected OntologyTermDAO ontologyTermDAO;
    @Autowired
    protected PropertyValueDAO propertyValueDAO;
    @Autowired
    protected SessionFactory sessionFactory;

    protected IDataSet getDataSet() throws DataSetException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_DATA_RESOURCE);
        return new FlatXmlDataSetBuilder().build(in);
    }

    private void populateDatabase() throws SQLException, DatabaseUnitException {
        assertNotNull(atlasDataSource);
        IDatabaseConnection conn = getConnection(atlasDataSource);
        try {
            DatabaseOperation.CLEAN_INSERT.execute(conn, getDataSet());
        } finally {
            conn.close();
        }
    }

    private void cleanupDatabase() throws SQLException, DatabaseUnitException {
        assertNotNull(atlasDataSource);
        IDatabaseConnection conn = getConnection(atlasDataSource);
        try {
            DatabaseOperation.DELETE_ALL.execute(conn, getDataSet());
        } finally {
            conn.close();
        }
    }

    private IDatabaseConnection getConnection(DataSource dataSource) throws SQLException {
        IDatabaseConnection conn = new DatabaseDataSourceConnection(dataSource);
        conn.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                new HsqldbDataTypeFactory());
        return conn;
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
        populateDatabase();
    }

    @After
    public void tearDown() throws Exception {
        cleanupDatabase();
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
                        "DISPLAYNAME VARCHAR(512), " +
                        "ACCESSION VARCHAR(255), " +
                        "CONSTRAINT SYS_C008064 PRIMARY KEY (PROPERTYID));");

        runStatement(conn,
                "CREATE TABLE A2_PROPERTYVALUE " +
                        "(PROPERTYVALUEID bigint not null, " +
                        "PROPERTYID bigint, " +
                        "NAME VARCHAR(255), " +
                        "DISPLAYNAME VARCHAR(512), " +
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
                        "CONSTRAINT SYS_C008058 PRIMARY KEY (ASSAYPVID)," +
                        "CONSTRAINT FK_ASSAYPV_PROPERTY FOREIGN KEY (PROPERTYVALUEID) " +
                        "REFERENCES A2_PROPERTYVALUE (PROPERTYVALUEID) ON DELETE CASCADE) ;");

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
                        "CONSTRAINT SYS_C008061 PRIMARY KEY (SAMPLEPVID)," +
                        "CONSTRAINT FK_SAMPLEPV_PROPERTY FOREIGN KEY (PROPERTYVALUEID) " +
                        "REFERENCES A2_PROPERTYVALUE (PROPERTYVALUEID) ON DELETE CASCADE) ;");

        runStatement(conn,
                "CREATE TABLE A2_ASSAYSAMPLE " +
                        "(ASSAYID bigint, " +
                        "SAMPLEID bigint, " +
                        "CONSTRAINT SYS_C008067 PRIMARY KEY (ASSAYID, SAMPLEID)) ;");

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
                        "ISACTIVE VARCHAR(1) NOT NULL, " +
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
                "CREATE TABLE A2_DESIGNELTBIOENTITY " +
                        "(DEBEID bigint, " +
                        "DESIGNELEMENTID bigint not null, " +
                        "SOFTWAREID bigint not null, " +
                        "BIOENTITYID bigint not null);");

        runStatement(conn,
                "CREATE TABLE A2_ANNOTATIONSRC(\n" +
                        "  annotationsrcid bigint NOT NULL\n" +
                        "  , SOFTWAREID bigint NOT NULL\n" +
                        "  , ORGANISMID bigint NOT NULL\n" +
                        "  , url VARCHAR(512)\n" +
                        "  , biomartorganismname VARCHAR(255)\n" +
                        "  , databaseName VARCHAR(255)\n" +
                        "  , mySqlDbName VARCHAR(255)\n" +
                        "  , mySqlDbUrl VARCHAR(255)\n" +
                        "  , annsrctype VARCHAR(255) NOT NULL\n" +
                        "  , LOADDATE DATE\n" +
                        "  , isApplied VARCHAR(1) DEFAULT 'F'\n" +
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
                        "    ONTOLOGYTERMID bigint not null\n" +
                        "  , ASSAYPVID bigint not null, " +
                        "CONSTRAINT FK_ASSAYPV FOREIGN KEY (ASSAYPVID) " +
                        "REFERENCES A2_ASSAYPV (ASSAYPVID)  ON DELETE CASCADE) ;");


        runStatement(conn,
                "  CREATE TABLE A2_SAMPLEPVONTOLOGY (\n" +
                        "    ONTOLOGYTERMID bigint not null\n" +
                        "  , SAMPLEPVID bigint not null, " +
                        "CONSTRAINT FK_SAMPLEV FOREIGN KEY (SAMPLEPVID) " +
                        "REFERENCES A2_SAMPLEPV (SAMPLEPVID)  ON DELETE CASCADE) ;");

        runStatement(conn, "CREATE SEQUENCE A2_ARRAYDESIGN_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ASSAY_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ASSAYPV_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ASSET_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_EXPERIMENT_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ONTOLOGY_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ONTOLOGYTERM_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_ORGANISM_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_PROPERTY_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_PROPERTYVALUE_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_SAMPLE_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_SAMPLEPV_SEQ START WITH 10000000");

        runStatement(conn, "CREATE SEQUENCE A2_ANNOTATIONSRC_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_BIOMARTPROPERTY_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_BIOENTITYPROPERTY_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_SOFTWARE_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_BIOENTITYTYPE_SEQ START WITH 10000000");
        runStatement(conn, "CREATE SEQUENCE A2_BIOMARTARRAYDESIGN_SEQ START WITH 10000000");

        System.out.println("...done!");
        conn.close();
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
