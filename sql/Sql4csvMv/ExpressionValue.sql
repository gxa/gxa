SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab on
set line 500
set wrap off
SET FEEDBACK OFF
SET VERIFY OFF

--select Experiment_id_key, Experiment_Accession
--from ae1__Experiment__Main

Select EXPRESSIONVALUE_ID_KEY || chr(9)
|| ABSOLUTE || chr(9)
|| DESIGNELEMENT_ID_KEY || chr(9)
|| ASSAY_ID || chr(9)
|| EXPERIMENT_ID || chr(9)
from AE2__EXPRESSIONVALUE__MAIN

/
quit;

