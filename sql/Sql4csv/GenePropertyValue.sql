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
GENEPROPERTYVALUEID || chr(9) ||            
GENEPROPERTYID || chr(9) ||                 
VALUE
from A2_GENEPROPERTYVALUE

/
quit;
