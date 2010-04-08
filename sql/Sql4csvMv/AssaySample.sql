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

select rownum            || chr(9) ||
       assay_id_key      || chr(9) ||
       sample_id_key
from ae1__sample__main
where assay_id_key is not null
AND EXISTS
  (select 1 from AE1__ASSAY__MAIN
    where AE1__ASSAY__MAIN.assay_id_key = ae1__sample__main.assay_id_key
      AND AE1__ASSAY__MAIN.ARRAYDESIGN_ID is not null);


quit;
