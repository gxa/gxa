SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab on
set timeout on
set line 500
set wrap off

Select GENEID || chr(9) || R || chr(9) || IDENTIFIER || chr(9) || EXPERIMENT || chr(9) || EF || chr(9) || EFV || 
chr(9) || EXPRESSION
from vwPivotGeneEf 

/
quit;
