SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab on
set timeout on
set line 500
set wrap off

--select Experiment_id_key, Experiment_Accession
--from ae1__Experiment__Main

Select EXPRESSIONVALUE_ID_KEY || ';'
|| ABSOLUTE || ';'
|| DESIGNELEMENT_ID_KEY || ';'
|| ASSAY_ID || ';'
|| EXPERIMENT_ID || ';'
from AE2__EXPRESSIONVALUE__MAIN

/
quit;
