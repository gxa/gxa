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

select distinct e.accession || chr(9) || e.accession || '_' || ad.accession || '.nc' from a2_experiment e
  join a2_assay a on a.experimentid = e.experimentid
  join a2_arraydesign ad on a.arraydesignid = ad.arraydesignid
  where e.private = 0;
exit;
