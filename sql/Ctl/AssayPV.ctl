OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/AssayPV.dat'
truncate into table a2_AssayPV
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYPVID,ASSAYID,PROPERTYID FILLER, PROPERTYVALUEID)
