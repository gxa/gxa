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
GENEGPVID || chr(9) ||            
GENEID || chr(9) ||                         
GENEPROPERTYVALUEID                 
from A2_GENEGPV

/
quit;
