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

select distinct e.accession from a2_experiment e
  where e.private = 0;
exit;
