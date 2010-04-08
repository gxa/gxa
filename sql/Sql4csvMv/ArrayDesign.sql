SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 500
set wrap off
SET FEEDBACK OFF
SET VERIFY OFF


select arraydesign_id_key    || chr(9) ||
       arraydesign_accession || chr(9) ||
       arraydesign_type      || chr(9) ||
       arraydesign_name      || chr(9) ||
       arraydesign_provider
from ae2__arraydesign;

quit;
