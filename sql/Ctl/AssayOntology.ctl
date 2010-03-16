OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/AssayOntology.dat'
truncate into table a2_AssayOntology
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYONTOLOGYID,ASSAYID,ONTOLOGYTERMID,SOURCEMAPPING)

