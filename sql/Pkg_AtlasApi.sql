--------------------------------------------------------
--  DDL for Package ATLASAPI
--------------------------------------------------------

create or replace
PACKAGE "ATLASAPI" IS

/*******************************************************************************/
/*******************************************************************************/

TYPE CurGene             IS REF CURSOR RETURN vwGene%ROWTYPE;
TYPE CurAssay            IS REF CURSOR RETURN vwAssay%ROWTYPE;
TYPE CurAssayProperty    IS REF CURSOR RETURN vwAssayProperty%ROWTYPE;
TYPE CurAssaySample      IS REF CURSOR RETURN vwAssaySample%ROWTYPE;
TYPE CurExperiment       IS REF CURSOR RETURN vwExperiment%ROWTYPE;
TYPE CurExperimentAssay  IS REF CURSOR RETURN vwExperimentAssay%ROWTYPE;
TYPE CurExperimentSample IS REF CURSOR RETURN vwExperimentSample%ROWTYPE;
TYPE CurSample           IS REF CURSOR RETURN vwSample%ROWTYPE;
TYPE CurSampleProperty   IS REF CURSOR RETURN vwSampleProperty%ROWTYPE;
TYPE CurSampleAssay      IS REF CURSOR RETURN vwSampleAssay%ROWTYPE;  
TYPE CurArrayDesign      IS REF CURSOR RETURN vwArrayDesign%ROWTYPE;  
TYPE CurArrayDesignElement    IS REF CURSOR RETURN vwArrayDesignElement%ROWTYPE;  
TYPE CurPropertyValue         IS REF CURSOR RETURN vwPropertyValue%ROWTYPE;  
TYPE CurGenePropertyValue     IS REF CURSOR RETURN vwGenePropertyValue%ROWTYPE;

TYPE ExpressionValueAssayRec IS RECORD(AssayID int);
TYPE ExpressionValueDERec IS RECORD(DesignElementID int);
TYPE ExpressionValueRec IS RECORD(AssayID int, DesignElementID int, value float);

TYPE CurExpressionValueAssay  IS REF CURSOR RETURN ExpressionValueAssayRec;
TYPE CurExpressionValueDE     IS REF CURSOR RETURN ExpressionValueDERec; 
TYPE CurExpressionValues      IS REF CURSOR RETURN ExpressionValueRec;      

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_GENEGET_ID (
 geneQuery IN GeneQuery
) return TBLINT;

FUNCTION a2_arraydesignget_id ( 
 arrayDesignQuery IN ArrayDesignQuery
) RETURN TBLINT;
  
FUNCTION A2_ASSAYGET_ID (
 assayQuery IN AssayQuery
) return TBLINT;
  
FUNCTION A2_EXPERIMENTGET_ID (
 ExperimentQuery IN ExperimentQuery
) return TBLINT;
  
FUNCTION A2_PROPERTYGET_ID (
 PropertyQuery IN PropertyQuery
) RETURN TBLINT;
  
FUNCTION A2_SAMPLEGET_ID (
 sampleQuery IN SampleQuery
) return TBLINT;

FUNCTION A2_GENEPROPERTYGET_ID (
 genePropertyQuery IN GenePropertyQuery
) RETURN TBLINT;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE A2_GENEGET(
 geneQuery      IN   GeneQuery  
,pageSortParams IN   PageSortParams    
,genes          OUT  AtlasAPI.CurGene
,properties     OUT  AtlasAPI.CurGenePropertyValue
);
  
PROCEDURE a2_ArrayDesignGet(
 arrayDesignQuery IN  ArrayDesignQuery
,pageSortParams   IN  PageSortParams
,arrayDesigns     OUT AtlasAPI.CurArrayDesign
,designElements   OUT AtlasAPI.CurArrayDesignElement
);

PROCEDURE          A2_ASSAYGET(
 assayQuery     IN  AssayQuery  
,pageSortParams IN  PageSortParams    
,assays         OUT AtlasAPI.CurAssay
,samples        OUT AtlasAPI.CurAssaySample
,properties     OUT AtlasAPI.CurAssayProperty
);

PROCEDURE          A2_EXPERIMENTGET(
 experimentQuery IN  ExperimentQuery  
,pageSortParams  IN  PageSortParams    
,experiments     OUT AtlasAPI.CurExperiment
,samples         OUT AtlasAPI.CurExperimentSample
,assays          OUT AtlasAPI.CurExperimentAssay
);

PROCEDURE          A2_SAMPLEGET(
 sampleQuery    IN  SampleQuery  
,pageSortParams IN  PageSortParams    
,samples        OUT AtlasAPI.CurSample
,assays         OUT AtlasAPI.CurSampleAssay 
,properties     OUT AtlasAPI.CurSampleProperty
);  

