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

select experiment_id_key      || chr(9) ||
       experiment_accession   || chr(9) ||
       experiment_description || chr(9) ||
       experiment_performer   || chr(9) ||
       experiment_lab
from ae1__experiment__main;

quit;
