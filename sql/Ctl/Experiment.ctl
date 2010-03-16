OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Experiment.dat'
truncate into table a2_Experiment
fields terminated by "\t" 
TRAILING NULLCOLS
(EXPERIMENTID,ACCESSION,DESCRIPTION CHAR(2000),PERFORMER,LAB)