PROCEDURE A2_PROPERTYGET(
   propertyQuery    IN    PropertyQuery  
   ,pageSortParams  IN    PageSortParams    
   ,properties      OUT   AtlasAPI.CurPropertyValue
);

PROCEDURE A2_GENEPROPERTYGET(
   genePropertyQuery    IN    GenePropertyQuery  
   ,pageSortParams      IN    PageSortParams    
   ,properties          OUT   AtlasAPI.CurGenePropertyValue
);

PROCEDURE A2_EXPRESSIONVALUEGET(
    experimentID    IN int
    ,arrayDesignID  IN int
    ,assays           OUT AtlasAPI.CurExpressionValueAssay
    ,designElements   OUT AtlasAPI.CurExpressionValueDE
    ,expressionValues OUT AtlasAPI.CurExpressionValues
);
  
END AtlasAPI;
/
  CREATE OR REPLACE PACKAGE BODY "ATLASAPI" AS

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_ARRAYDESIGNGET_ID( 
  arrayDesignQuery IN ArrayDesignQuery
)
RETURN TBLINT AS
  result TBLINT;
BEGIN
  
  Select INTRECORD(ad.ArrayDesignID) BULK COLLECT INTO result
  from a2_ArrayDesign ad
  where ((arrayDesignQuery.Name is null) or (ad.Name like arrayDesignQuery.Name))
  ;
  
  RETURN result;
END;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_ASSAYGET_ID(
  assayQuery IN AssayQuery
)
return
 TBLINT
is
 result TBLINT;
begin
  
  select INTRECORD(AssayID) BULK COLLECT INTO result
  from a2_Assay a
  where a.assayid = COALESCE(assayQuery."AccessionQuery".ID,a.AssayID)
  and a.accession = COALESCE(assayQuery."AccessionQuery".Accession,a.accession)
  and exists (select 1 from a2_assaypropertyvalue apv 
              join TABLE(CAST(assayQuery.properties as TBLINT)) t on t.ID = apv.PropertyValueID
              where apv.AssayID = a.AssayID );
  
  return result;
end;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_EXPERIMENTGET_ID(
  ExperimentQuery IN ExperimentQuery
)
return
 TBLINT
is
 result TBLINT;
begin
  
  select INTRECORD(ExperimentID) BULK COLLECT INTO result
  from a2_Experiment a
  where a.experimentid = COALESCE(ExperimentQuery."AccessionQuery".ID,a.ExperimentID)
  and a.accession = COALESCE(ExperimentQuery."AccessionQuery".Accession,a.accession);
  /*and exists (select 1 from vwexperimentfactors ef 
              join TABLE(CAST(ExperimentQuery.properties as TBLINT)) t on t.ID = ef.PropertyValueID
              where ef.ExperimentID = a.ExperimentID );*/
  
  return result;
end;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_GENEGET_ID(
  geneQuery IN GeneQuery
)
return
 TBLINT
is
 result TBLINT;
 TmpResult TBLINT;
begin
  dbms_output.put_line('A2_GENEGET_ID started');
  
  select INTRECORD(GeneID) BULK COLLECT INTO result
  from a2_Gene a
  where a.geneid = COALESCE(geneQuery."AccessionQuery".ID,a.GeneID)
  and a.Identifier = COALESCE(geneQuery."AccessionQuery".Accession,a.Identifier);
 
  DBMS_OUTPUT.PUT_LINE('filter 1:' || result.count);
 
 /*
  if(not(GeneQuery.species is null) and GeneQuery.species != '') then
    
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpResult
    from (select distinct pv.ID
    from TABLE(CAST(result as TBLINT)) pv 
    join a2_AssayPropertyValue aa on aa.propertyvalueid = pv.id
    join TABLE(CAST(PropertyQuery.assays AS TBLINT)) t on t.ID = aa.AssayID ) t;
  
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 2:' || result.count);
  end if;
  */
 
  /* and exists (select 1 from a2_genepropertyvalue apv 
              join TABLE(CAST(geneQuery.properties as TBLINT)) t on t.ID = apv.GenePropertyValueID
              where apv.GeneID = a.GeneID );*/
  
  return result;
end;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_PROPERTYGET_ID (
  PropertyQuery IN PropertyQuery
)
RETURN TBLINT AS
  result TBLINT;
  TmpPropertyValueIDs TBLINT; --refine
  PropertyCount int := 0;
