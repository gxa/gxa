CREATE OR REPLACE PACKAGE ATLASMGR IS
  PROCEDURE DisableConstraints;
  PROCEDURE EnableConstraints;
  PROCEDURE DisableTriggers;
  PROCEDURE EnableTriggers;
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
 cursor c1 is select CONSTRAINT_NAME, TABLE_NAME from user_constraints where constraint_type = 'R';
 q varchar2(8000);
begin 
for rec in c1
 loop
    q := 'ALTER TABLE ' || rec.TABLE_NAME  || ' ENABLE CONSTRAINT ' || rec.CONSTRAINT_NAME;
    
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

END;
/
exit;
/