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

select assay_id_key      || chr(9) ||
       assay_identifier  || chr(9) ||
       experiment_id_key || chr(9) ||
       arraydesign_id
from ae1__assay__main
where arraydesign_id is not null;

quit;
