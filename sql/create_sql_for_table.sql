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

SELECT 'SELECT ' || WM_CONCAT(DECODE(Data_TYPE, 'DATE', 'TO_CHAR(' || column_name || ',''YYYY-MM-DD HH24:MI:SS'')', column_name)) 
|| ' FROM ' || UPPER('A2_&1')
FROM user_tab_columns WHERE table_name=UPPER('A2_&1');
/
exit;
/