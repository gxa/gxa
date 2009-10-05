begin
--AE1__ASSAY_RNAI__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 1, VALUE from AE1__ASSAY_RNAI__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=1 and pv.Name = v.Value );
--AE1__ASSAY_AGE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 2, VALUE from AE1__ASSAY_AGE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=2 and pv.Name = v.Value );
--AE1__ASSAY_CELLLINE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 3, VALUE from AE1__ASSAY_CELLLINE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=3 and pv.Name = v.Value );
--AE1__ASSAY_CELLTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 4, VALUE from AE1__ASSAY_CELLTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=4 and pv.Name = v.Value );
--AE1__ASSAY_CLINHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 5, VALUE from AE1__ASSAY_CLINHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=5 and pv.Name = v.Value );
--AE1__ASSAY_CLININFO__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 6, VALUE from AE1__ASSAY_CLININFO__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=6 and pv.Name = v.Value );
--AE1__ASSAY_CLINTREATMENT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 7, VALUE from AE1__ASSAY_CLINTREATMENT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=7 and pv.Name = v.Value );
--AE1__ASSAY_COMPOUND__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 8, VALUE from AE1__ASSAY_COMPOUND__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=8 and pv.Name = v.Value );
--AE1__ASSAY_DEVSTAGE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 9, VALUE from AE1__ASSAY_DEVSTAGE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=9 and pv.Name = v.Value );
--AE1__ASSAY_DISEASESTAGING__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 11, VALUE from AE1__ASSAY_DISEASESTAGING__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=11 and pv.Name = v.Value );
--AE1__ASSAY_DISEASESTATE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 12, VALUE from AE1__ASSAY_DISEASESTATE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=12 and pv.Name = v.Value );
--AE1__ASSAY_DOSE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 13, VALUE from AE1__ASSAY_DOSE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=13 and pv.Name = v.Value );
--AE1__ASSAY_ECOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 14, VALUE from AE1__ASSAY_ECOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=14 and pv.Name = v.Value );
--AE1__ASSAY_GENMODIF__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 15, VALUE from AE1__ASSAY_GENMODIF__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=15 and pv.Name = v.Value );
--AE1__ASSAY_GENOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 16, VALUE from AE1__ASSAY_GENOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=16 and pv.Name = v.Value );
--AE1__ASSAY_GROWTHCONDITION__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 17, VALUE from AE1__ASSAY_GROWTHCONDITION__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=17 and pv.Name = v.Value );
--AE1__ASSAY_HISTOLOGY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 18, VALUE from AE1__ASSAY_HISTOLOGY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=18 and pv.Name = v.Value );
--AE1__ASSAY_INDGENETICCHAR__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 19, VALUE from AE1__ASSAY_INDGENETICCHAR__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=19 and pv.Name = v.Value );
--AE1__ASSAY_INDIVIDUAL__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 20, VALUE from AE1__ASSAY_INDIVIDUAL__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=20 and pv.Name = v.Value );
--AE1__ASSAY_INFECT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 21, VALUE from AE1__ASSAY_INFECT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=21 and pv.Name = v.Value );
--AE1__ASSAY_INJURY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 22, VALUE from AE1__ASSAY_INJURY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=22 and pv.Name = v.Value );
--AE1__ASSAY_LIGHT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 23, VALUE from AE1__ASSAY_LIGHT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=23 and pv.Name = v.Value );
--AE1__ASSAY_MATERIALTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 24, VALUE from AE1__ASSAY_MATERIALTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=24 and pv.Name = v.Value );
--AE1__ASSAY_MEDIA__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 25, VALUE from AE1__ASSAY_MEDIA__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=25 and pv.Name = v.Value );
--AE1__ASSAY_OBSERVATION__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 26, VALUE from AE1__ASSAY_OBSERVATION__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=26 and pv.Name = v.Value );
--AE1__ASSAY_ORGANISM__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 27, VALUE from AE1__ASSAY_ORGANISM__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=27 and pv.Name = v.Value );
--AE1__ASSAY_ORGANISMPART__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 28, VALUE from AE1__ASSAY_ORGANISMPART__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=28 and pv.Name = v.Value );
--AE1__ASSAY_PHENOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 29, VALUE from AE1__ASSAY_PHENOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=29 and pv.Name = v.Value );
--AE1__ASSAY_PROTOCOLTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 30, VALUE from AE1__ASSAY_PROTOCOLTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=30 and pv.Name = v.Value );
--AE1__ASSAY_REPLICATE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 31, VALUE from AE1__ASSAY_REPLICATE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=31 and pv.Name = v.Value );
--AE1__ASSAY_SEX__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 32, VALUE from AE1__ASSAY_SEX__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=32 and pv.Name = v.Value );
--AE1__ASSAY_STRAINORLINE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 33, VALUE from AE1__ASSAY_STRAINORLINE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=33 and pv.Name = v.Value );
--AE1__ASSAY_TARGETCELLTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 34, VALUE from AE1__ASSAY_TARGETCELLTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=34 and pv.Name = v.Value );
--AE1__ASSAY_TEMPERATURE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 35, VALUE from AE1__ASSAY_TEMPERATURE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=35 and pv.Name = v.Value );
--AE1__ASSAY_TESTRESULT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 36, VALUE from AE1__ASSAY_TESTRESULT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=36 and pv.Name = v.Value );
--AE1__ASSAY_TIME__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 37, VALUE from AE1__ASSAY_TIME__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=37 and pv.Name = v.Value );
--AE1__ASSAY_TUMORGRADING__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 38, VALUE from AE1__ASSAY_TUMORGRADING__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=38 and pv.Name = v.Value );
--AE1__ASSAY_VEHICLE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 39, VALUE from AE1__ASSAY_VEHICLE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=39 and pv.Name = v.Value );
--AE1__ASSAY_BIOMETRIC__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 104, VALUE from AE1__ASSAY_BIOMETRIC__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=104 and pv.Name = v.Value );
--AE1__ASSAY_CULTIVAR__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 105, VALUE from AE1__ASSAY_CULTIVAR__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=105 and pv.Name = v.Value );
--AE1__ASSAY_DISEASELOC__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 106, VALUE from AE1__ASSAY_DISEASELOC__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=106 and pv.Name = v.Value );
--AE1__ASSAY_ENVHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 107, VALUE from AE1__ASSAY_ENVHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=107 and pv.Name = v.Value );
--AE1__ASSAY_FAMILYHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 108, VALUE from AE1__ASSAY_FAMILYHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=108 and pv.Name = v.Value );
--AE1__ASSAY_GENERATION__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 109, VALUE from AE1__ASSAY_GENERATION__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=109 and pv.Name = v.Value );
--AE1__ASSAY_INITIALTIME__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 110, VALUE from AE1__ASSAY_INITIALTIME__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=110 and pv.Name = v.Value );
--AE1__ASSAY_ORGANISMSTATUS__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 111, VALUE from AE1__ASSAY_ORGANISMSTATUS__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=111 and pv.Name = v.Value );
--AE1__ASSAY_PERFORMER__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 112, VALUE from AE1__ASSAY_PERFORMER__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=112 and pv.Name = v.Value );
--AE1__ASSAY_POPULATION__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 113, VALUE from AE1__ASSAY_POPULATION__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=113 and pv.Name = v.Value );
--AE1__ASSAY_QCDESCRTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 114, VALUE from AE1__ASSAY_QCDESCRTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=114 and pv.Name = v.Value );
--AE1__ASSAY_SOURCEPROVIDER__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 115, VALUE from AE1__ASSAY_SOURCEPROVIDER__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=115 and pv.Name = v.Value );
--AE1__ASSAY_TESTTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 116, VALUE from AE1__ASSAY_TESTTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=116 and pv.Name = v.Value );
--AE1__ASSAY_TEST__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 117, VALUE from AE1__ASSAY_TEST__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=117 and pv.Name = v.Value );
end;

