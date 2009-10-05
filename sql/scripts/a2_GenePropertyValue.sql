begin
--CAS
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 31, VALUE from AE2__GENE_CAS__DM@dwprd;
--CHEBI
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 32, VALUE from AE2__GENE_CHEBI__DM@dwprd;
--CHEMFORMULA
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 33, VALUE from AE2__GENE_CHEMFORMULA__DM@dwprd;
--DBXREF
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 34, VALUE from AE2__GENE_DBXREF__DM@dwprd;
--DESC
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 35, VALUE from AE2__GENE_DESC__DM@dwprd;
--DISEASE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 36, VALUE from AE2__GENE_DISEASE__DM@dwprd;
--EMBL
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 37, VALUE from AE2__GENE_EMBL__DM@dwprd;
--ENSFAMILY
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 38, VALUE from AE2__GENE_ENSFAMILY__DM@dwprd;
--ENSGENE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 39, VALUE from AE2__GENE_ENSGENE__DM@dwprd;
--ENSPROTEIN
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 40, VALUE from AE2__GENE_ENSPROTEIN__DM@dwprd;
--ENSTRANSCRIPT
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 41, VALUE from AE2__GENE_ENSTRANSCRIPT__DM@dwprd;
--GOTERM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 42, VALUE from AE2__GENE_GOTERM__DM@dwprd;
--GO
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 43, VALUE from AE2__GENE_GO__DM@dwprd;
--HMDB
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 44, VALUE from AE2__GENE_HMDB__DM@dwprd;
--IDS
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 45, VALUE from AE2__GENE_IDS__DM@dwprd;
--IMAGE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 46, VALUE from AE2__GENE_IMAGE__DM@dwprd;
--INTERPROTERM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 47, VALUE from AE2__GENE_INTERPROTERM__DM@dwprd;
--INTERPRO
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 48, VALUE from AE2__GENE_INTERPRO__DM@dwprd;
--KEYWORD
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 49, VALUE from AE2__GENE_KEYWORD__DM@dwprd;
--LOCUSLINK
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 50, VALUE from AE2__GENE_LOCUSLINK__DM@dwprd;
--OMIM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 51, VALUE from AE2__GENE_OMIM__DM@dwprd;
--ORF
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 52, VALUE from AE2__GENE_ORF__DM@dwprd;
--ORTHOLOG
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 53, VALUE from AE2__GENE_ORTHOLOG__DM@dwprd;
--PATHWAY
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 54, VALUE from AE2__GENE_PATHWAY__DM@dwprd;
--PROTEINNAME
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 55, VALUE from AE2__GENE_PROTEINNAME__DM@dwprd;

end;

COMMIT WORK;