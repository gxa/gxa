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

select distinct e.accession || chr(9) || e.experimentId || '_' || a.arraydesignid || '.nc' from a2_experiment e
  join a2_assay a on a.experimentid = e.experimentid
  where e.private = 0;
/
exit;