begin
--AE1__SAMPLE_AGE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 2, VALUE from AE1__SAMPLE_AGE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=2 and pv.Name = v.Value );
--AE1__SAMPLE_CELLLINE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 3, VALUE from AE1__SAMPLE_CELLLINE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=3 and pv.Name = v.Value );
--AE1__SAMPLE_CELLTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 4, VALUE from AE1__SAMPLE_CELLTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=4 and pv.Name = v.Value );
--AE1__SAMPLE_CLINHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 5, VALUE from AE1__SAMPLE_CLINHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=5 and pv.Name = v.Value );
--AE1__SAMPLE_CLININFO__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 6, VALUE from AE1__SAMPLE_CLININFO__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=6 and pv.Name = v.Value );
--AE1__SAMPLE_CLINTREATMENT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 7, VALUE from AE1__SAMPLE_CLINTREATMENT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=7 and pv.Name = v.Value );
--AE1__SAMPLE_DEVSTAGE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 9, VALUE from AE1__SAMPLE_DEVSTAGE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=9 and pv.Name = v.Value );
--AE1__SAMPLE_DISEASESTAGING__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 11, VALUE from AE1__SAMPLE_DISEASESTAGING__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=11 and pv.Name = v.Value );
--AE1__SAMPLE_DISEASESTATE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 12, VALUE from AE1__SAMPLE_DISEASESTATE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=12 and pv.Name = v.Value );
--AE1__SAMPLE_ECOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 14, VALUE from AE1__SAMPLE_ECOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=14 and pv.Name = v.Value );
--AE1__SAMPLE_GENMODIF__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 15, VALUE from AE1__SAMPLE_GENMODIF__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=15 and pv.Name = v.Value );
--AE1__SAMPLE_GENOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 16, VALUE from AE1__SAMPLE_GENOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=16 and pv.Name = v.Value );
--AE1__SAMPLE_HISTOLOGY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 18, VALUE from AE1__SAMPLE_HISTOLOGY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=18 and pv.Name = v.Value );
--AE1__SAMPLE_INDGENETICCHAR__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 19, VALUE from AE1__SAMPLE_INDGENETICCHAR__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=19 and pv.Name = v.Value );
--AE1__SAMPLE_INDIVIDUAL__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 20, VALUE from AE1__SAMPLE_INDIVIDUAL__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=20 and pv.Name = v.Value );
--AE1__SAMPLE_OBSERVATION__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 26, VALUE from AE1__SAMPLE_OBSERVATION__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=26 and pv.Name = v.Value );
--AE1__SAMPLE_ORGANISMPART__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 28, VALUE from AE1__SAMPLE_ORGANISMPART__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=28 and pv.Name = v.Value );
--AE1__SAMPLE_PHENOTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 29, VALUE from AE1__SAMPLE_PHENOTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=29 and pv.Name = v.Value );
--AE1__SAMPLE_SEX__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 32, VALUE from AE1__SAMPLE_SEX__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=32 and pv.Name = v.Value );
--AE1__SAMPLE_STRAINORLINE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 33, VALUE from AE1__SAMPLE_STRAINORLINE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=33 and pv.Name = v.Value );
--AE1__SAMPLE_TARGETCELLTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 34, VALUE from AE1__SAMPLE_TARGETCELLTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=34 and pv.Name = v.Value );
--AE1__SAMPLE_TESTRESULT__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 36, VALUE from AE1__SAMPLE_TESTRESULT__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=36 and pv.Name = v.Value );
--AE1__SAMPLE_TUMORGRADING__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 38, VALUE from AE1__SAMPLE_TUMORGRADING__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=38 and pv.Name = v.Value );
--AE1__SAMPLE_BIOMETRIC__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 104, VALUE from AE1__SAMPLE_BIOMETRIC__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=104 and pv.Name = v.Value );
--AE1__SAMPLE_CULTIVAR__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 105, VALUE from AE1__SAMPLE_CULTIVAR__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=105 and pv.Name = v.Value );
--AE1__SAMPLE_DISEASELOC__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 106, VALUE from AE1__SAMPLE_DISEASELOC__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=106 and pv.Name = v.Value );
--AE1__SAMPLE_ENVHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 107, VALUE from AE1__SAMPLE_ENVHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=107 and pv.Name = v.Value );
--AE1__SAMPLE_FAMILYHISTORY__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 108, VALUE from AE1__SAMPLE_FAMILYHISTORY__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=108 and pv.Name = v.Value );
--AE1__SAMPLE_INITIALTIME__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 110, VALUE from AE1__SAMPLE_INITIALTIME__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=110 and pv.Name = v.Value );
--AE1__SAMPLE_ORGANISMSTATUS__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 111, VALUE from AE1__SAMPLE_ORGANISMSTATUS__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=111 and pv.Name = v.Value );
--AE1__SAMPLE_SOURCEPROVIDER__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 115, VALUE from AE1__SAMPLE_SOURCEPROVIDER__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=115 and pv.Name = v.Value );
--AE1__SAMPLE_TESTTYPE__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 116, VALUE from AE1__SAMPLE_TESTTYPE__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=116 and pv.Name = v.Value );
--AE1__SAMPLE_ALL__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 118, VALUE from AE1__SAMPLE_ALL__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=118 and pv.Name = v.Value );
--AE1__SAMPLE_TEST__DM
insert into a2_PropertyValue(PropertyID,Name) select distinct 119, VALUE from AE1__SAMPLE_TEST__DM@dwprd v where not exists (select 1 from a2_PropertyValue pv where pv.PropertyID=119 and pv.Name = v.Value );
end;

COMMIT WORK;
