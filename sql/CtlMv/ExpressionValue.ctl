OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'DataMv/ExpressionValue.dat'
truncate into table a2_ExpressionValue
fields terminated by ";" optionally enclosed by '"'
TRAILING NULLCOLS
(EXPRESSIONVALUEID,VALUE,DESIGNELEMENTID,ASSAYID,EXPERIMENTID)

