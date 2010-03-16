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

Select ASSAYPROPERTYVALUEID || chr(9) ||           
ASSAYID || chr(9) ||                        
PROPERTYVALUEID || chr(9) ||                
ISFACTORVALUE  
from A2_ASSAYPROPERTYVALUE

/
quit;
