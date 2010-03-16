OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Sample.dat'
truncate into table a2_Sample
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEID,ACCESSION,SPECIES,CHANNEL)

