package uk.ac.ebi.gxa.db;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;

import static org.easymock.EasyMock.*;

/**
 * @author alf
 */
public class TestOracleScriptSplitter {
    @Test
    public void testPackage() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-package_with_body.sql";
        String[] expectedStatements = {
                "CREATE OR REPLACE PACKAGE ATLASMGR IS " +
                        "PROCEDURE DisableConstraints; " +
                        "PROCEDURE EnableConstraints; " +
                        "PROCEDURE DisableTriggers; " +
                        "PROCEDURE EnableTriggers; " +
                        "PROCEDURE RebuildSequence(seq_name varchar2); " +
                        "PROCEDURE RebuildSequences; " +
                        "PROCEDURE RebuildIndex; " +
                        "PROCEDURE fix_sequence(tbl VARCHAR2, field VARCHAR2, seq VARCHAR2); " +
                        "END ATLASMGR; ",
                "CREATE OR REPLACE PACKAGE BODY ATLASMGR AS " +
                        "PROCEDURE DisableConstraints " +
                        "AS " +
                        "cursor c1 is select CONSTRAINT_NAME, TABLE_NAME from user_constraints where constraint_type = 'R'; " +
                        "q varchar2(8000); " +
                        "begin " +
                        "for rec in c1 " +
                        "loop " +
                        "q := 'ALTER TABLE ' || rec.TABLE_NAME  || ' DISABLE CONSTRAINT ' || rec.CONSTRAINT_NAME; " +
                        "dbms_output.put_line(q); " +
                        "EXECUTE IMMEDIATE q; " +
                        "end loop; " +
                        "END; " +
                        "END; "};
        checkParser(sourceScript, expectedStatements);
    }

    @Test
    public void testSimpleStatements() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-simple_statements.sql";
        String[] expectedStatements = {
                "select * from dual",
                "select spv.sampleid, o.name, o.organismid from a2_samplepv spv " +
                        "join a2_propertyvalue pv on pv.propertyvalueid = spv.propertyvalueid " +
                        "join a2_property p on p.propertyid = pv.propertyid " +
                        "join a2_organism o on lower(o.name) = lower(pv.name) " +
                        "where p.name = 'organism'",
                "update ( select s1.organismid, s2.organismid as orgid " +
                        "from A2_SAMPLE s1 " +
                        "inner join A2_SAMPLE_ORGANISM_TMP s2 ON s1.sampleid = s2.sampleid " +
                        ") o set o.organismid = o.orgid"};
        checkParser(sourceScript, expectedStatements);
    }

    @Test
    public void testTrigger() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-trigger.sql";
        String[] expectedStatements = {
                "CREATE SEQUENCE  \"A2_ONTOLOGY_SEQ\"",
                "CREATE OR REPLACE TRIGGER A2_ONTOLOGY_INSERT " +
                        "before insert on A2_Ontology " +
                        "for each row " +
                        "begin " +
                        "if( :new.OntologyID is null) then " +
                        "select A2_Ontology_seq.nextval into :new.OntologyID from dual; " +
                        "end if; " +
                        "end; ",
                "ALTER TRIGGER A2_ONTOLOGY_INSERT ENABLE",
                "call atlasmgr.RebuildSequences()"};
        checkParser(sourceScript, expectedStatements);
    }

    @Test
    public void testSampleUpdateFrom209() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-sample.sql";
        String[] expectedStatements = {
                "DROP TRIGGER \"A2_ASSAYSAMPLE_INSERT\"",
                "DROP SEQUENCE \"A2_ASSAYSAMPLE_SEQ\"",
                "ALTER TABLE \"A2_ASSAYSAMPLE\" DROP CONSTRAINT PK_ASSAYSAMPLE",
                "ALTER TABLE \"A2_ASSAYSAMPLE\" DROP CONSTRAINT UQ_ASSAYSAMPLE",
                "ALTER TABLE \"A2_ASSAYSAMPLE\" DROP COLUMN ASSAYSAMPLEID",
                "ALTER TABLE \"A2_ASSAYSAMPLE\" " +
                        "ADD CONSTRAINT \"PK_ASSAYSAMPLE\" " +
                        "PRIMARY KEY (ASSAYID,SAMPLEID) " +
                        "/*PK_TABLESPACE*/ " +
                        "ENABLE"};
        checkParser(sourceScript, expectedStatements);
    }


    private void checkParser(String sourceScript, String[] expectedStatements) throws SQLException, IOException {
        SqlStatementExecutor executor = createMock(SqlStatementExecutor.class);
        for (String statement : expectedStatements) {
            executor.executeStatement(statement);
            expectLastCall();
        }
        replay(executor);

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream("uk/ac/ebi/gxa/db/" + sourceScript));
        new OracleScriptSplitter().parse(reader, executor);
        reader.close();
    }
}
