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
EXPRESSIONID || chr(9) ||                   
EXPERIMENTID || chr(9) ||                   
PROPERTYVALUEID || chr(9) ||                
TSTAT || chr(9) ||                          
PVALADJ || chr(9) ||                        
FPVAL || chr(9) ||                          
FPVALADJ || chr(9) ||                       
DESIGNELEMENTID      
from A2_EXPRESSIONANALYTICS

/
quit;
