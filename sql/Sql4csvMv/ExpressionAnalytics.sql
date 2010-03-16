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

Select 
rownum || chr(9) ||
Experiment_ID_Key || chr(9) ||
DESIGNELEMENT_ID_KEY || chr(9) ||
NULL || chr(9) ||
EF || chr(9) ||
EFV || chr(9) ||
UPDN_TSTAT || chr(9) ||
FPVAL || chr(9) ||
FPVALADJ || chr(9) ||
UPDN_pvaladj 
from Atlas

/
quit;
