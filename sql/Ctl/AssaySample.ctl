OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/AssaySample.dat'
truncate into table a2_AssaySample
fields terminated by "\t" optionally enclosed by '"'
TRAILING NULLCOLS
(ASSAYSAMPLEID,ASSAYID,SAMPLEID)

