OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/AssayPVOntology.dat'
truncate into table a2_AssayPVOntology
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYPVONTOLOGYID,ONTOLOGYTERMID,ASSAYPVID)

