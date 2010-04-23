set head off
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 500
set wrap off
set feedback off
set verify off

SELECT 'SELECT ' || LISTAGG(column_name, ',') WITHIN GROUP (ORDER BY column_id) || ' FROM ' || UPPER('A2_&1')
FROM user_tab_columns WHERE table_name=UPPER('A2_&1');

quit;
