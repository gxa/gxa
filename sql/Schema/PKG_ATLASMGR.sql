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
 * http://gxa.github.com/gxa/
 */

CREATE OR REPLACE PACKAGE ATLASMGR IS
  PROCEDURE DisableConstraints;
  PROCEDURE EnableConstraints;
  PROCEDURE DisableTriggers;
  PROCEDURE EnableTriggers;
  PROCEDURE RebuildSequence(seq_name varchar2);
  PROCEDURE RebuildSequences;
  PROCEDURE RebuildIndex;
  PROCEDURE fix_sequence(tbl VARCHAR2, field VARCHAR2, seq VARCHAR2);
END ATLASMGR;
/

/*******************************************************************************
/*******************************************************************************/
CREATE OR REPLACE PACKAGE BODY ATLASMGR AS

--------------------------------------------------------------------------------
PROCEDURE DisableConstraints
AS
 cursor c1 is select CONSTRAINT_NAME, TABLE_NAME from user_constraints where constraint_type = 'R';
 q varchar2(8000);
begin

for rec in c1
 loop
    q := 'ALTER TABLE ' || rec.TABLE_NAME  || ' DISABLE CONSTRAINT ' || rec.CONSTRAINT_NAME;
    
    dbms_output.put_line(q);
    
    EXECUTE IMMEDIATE q;
 end loop;
END;
--------------------------------------------------------------------------------
PROCEDURE EnableConstraints
AS
 cursor c1 is select CONSTRAINT_NAME, TABLE_NAME 
              from user_constraints 
              where constraint_type = 'R' 
              and CONSTRAINT_NAME <> 'FK_EV_DESIGNELEMENT'; --orphane ev in release 10.3
 q varchar2(8000);
begin 
 delete from A2_ASSAYPVONTOLOGY where ASSAYPVID = 0;
 delete from A2_SAMPLEPVONTOLOGY where SAMPLEPVID  = 0;

for rec in c1
 loop
    q := 'ALTER TABLE ' || rec.TABLE_NAME  || ' ENABLE CONSTRAINT ' || rec.CONSTRAINT_NAME;
    
    dbms_output.put_line(q);
    
    EXECUTE IMMEDIATE q;
 end loop;
END;
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
PROCEDURE DisableTriggers
AS
 cursor c1 is select TRIGGER_NAME from user_triggers; 
 q varchar2(8000);
begin
for rec in c1
 loop
    q := 'ALTER TRIGGER ' || rec.TRIGGER_NAME  || ' DISABLE';
    
    dbms_output.put_line(q);
    
    EXECUTE IMMEDIATE q;
 end loop;
END;
--------------------------------------------------------------------------------
PROCEDURE EnableTriggers
AS
 cursor c1 is select TRIGGER_NAME from user_triggers; 
 q varchar2(8000);
begin
for rec in c1
 loop
    q := 'ALTER TRIGGER ' || rec.TRIGGER_NAME  || ' ENABLE';

    dbms_output.put_line(q);

    EXECUTE IMMEDIATE q;
 end loop;
END;
--------------------------------------------------------------------------------

PROCEDURE fix_sequence(tbl VARCHAR2, field VARCHAR2, seq VARCHAR2)
AS
  current_id  NUMBER;
  current_seq NUMBER;
BEGIN
  EXECUTE immediate 'select max(' || field || ') from ' || tbl into current_id;
  EXECUTE immediate 'select ' || seq || '.nextval from dual' into current_seq;
  IF (current_id > current_seq) THEN
    EXECUTE immediate 'alter sequence ' || seq || ' increment by ' || to_char(current_id - current_seq);
    EXECUTE immediate 'select ' || seq || '.nextval from dual' into current_seq;
    EXECUTE immediate 'alter sequence ' || seq || ' increment by 1';
  END IF;
END;

procedure RebuildSequence(seq_name varchar2)
AS
 TABLE_NAME varchar2(255);
 PK_NAME varchar2(255);
begin
    TABLE_NAME := REPLACE(seq_name, '_SEQ', '');

    select c.COLUMN_NAME into PK_NAME
    from user_tab_columns c
    join user_constraints k on k.table_name = c.table_name
    join user_indexes i on i.INDEX_NAME = k.INDEX_NAME and i.TABLE_NAME = k.TABLE_NAME
    join user_ind_columns ic on ic.INDEX_NAME = i.INDEX_NAME and ic.TABLE_NAME = i.TABLE_NAME
    where ic.COLUMN_NAME = c.COLUMN_NAME
    and k.constraint_type ='P'
    and k.TABLE_NAME = REBUILDSEQUENCE.TABLE_NAME;

    fix_sequence(TABLE_NAME, PK_NAME, seq_name);
exception
  when NO_DATA_FOUND then
    dbms_output.put_line('No data for ' ||  seq_name || ' - please report');
end;


/*******************************************************************************
rebuilding sequences for all tables in schema

naming conventions and DB structure assumptions:
Each table has one autoincrement PK, updated in trigger from corresponding
sequence. SEQUENCE_NAME = TABLE_NAME || "_SEQ"

call RebuildSequence();
********************************************************************************/
procedure RebuildSequences
AS
 cursor c1 is select SEQUENCE_NAME, LAST_NUMBER from user_sequences;
begin
 for rec in c1
 loop
    RebuildSequence(rec.SEQUENCE_NAME);
 END LOOP;
END;

--call ATLASMGR.RebuildIndex()
procedure RebuildIndex
AS
 cursor c1 is select INDEX_NAME from user_indexes;
 q varchar2(8000);
BEGIN
 for rec in c1
 loop
  q := 'ALTER INDEX $INDEX_NAME REBUILD';
  q := REPLACE(q,'$INDEX_NAME',rec.INDEX_NAME); 
  dbms_output.put_line(q);
  Execute immediate q;
  
 end loop;
END;

END;
/
exit;