BEGIN
  
  dbms_output.put_line('A2_PROPERTYGET_ID started');
  
  --select count(1) INTO PropertyCount from TABLE(CAST(PropertyQuery.assays AS TBLINT)) ;
  
  Select INTRECORD(pv.PropertyValueID) BULK COLLECT INTO result
  from a2_PropertyValue pv
  where ((PropertyQuery.PropertyValue is null) or (pv.Name = PropertyQuery.PropertyValue))
  and ((COALESCE(PropertyQuery.PropertyID,0) = 0) or (pv.PropertyID = PropertyQuery.PropertyID))
  and ((PropertyQuery.FullTextQuery is null) or (pv.Name like PropertyQuery.FullTextQuery));
  
  DBMS_OUTPUT.PUT_LINE('filter 1:' || result.count);
  
  if(not(PropertyQuery.assays is null) and PropertyQuery.assays.count > 0) then
    
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpPropertyValueIDs
    from (select distinct pv.ID
    from TABLE(CAST(result as TBLINT)) pv 
    join a2_AssayPropertyValue aa on aa.propertyvalueid = pv.id
    join TABLE(CAST(PropertyQuery.assays AS TBLINT)) t on t.ID = aa.AssayID ) t;
  
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 2:' || result.count);
  end if;
  
  if (1 = PropertyQuery.IsAssay) then
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpPropertyValueIDs
    from (select distinct pv.ID
    from TABLE(CAST(result as TBLINT)) pv 
    join a2_AssayPropertyValue aa on aa.propertyvalueid = pv.id) t;
    
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 3:' || result.count);
  end if;
 
  if(not(PropertyQuery.samples is null) and (PropertyQuery.samples.count > 0)) then
    
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpPropertyValueIDs
    from (select pv.ID
    from TABLE(CAST(result as TBLINT)) pv 
    join a2_SamplePropertyValue aa on aa.propertyvalueid = pv.id
    join TABLE(CAST(PropertyQuery.samples AS TBLINT)) t on t.ID = aa.SampleID) t;
  
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 4:' || result.count);
  end if;
  
  
  if(not(PropertyQuery.experiments is null) and (PropertyQuery.experiments.count > 0)) then
    
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpPropertyValueIDs
    from (Select distinct pv.ID from TABLE(CAST(result as TBLINT)) pv 
    join vwexperimentfactors aa on aa.propertyvalueid = pv.id
    join TABLE(CAST(PropertyQuery.samples AS TBLINT)) t on t.ID = aa.ExperimentID) t ;
  
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 5:' || result.count);
  end if;
 
  /*
  and ( or (
      exists( select count(1) 
       return only properties associated with ALL given assays (AND) 
      PropertyCount = (select count(1)  from a2_AssayPropertyValue aa
       join TABLE(CAST(PropertyQuery.assays AS TBLINT)) t on t.ID = aa.AssayID 
       where aa.PropertyValueID = pv.propertyvalueid)
  ))
  and ((PropertyQuery.samples is null) or (exists 
      (select count(1) from a2_SamplePropertyValue aa
       join TABLE(CAST(PropertyQuery.samples AS TBLINT)) t on t.ID = aa.SampleID 
       where aa.PropertyValueID = pv.propertyvalueid)
  ))
  and ((PropertyQuery.experiments is null) or (exists(select 1 from vwexperimentfactors ef
        join TABLE(CAST(PropertyQuery.samples AS TBLINT)) t on t.ID = ef.ExperimentID 
        where ef.propertyvalueid = pv.propertyvalueid))    
  ); */
  
  RETURN result;
END A2_PROPERTYGET_ID;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_SAMPLEGET_ID (
  sampleQuery IN SampleQuery
)
return
 TBLINT
is
 result TBLINT;
 TmpPropertyValueIDs TBLINT; --refine
begin

  DBMS_OUTPUT.PUT_LINE('A2_SAMPLEGET_ID started');
  
  select INTRECORD(SampleID) BULK COLLECT INTO result
  from a2_Sample a
  where a.sampleid = COALESCE(sampleQuery."AccessionQuery".ID,a.SampleID)
  and a.accession = COALESCE(sampleQuery."AccessionQuery".Accession,a.accession);
  
  DBMS_OUTPUT.PUT_LINE('filter 0:' || result.count);
  
  if(not(sampleQuery.properties is null) and sampleQuery.properties.count > 0) then
  
    Select INTRECORD(t.ID) BULK COLLECT INTO TmpPropertyValueIDs
    from (Select distinct r.ID from TABLE(CAST(result as TBLINT)) r 
    join a2_SamplePropertyValue aa on aa.SampleID = r.id
    join TABLE(CAST(sampleQuery.properties AS TBLINT)) p on p.ID = aa.PropertyValueID) t ;
  
    result := TmpPropertyValueIDs;
    
    DBMS_OUTPUT.PUT_LINE('filter 1:' || result.count);

  end if;

  return result;
