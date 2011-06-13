cat experiment-assets-to-export.sql
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

select distinct e.accession || chr(9) || filename from a2_experiment e
  join a2_experimentasset a on a.experimentid = e.experimentid
  where e.private = 0;
exit;
