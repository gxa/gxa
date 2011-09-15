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
                "CREATE OR REPLACE PACKAGE ATLASMGR IS\n" +
                        "PROCEDURE DisableConstraints;\n" +
                        "PROCEDURE EnableConstraints;\n" +
                        "PROCEDURE DisableTriggers;\n" +
                        "PROCEDURE EnableTriggers;\n" +
                        "PROCEDURE RebuildSequence(seq_name varchar2);\n" +
                        "PROCEDURE RebuildSequences;\n" +
                        "PROCEDURE RebuildIndex;\n" +
                        "PROCEDURE fix_sequence(tbl VARCHAR2, field VARCHAR2, seq VARCHAR2);\n" +
                        "END ATLASMGR;\n",
                "CREATE OR REPLACE PACKAGE BODY ATLASMGR AS\n" +
                        "PROCEDURE DisableConstraints\n" +
                        "AS\n" +
                        "cursor c1 is select CONSTRAINT_NAME, TABLE_NAME from user_constraints where constraint_type = 'R';\n" +
                        "q varchar2(8000);\n" +
                        "begin\n" +
                        "for rec in c1\n" +
                        "loop\n" +
                        "q := 'ALTER TABLE ' || rec.TABLE_NAME  || ' DISABLE CONSTRAINT ' || rec.CONSTRAINT_NAME;\n" +
                        "dbms_output.put_line(q);\n" +
                        "EXECUTE IMMEDIATE q;\n" +
                        "end loop;\n" +
                        "END;\n" +
                        "END;\n"};
        checkParser(sourceScript, expectedStatements);
    }

    @Test
    public void testSimpleStatements() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-simple_statements.sql";
        String[] expectedStatements = {
                "select * from dual",
                "select spv.sampleid, o.name, o.organismid from a2_samplepv spv\n" +
                        "join a2_propertyvalue pv on pv.propertyvalueid = spv.propertyvalueid\n" +
                        "join a2_property p on p.propertyid = pv.propertyid\n" +
                        "join a2_organism o on lower(o.name) = lower(pv.name)\n" +
                        "where p.name = 'organism'",
                "update ( select s1.organismid, s2.organismid as orgid\n" +
                        "from A2_SAMPLE s1\n" +
                        "inner join A2_SAMPLE_ORGANISM_TMP s2 ON s1.sampleid = s2.sampleid\n" +
                        ") o set o.organismid = o.orgid"};
        checkParser(sourceScript, expectedStatements);
    }

    @Test
    public void testTrigger() throws IOException, SQLException {
        String sourceScript = "TestOracleScriptSplitter-trigger.sql";
        String[] expectedStatements = {
                "CREATE SEQUENCE  \"A2_ONTOLOGY_SEQ\"",
                "CREATE OR REPLACE TRIGGER A2_ONTOLOGY_INSERT\n" +
                        "before insert on A2_Ontology\n" +
                        "for each row\n" +
                        "begin\n" +
                        "if( :new.OntologyID is null) then\n" +
                        "select A2_Ontology_seq.nextval into :new.OntologyID from dual;\n" +
                        "end if;\n" +
                        "end;\n",
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
                "ALTER TABLE \"A2_ASSAYSAMPLE\"\n" +
                        "ADD CONSTRAINT \"PK_ASSAYSAMPLE\"\n" +
                        "PRIMARY KEY (ASSAYID,SAMPLEID)\n" +
                        "/*PK_TABLESPACE*/\n" +
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