end;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE A2_ARRAYDESIGNGET(
   arrayDesignQuery IN  ArrayDesignQuery
  ,pageSortParams   IN  PageSortParams
  ,arrayDesigns     OUT AtlasAPI.CurArrayDesign
  ,designElements   OUT AtlasAPI.CurArrayDesignElement
)
as
  IDs TBLINT;
  IDsPage TBLINT;
begin
  IDs := a2_ArrayDesignGet_ID(arrayDesignQuery);
  
  Select INTRECORD(ID) BULK COLLECT INTO IDsPage
  from TABLE(CAST(IDs as TBLINT))
  where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;

  open arrayDesigns for
  select vw.*
  from vwArrayDesign vw
  join TABLE(CAST(IDsPage as TBLINT)) t on t.id = vw.ArrayDesignID;
  
  open designElements for
  select vw.* 
  from vwArrayDesignElement vw
  join TABLE(CAST(IDsPage as TBLINT)) t on t.id = vw.ArrayDesignID;
  
end;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE A2_ASSAYGET(
   assayQuery      IN  AssayQuery  
   ,pageSortParams IN  PageSortParams    
   ,assays         OUT AtlasAPI.CurAssay
   ,samples        OUT AtlasAPI.CurAssaySample
   ,properties     OUT AtlasAPI.CurAssayProperty
)
as 
IDs     TBLINT;
IDsPage TBLINT;

begin
  
   IDs := a2_assayget_id(assayQuery);   
   
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;

   open assays for 
   select vw.*
   from vwAssay vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.AssayID
   order by t.ID;
    
   open samples for 
   select vw.*
   from vwAssaySample vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.AssayID  
   order by t.ID;
    
   open properties for
   select vw.*
   from vwAssayProperty vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.AssayID  
   order by t.ID;
    
end;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE  A2_EXPERIMENTGET(
   experimentQuery IN   ExperimentQuery  
   ,pageSortParams IN   PageSortParams    
   ,experiments    OUT  AtlasAPI.CurExperiment
   ,samples        OUT  AtlasAPI.CurExperimentSample
   ,assays         OUT  AtlasAPI.CurExperimentAssay
)
as 
IDs     TBLINT;
IDsPage TBLINT;

begin
  
   IDs := a2_experimentget_id(experimentQuery);   
   
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;

   open experiments for 
   select vw.*
   from vwExperiment vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.ExperimentID
   order by t.ID;
    
   open samples for 
   select vw.*
   from vwExperimentSample vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.ExperimentID  
   order by t.ID;
    
   open assays for
   select vw.*
   from vwExperimentAssay vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.ExperimentID  
   order by t.ID;
    
end;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE A2_GENEGET(
   geneQuery        IN  GeneQuery  
   ,pageSortParams  IN  PageSortParams    
   ,genes           OUT AtlasAPI.CurGene
   ,properties      OUT AtlasAPI.CurGenePropertyValue
)
as 
IDs     TBLINT;
IDsPage TBLINT;

ia INTARRAY;

cow integer;

TheGeneID integer;
begin
  
   IDs := a2_geneget_id(geneQuery);   
   
   /*
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;
   */
   
   select count(1) into cow from TABLE(CAST(IDs as TBLINT)) ;
   
   dbms_output.put_line(cow);

  if(/*cow*/ 2 > 1) then
   dbms_output.put_line('join temp table');
 
   open genes for 
   select /*+ CARDINALITY (t 1)*/ vw.*
   from vwgene vw
   join TABLE(CAST(IDs as TBLINT)) t on t.ID = vw.GeneID
   order by vw.GeneID;
    
   open properties for
   select /*+ CARDINALITY (t 1) */ vw.* 
   from TABLE(CAST(IDs as TBLINT)) t
   join vwGenePropertyValue vw on t.ID = vw.GeneID  
   --where vw.GeneID in ( ia /*Select ID from TABLE(CAST(IDs as TBLINT))*/) 
   order by t.ID;
   
  else  
   dbms_output.put_line('use single ID');
  
   Select Max(ID) INTO TheGeneID from TABLE(CAST(IDs as TBLINT));
  
   open genes for 
   select vw.*
   from vwgene vw
   where GeneID = TheGeneID;
    
   open properties for
   select vw.* 
   from vwGenePropertyValue vw   
   where vw.GeneID = TheGeneID;
   
  end if;
    
