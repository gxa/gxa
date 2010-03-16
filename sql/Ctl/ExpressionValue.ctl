OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Expression/ExpressionValue.dat'
APPEND into table a2_ExpressionValue
fields terminated by "\t" 
TRAILING NULLCOLS
(EXPRESSIONVALUEID,VALUE,DESIGNELEMENTID,ASSAYID,EXPERIMENTID FILLER)

