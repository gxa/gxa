OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/ExpressionAnalytics.dat'
truncate into table a2_ExpressionAnalytics
fields terminated by "\t" 
TRAILING NULLCOLS
(EXPRESSIONID,EXPERIMENTID,PROPERTYVALUEID,TSTAT,PVALADJ,FPVAL,FPVALADJ,DESIGNELEMENTID)

