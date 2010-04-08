OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/SamplePV.dat'
truncate into table a2_SamplePV
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEPVID,SAMPLEID,PROPERTYVALUEID,ISFACTORVALUE)

