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

select designelement_id_key     || chr(9) ||
       arraydesign_id           || chr(9) ||
       gene_id_key              || chr(9) ||
       designelement_identifier || chr(9) ||
       designelement_name       || chr(9) ||
       designelement_type       || chr(9) ||
       designelement_iscontrol
from ae2__designelement__main
where gene_id_key is not null;

quit;
