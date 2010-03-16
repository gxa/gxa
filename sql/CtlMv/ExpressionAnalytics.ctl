OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/ExpressionAnalytics.dat'
truncate into table a2_ExpressionAnalytics
fields terminated by "\t" 
TRAILING NULLCOLS
(EXPRESSIONID
,EXPERIMENTID
,DESIGNELEMENTID
,PROPERTYVALUEID "get_propertyvalueid (:EF, :EFV)"
,EF BOUNDFILLER
,EFV BOUNDFILLER
,TSTAT
,FPVAL
,FPVALADJ
,PVALADJ
)

