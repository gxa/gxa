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
        ' TRAILING NULLCOLS (' || Listagg(Column_Name ||
          Decode(Data_Type, 'VARCHAR2', ' CHAR(' || data_length || ')' ), ',') WITHIN GROUP (ORDER BY column_id) || ')'
FROM user_tab_columns WHERE table_name=upper('A2_&1');

quit;
