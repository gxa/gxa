set head off
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 5000
set wrap off
set feedback off
set verify off

SELECT 'SELECT ' || WM_CONCAT(
  DECODE(data_type,
         'DATE',         'TO_CHAR(' || column_name || ',''YYYY-MM-DD HH24:MI:SS'')',
         'TIMESTAMP(6)', 'TO_CHAR(' || column_name || ',''YYYY-MM-DD HH24:MI:SS.FF'')',
         'VARCHAR2',     'DECODE(' || column_name || ', null, ' || column_name || ','
                                    || ' REPLACE(' || column_name ||', chr(10), chr(32)))',
         column_name))
 || ' FROM ' || UPPER('A2_&1')
FROM user_tab_columns WHERE table_name=UPPER('A2_&1');
exit;
