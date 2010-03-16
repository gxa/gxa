OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/SamplePropertyValue.dat'
truncate into table a2_SamplePropertyValue
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEPROPERTYVALUEID,SAMPLEID,PROPERTYVALUEID,ISFACTORVALUE)

