alter table a2_property add displayname varchar2(512);
alter table a2_propertyvalue add displayname varchar2(512);

-- alter table a2_property drop column displayname;
-- alter table a2_propertyvalue drop column displayname;

update a2_property set displayname='Clinical info' where name='clinical_information';
update a2_property set displayname='Compound treatment' where name='compound';
update a2_property set displayname='Genotype' where name='genotype';
update a2_property set displayname='Genotype' where name='individual_genetic_characteristics';
update a2_property set displayname='Infection' where name='infect';
update a2_property set displayname='RNAi' where name='rnai';
update a2_property set displayname='Target cell type' where name='targeted_cell_type';
update a2_property set displayname='Blood/non-blood meta-groups' where name='blood_nonblood_meta_groups';

------------------------------------------------------------------------
-- The following statements refer to the factors never used in our DB
------------------------------------------------------------------------
update a2_property set displayname='Disease location' where name='diseaseloc';
update a2_property set displayname='Biometric measurement' where name='biometric';
update a2_property set displayname='Initial time' where name='initialtime';
update a2_property set displayname='QC description' where name='qcdescrtype';
update a2_property set displayname='Source provider' where name='source_provider';
update a2_property set displayname='Any factor' where name='anything';
update a2_property set displayname='EFO' where name='efo';
