OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/SampleOntology.dat'
truncate into table a2_SampleOntology
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEONTOLOGYID,SAMPLEID,ONTOLOGYTERMID,SOURCEMAPPING)

