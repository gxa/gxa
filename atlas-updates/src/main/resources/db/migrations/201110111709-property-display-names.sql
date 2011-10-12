alter table a2_property add displayname varchar2(512);
alter table a2_propertyvalue add displayname varchar2(512);

-- alter table a2_property drop column displayname;
-- alter table a2_propertyvalue drop column displayname;

update a2_property set displayname='Age' where name='age';

update a2_property set displayname='Biometric measurement' where name='biometric';
--  0 rows updated.

update a2_property set displayname='Cell line' where name='cell_line';
update a2_property set displayname='Cell type' where name='cell_type';
update a2_property set displayname='Clinical history' where name='clinical_history';
update a2_property set displayname='Clinical info' where name='clinical_information';
update a2_property set displayname='Clinical treatment' where name='clinical_treatment';
update a2_property set displayname='Compound treatment' where name='compound';
update a2_property set displayname='Cultivar' where name='cultivar';
update a2_property set displayname='Developmental stage' where name='developmental_stage';

update a2_property set displayname='Disease location' where name='diseaseloc';
--  0 rows updated.

update a2_property set displayname='Disease staging' where name='disease_staging';
update a2_property set displayname='Disease state' where name='disease_state';
update a2_property set displayname='Dose' where name='dose';
update a2_property set displayname='Ecotype' where name='ecotype';
update a2_property set displayname='Environmental history' where name='environmental_history';
update a2_property set displayname='Environmental stress ' where name='environmental_stress';
update a2_property set displayname='Family history' where name='family_history';
update a2_property set displayname='Genetic modification' where name='genetic_modification';
update a2_property set displayname='Genotype' where name='genotype';
update a2_property set displayname='Growth condition' where name='growth_condition';
update a2_property set displayname='Histology' where name='histology';
update a2_property set displayname='Genotype' where name='individual_genetic_characteristics';
update a2_property set displayname='Individual' where name='individual';
update a2_property set displayname='Infection' where name='infect';

update a2_property set displayname='Initial time' where name='initialtime';
--  0 rows updated.

update a2_property set displayname='Injury' where name='injury';
update a2_property set displayname='Light' where name='light';
update a2_property set displayname='Material type' where name='material_type';
update a2_property set displayname='Media' where name='media';
update a2_property set displayname='Observation' where name='observation';
update a2_property set displayname='Organism' where name='organism';
update a2_property set displayname='Organism part' where name='organism_part';
update a2_property set displayname='Organism status' where name='organism_status';
update a2_property set displayname='Performer' where name='performer';
update a2_property set displayname='Phenotype' where name='phenotype';
update a2_property set displayname='Population' where name='population';
update a2_property set displayname='Protocol type' where name='protocol_type';

update a2_property set displayname='QC description' where name='qcdescrtype';
--  0 rows updated.

update a2_property set displayname='Replicate' where name='replicate';
update a2_property set displayname='RNAi' where name='rnai';
update a2_property set displayname='Sex' where name='sex';

update a2_property set displayname='Source provider' where name='source_provider';
--  0 rows updated.

update a2_property set displayname='Strain or line' where name='strain_or_line';
update a2_property set displayname='Target cell type' where name='targeted_cell_type';
update a2_property set displayname='Temperature' where name='temperature';
update a2_property set displayname='Test' where name='test';
update a2_property set displayname='Test result' where name='test_result';
update a2_property set displayname='Test type' where name='test_type';
update a2_property set displayname='Time' where name='time';
update a2_property set displayname='Tumor grading' where name='tumor_grading';
update a2_property set displayname='Tumor size' where name='tumor_size';
update a2_property set displayname='Vehicle' where name='vehicle';
update a2_property set displayname='Generation' where name='generation';

update a2_property set displayname='Any factor' where name='anything';
--  0 rows updated.

update a2_property set displayname='Experiment' where name='experiment';

update a2_property set displayname='EFO' where name='efo';
--  0 rows updated.
