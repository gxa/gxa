OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/AssayPropertyValue.dat'
truncate into table a2_AssayPropertyValue
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYPROPERTYVALUEID,ASSAYID,PROPERTYVALUEID,ISFACTORVALUE)

