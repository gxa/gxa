OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/SamplePVOntology.dat'
truncate into table a2_SamplePVOntology
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEPVONTOLOGYID,ONTOLOGYTERMID,SAMPLEPVID)

