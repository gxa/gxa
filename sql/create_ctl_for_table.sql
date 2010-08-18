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

SELECT  'OPTIONS(DIRECT=TRUE,ROWS=1000000) LOAD DATA TRUNCATE INTO TABLE ' || upper('A2_&1') ||
        ' FIELDS TERMINATED BY ''\t''' ||
        ' TRAILING NULLCOLS (' || WM_CONCAT(Column_Name ||
          Decode(Data_Type, 'VARCHAR2', ' CHAR(' || data_length || ')', 'DATE', ' DATE "YYYY-MM-DD HH24:MI:SS"' )) || ')'
FROM user_tab_columns WHERE table_name=upper('A2_&1');

quit;
