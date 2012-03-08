-- delete bioentities from Esembl for xenopus laevis, this will cascade delete corresponding annotations and mappings
DELETE FROM A2_BIOENTITY WHERE ORGANISMID=57 and BIOENTITYTYPEID IN (1, 2);

-- xenopus laevis is not in Ensembl - delete annotation source
DELETE FROM A2_ANNOTATIONSRC WHERE name like 'xenopus laevis%'; 