end;

/*******************************************************************************/
/*******************************************************************************/

FUNCTION A2_GENEPROPERTYGET_ID (
  GenePropertyQuery IN GenePropertyQuery
)
RETURN TBLINT AS
  result TBLINT;
  TmpPropertyValueIDs TBLINT; --refine
BEGIN
  
  dbms_output.put_line('A2_GENEPROPERTYGET_ID started');
  
  Select INTRECORD(pv.GenePropertyValueID) BULK COLLECT INTO result
  from a2_GenePropertyValue pv
  where ((GenePropertyQuery.PropertyValue is null) or (pv.Value = GenePropertyQuery.PropertyValue))
  and ((COALESCE(GenePropertyQuery.PropertyID,0) = 0) or (pv.GenePropertyID = GenePropertyQuery.PropertyID))
  and ((GenePropertyQuery.FullTextQuery is null) or (pv.Value like GenePropertyQuery.FullTextQuery));
  
  DBMS_OUTPUT.PUT_LINE('filter 1:' || result.count);
  
  RETURN result;
END A2_GENEPROPERTYGET_ID;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE a2_propertyGet(
   propertyQuery    IN    PropertyQuery  
   ,pageSortParams  IN    PageSortParams    
   ,properties      OUT   AtlasAPI.CurPropertyValue
)
as
IDs     TBLINT;
IDsPage TBLINT;

begin
  
   IDs := a2_propertyget_id(propertyQuery);   
   
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;
   
   open properties for 
        select vw.*
    from vwPropertyValue vw
    join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.PropertyValueID  
    order by t.ID;
  
end;

/*******************************************************************************/
/*******************************************************************************/

PROCEDURE A2_SAMPLEGET(
   sampleQuery      IN  SampleQuery  
   ,pageSortParams  IN  PageSortParams    
   ,samples         OUT AtlasAPI.CurSample
   ,assays          OUT AtlasAPI.CurSampleAssay
   ,properties      OUT AtlasAPI.CurSampleProperty
)
as 
IDs     TBLINT;
IDsPage TBLINT;

begin
  
   IDs := a2_sampleget_id(sampleQuery);   
   
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;

   open samples for 
   select vw.*
   from vwSample vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.SampleID
   order by t.ID;
    
   open assays for 
   select vw.*
   from vwSampleAssay vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.SampleID  
   order by t.ID;
    
   open properties for
   select vw.*
   from vwSampleProperty vw
   join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.SampleID  
   order by t.ID;
    
end;

PROCEDURE a2_GenePropertyGet(
   GenePropertyQuery    IN    GenePropertyQuery  
   ,pageSortParams  IN    PageSortParams    
   ,properties      OUT   AtlasAPI.CurGenePropertyValue
)
as
IDs     TBLINT;
IDsPage TBLINT;

begin
  
   IDs := a2_genepropertyget_id(genePropertyQuery);   
   
   Select INTRECORD(ID) BULK COLLECT INTO IDsPage
   from TABLE(CAST(IDs as TBLINT))
   where rownum between pageSortParams.StartRow and pageSortParams.StartRow + pageSortParams.NumRows;
   
   open properties for 
        select vw.*
    from vwGenePropertyValue vw
    join TABLE(CAST(IDsPage as TBLINT)) t on t.ID = vw.GenePropertyValueID  
    order by t.ID;
  
end;

/*******************************************************************************/
/*******************************************************************************/
PROCEDURE A2_EXPRESSIONVALUEGET(
    experimentID    IN int
    ,arrayDesignID  IN int
    ,assays           OUT AtlasAPI.CurExpressionValueAssay
    ,designElements   OUT AtlasAPI.CurExpressionValueDE
    ,expressionValues OUT AtlasAPI.CurExpressionValues
)
as
begin
  
  open assays for 
  select AssayID 
  from a2_Assay 
  where ExperimentID = A2_EXPRESSIONVALUEGET.ExperimentID
  and ArrayDesignID = A2_EXPRESSIONVALUEGET.ArrayDesignID
  order by Accession;

  open designElements for
  select DesignElementID
  from a2_DesignElement 
  where ArrayDesignID = A2_EXPRESSIONVALUEGET.ArrayDesignID
  order by Accession;
  
  open expressionValues for 
  select ev.AssayID, ev.DesignElementID, Value
  from a2_ExpressionValue ev
  join a2_Assay a on a.AssayID = ev.AssayID
  join a2_DesignElement de on de.DesignElementID = ev.DesignElementID
  order by de.Accession, a.Accession;
  
end;
  

END AtlasAPI;