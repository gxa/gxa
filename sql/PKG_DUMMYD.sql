CREATE OR REPLACE PACKAGE DUMMYD AS

  /* TODO enter package declarations (types, exceptions, methods etc) here */
  procedure ArrayDesignCreate(
      ArrayDesigns Integer := 10
    , DesignElements Integer := 10 --per array design
  );
  procedure PropertyCreate(
       Properties integer :=10
      ,PropertyValues integer :=10 --values per property
  );
  procedure ExperimentCreate(
        Experiments integer :=1
       ,Assays integer := 10 --assays per experiment
       ,AssayProperties integer :=10 --distinct properties per assay
       ,Samples integer :=2 --samples PER ASSAY
       ,SampleProperties integer :=10  --distinct properties per sample
       ,ExpressionValues integer :=10
  );
  
  procedure GenePropertyCreate(
       Properties integer :=10
  );
  procedure GeneCreate(
    Genes integer := 1
    ,GeneProperties integer := 10
  );

  procedure AnalyticsCreate(
    ExperimentID integer
    ,Analytics integer := 1000
  );
  
  function GetRandomGeneID return integer;
  function GetRandomArrayDesignID return integer;
  function GetRandomPropertyValueID return integer;
  function GetRandomDesignElementID return integer;
  function GetRandomGenePropertyID return integer; 
  function GetRandomSpecID return integer;

END DUMMYD;

/


CREATE OR REPLACE PACKAGE BODY DUMMYD AS

    CURSOR Gene_Cursor IS Select GeneID from a2_gene; 
    CURSOR ArrayDesign_Cursor IS Select ArrayDesignID from a2_ArrayDesign;
    CURSOR PropertyValue_Cursor IS Select PropertyValueID from a2_PropertyValue;
    CURSOR DesignElement_Cursor IS Select DesignElementID from a2_DesignElement;
    CURSOR Spec_Cursor IS Select SpecID from a2_Spec;
    CURSOR GeneProperty_Cursor IS Select GenePropertyID from a2_GeneProperty;
  
/*******************************************************************************/
/*******************************************************************************/
  function GetRandomGeneID return integer
  AS
    GeneID integer;
  begin
    
    if(not Gene_Cursor%ISOPEN) then
      OPEN Gene_Cursor;
    end if;
    
    fetch Gene_Cursor into GeneID; 
    
    if((Gene_Cursor%NOTFOUND) and (Gene_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'Gene not found');
    end if;
    
    if(Gene_Cursor%NOTFOUND) then
      CLOSE Gene_Cursor;
      OPEN Gene_Cursor;
    end if;
    
    if((GeneID is null) or (GeneID = 0)) then
      fetch Gene_Cursor into GeneID; 
    end if;
        
    return GeneID;
  end;

/*******************************************************************************/
/*******************************************************************************/
  function GetRandomArrayDesignID return integer
  AS
    ArrayDesignID integer;
  begin
    
    if(not ArrayDesign_Cursor%ISOPEN) then
      OPEN ArrayDesign_Cursor;
    end if;
    
    fetch ArrayDesign_Cursor into ArrayDesignID; 
    
    if((ArrayDesign_Cursor%NOTFOUND) and (ArrayDesign_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'Array Design not found');
    end if;
    
    if(ArrayDesign_Cursor%NOTFOUND) then
      CLOSE ArrayDesign_Cursor;
      OPEN ArrayDesign_Cursor;
    end if;
    
    if((ArrayDesignID is null) or (ArrayDesignID = 0)) then
      fetch ArrayDesign_Cursor into ArrayDesignID; 
    end if;
        
    return ArrayDesignID;
  end;

/*******************************************************************************/
/*******************************************************************************/
  function GetRandomPropertyValueID return integer
  AS
    PropertyValueID integer;
  begin
    
    if(not PropertyValue_Cursor%ISOPEN) then
      OPEN PropertyValue_Cursor;
    end if;
    
    fetch PropertyValue_Cursor into PropertyValueID; 
    
    if((PropertyValue_Cursor%NOTFOUND) and (PropertyValue_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'PropertyValue not found');
    end if;
    
    if(PropertyValue_Cursor%NOTFOUND) then
      CLOSE PropertyValue_Cursor;
      OPEN  PropertyValue_Cursor;
    end if;
    
    if((PropertyValueID is null) or (PropertyValueID = 0)) then
      fetch PropertyValue_Cursor into PropertyValueID; 
    end if;
        
    return PropertyValueID;
  end;

/*******************************************************************************/
/*******************************************************************************/
  function GetRandomDesignElementID return integer
  AS
    DesignElementID integer;
  begin
    
    if(not DesignElement_Cursor%ISOPEN) then
      OPEN DesignElement_Cursor;
    end if;
    
    fetch DesignElement_Cursor into DesignElementID; 
    
    if((DesignElement_Cursor%NOTFOUND) and (DesignElement_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'DesignElement not found');
    end if;
    
    if(DesignElement_Cursor%NOTFOUND) then
      CLOSE DesignElement_Cursor;
      OPEN  DesignElement_Cursor;
    end if;
    
    if((DesignElementID is null) or (DesignElementID = 0)) then
      fetch DesignElement_Cursor into DesignElementID; 
    end if;
        
    return DesignElementID;
  end;

/*******************************************************************************/
/*******************************************************************************/
  function GetRandomSpecID return integer
  AS
    SpecID integer;
  begin
    
    if(not Spec_Cursor%ISOPEN) then
      OPEN Spec_Cursor;
    end if;
    
    fetch Spec_Cursor into SpecID; 
    
    if((Spec_Cursor%NOTFOUND) and (Spec_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'Spec not found');
    end if;
    
    if(Spec_Cursor%NOTFOUND) then
      CLOSE Spec_Cursor;
      OPEN  Spec_Cursor;
    end if;
    
    if((SpecID is null) or (SpecID = 0)) then
      fetch Spec_Cursor into SpecID; 
    end if;
        
    return SpecID;
  end;

/*******************************************************************************/
/*******************************************************************************/
  function GetRandomGenePropertyID return integer
  AS
    GenePropertyID integer;
  begin
    
    if(not GeneProperty_Cursor%ISOPEN) then
      OPEN GeneProperty_Cursor;
    end if;
    
    fetch GeneProperty_Cursor into GenePropertyID; 
    
    if((GeneProperty_Cursor%NOTFOUND) and (GeneProperty_Cursor%ROWCOUNT = 0)) then
      RAISE_APPLICATION_ERROR(-20006, 'Spec not found');
    end if;
    
    if(GeneProperty_Cursor%NOTFOUND) then
      CLOSE GeneProperty_Cursor;
      OPEN  GeneProperty_Cursor;
    end if;
    
    if((GenePropertyID is null) or (GenePropertyID = 0)) then
      fetch GeneProperty_Cursor into GenePropertyID; 
    end if;
        
    return GenePropertyID;
  end;


/*******************************************************************************/
/*******************************************************************************/

  procedure ArrayDesignCreate(ArrayDesigns Integer, DesignElements Integer) AS
  created integer := 0;
  ArrayDesignID integer := 0;
  DesignElementCreated integer := 0;
  DesignElementID integer := 0;
  
  BEGIN
    WHILE(created < ArrayDesigns) 
    LOOP
      Select A2_ArrayDesign_Seq.nextval into ArrayDesignID from dual;    
      
      Insert into a2_ArrayDesign(  ARRAYDESIGNID
                              ,ACCESSION
                              ,TYPE
                              ,NAME
                              ,PROVIDER)
      Select ArrayDesignID
            ,'ArrayDesign' || ArrayDesignID
            ,'some type'
            ,'some name'
            ,'some provider'
      from dual;      
       
      DesignElementCreated := 0;
      
    While(DesignElementCreated < DesignElements)
    Loop
    
        Select a2_DesignElement_Seq.nextval INTO DesignElementID from dual;
      
        Insert into a2_DesignElement(
              DESIGNELEMENTID
              ,ARRAYDESIGNID
              ,GENEID
              ,ACCESSION
              ,NAME
              ,TYPE
              ,ISCONTROL)
        Select  DesignElementID
            , ArrayDesignid 
            , GetRandomGeneID()
            , 'DesignElement' || DesignElementID 
            , 'DesignElement' || DesignElementID 
            , 'some design element'
            , 0 --is control ??
        from dual;          

        DesignElementCreated := DesignElementCreated + 1;    
    End Loop;

    created := created +1 ; 
    
    END LOOP;
    
    Commit work;
  END ArrayDesignCreate;
  
/*******************************************************************************/
/*******************************************************************************/

  procedure PropertyCreate(
       Properties integer :=10
      ,PropertyValues integer :=10 --values per property
  ) AS
    PropertyCreated integer := 0;
    PropertyValueCreated integer := 0;
    PropertyID integer := 0;
    PropertyValueID integer := 0;
  BEGIN
    WHILE(PropertyCreated < Properties) 
    LOOP
      Select A2_Property_Seq.nextval into PropertyID from dual;   
      
      Insert into a2_Property(PropertyID
                              ,Name)
      Select PropertyID
            ,'Property' || PropertyID
      from dual;      
       
      PropertyValueCreated := 0;
   
      WHILE(PropertyValueCreated < PropertyValues)
      LOOP
      
        Select A2_PropertyValue_Seq.nextval into PropertyValueID from dual;
    
        Insert into a2_PropertyValue(
               PropertyValueID
              ,PropertyID
              ,Name)
        Select 
              PropertyValueID 
            , PropertyID 
            , 'value' || PropertyValueID 
        from dual;          

        PropertyValueCreated := PropertyValueCreated + 1;    
      END LOOP;

    PropertyCreated := PropertyCreated +1 ; 
    END LOOP;
    
    Commit work;
  END PropertyCreate;

/*******************************************************************************/
/*******************************************************************************/

  procedure ExperimentCreate(
        Experiments integer :=1
       ,Assays integer := 10 --assays per experiment
       ,AssayProperties integer :=10 --distinct properties per assay
       ,Samples integer :=2 --samples PER ASSAY
       ,SampleProperties integer :=10  --distinct properties per sample
       ,ExpressionValues integer :=10
  ) AS
    ExperimentsCreated integer := 0;
    AssaysCreated integer := 0;
    SamplesCreated integer := 0;
    SamplePropertyCreated integer := 0;
    AssayPropertyCreated integer := 0;
    ExpressionValuesCreated integer := 0;
    
    ExperimentID INTEGER := 0;
    AssayID integer := 0;
    SampleID integer := 0;

    CURSOR ArrayDesigns IS Select ArrayDesignID from A2_ArrayDesign;    
  BEGIN
    WHILE(ExperimentsCreated < ExperimentCreate.Experiments) 
    LOOP
      Select A2_Experiment_Seq.nextval into ExperimentID from dual;   
      
      Insert into a2_Experiment( ExperimentID
                                ,Accession
                                ,Description
                                ,Performer 
                                ,Lab
                                ,LoadDate)
      Select ExperimentID
            , 'Experiment' || ExperimentID 
            , 'Experiment' || ExperimentID 
            , 'Experiment' || ExperimentID 
            , 'Scientist Joe Doe'  
            , sysdate
      from dual;      
       
      AssaysCreated := 0;
   
      WHILE(AssaysCreated < ExperimentCreate.Assays)
      LOOP
      
        Select A2_Assay_Seq.nextval into AssayID from dual;
    
        Insert into a2_Assay(  ASSAYID
                              ,ACCESSION
                              ,EXPERIMENTID
                              ,ARRAYDESIGNID )
        Select AssayID
            , 'Assay' || AssayID 
            , ExperimentID 
            , GetRandomArrayDesignID() 
        from dual;         
        
        WHILE(SamplesCreated < ExperimentCreate.Samples)
        LOOP
          Select A2_Sample_Seq.nextval into SampleID from dual;
          
          Insert into a2_Sample(SAMPLEID
                           ,ACCESSION
                           ,SPECIES
                           ,CHANNEL)
          Select SampleID 
                ,'Sample' || SampleID
                ,'enee menee monee'
                ,1
          from dual;   
          
          Insert into a2_AssaySample(AssayID
                              ,SampleID)
          Select AssayID
                ,SampleID
          from dual;
          
          SamplePropertyCreated := 0;
          
          WHILE(SamplePropertyCreated < ExperimentCreate.SampleProperties)
          LOOP
            Insert into a2_SamplePropertyValue(SampleID, PropertyValueID, IsFactorValue)
            select SampleID, GetRandomPropertyValueID(), 0
            from dual;
            
            SamplePropertyCreated := SamplePropertyCreated + 1;
          END LOOP;
          
          SamplesCreated := SamplesCreated + 1;
        END LOOP;

          AssayPropertyCreated := 0;
          
          WHILE(AssayPropertyCreated < ExperimentCreate.AssayProperties)
          LOOP
            Insert into a2_AssayPropertyValue(AssayID, PropertyValueID, IsFactorValue)
            select AssayID, GetRandomPropertyValueID(), 1
            from dual;
            
            AssayPropertyCreated := AssayPropertyCreated + 1;
          END LOOP;
          
          ExpressionValuesCreated := 0;

        WHILE(ExpressionValuesCreated < ExperimentCreate.ExpressionValues)
          LOOP
            Insert into a2_ExpressionValue(ExpressionValueID, DesignelementID, AssayID, Value)
            select a2_ExpressionValue_Seq.nextval, GetRandomDesignElementID(), AssayID, 0
            from dual;
            
            ExpressionValuesCreated := ExpressionValuesCreated + 1;
          END LOOP;  
          
           AssaysCreated := AssaysCreated + 1;   
      END LOOP;

    ExperimentsCreated := ExperimentsCreated +1 ; 
    END LOOP;
    
    Commit work;

  END ExperimentCreate;

/*******************************************************************************/
/*******************************************************************************/
  procedure GenePropertyCreate(
       Properties integer :=10
  ) AS
    PropertyCreated integer := 0;
    GenePropertyID integer := 0;
  BEGIN
    WHILE(PropertyCreated < Properties) 
    LOOP
      Select A2_GeneProperty_Seq.nextval into GenePropertyID from dual;   
      
      Insert into a2_GeneProperty(GenePropertyID
                              ,Name)
      Select GenePropertyID
            ,'GeneProperty' || GenePropertyID
      from dual;      
       
      PropertyCreated := PropertyCreated +1 ; 
    END LOOP;
    
    Commit work;
  END GenePropertyCreate;

/*******************************************************************************/
/*******************************************************************************/

  procedure GeneCreate(
    Genes integer := 1
    ,GeneProperties integer := 10
  ) AS
    GeneID integer := 0;
    GeneCreated integer := 0;
    GenePropertiesCreated integer := 0;
    GenePropertyValueID integer := 0;
  BEGIN
    WHILE(GeneCreated < GeneCreate.Genes) 
    LOOP
      Select A2_Gene_Seq.nextval into GeneID from dual;   
      
      Insert into a2_Gene(Specid
                          ,Identifier
                          ,name)
      Select  GetRandomSpecID()
            ,'Gene' || GeneID
            ,'Gene' || GeneID
      from dual;      
       
      GenePropertiesCreated := 0;
   
      WHILE(GenePropertiesCreated < GeneCreate.GeneProperties)
      LOOP
      
        Select A2_GenePropertyValue_Seq.nextval into GenePropertyValueID from dual;
    
        Insert into a2_GenePropertyValue(
               GenePropertyValueID
              ,GenePropertyID
              ,GeneID
              ,Value)
        Select
              GenePropertyValueID
            , GetRandomGenePropertyID() 
            , GeneID 
            , 'gene=' || GeneID  || ' propertyvalueID=' || GenePropertyValueID  
        from dual;          

        GenePropertiesCreated :=GenePropertiesCreated + 1;    
      END LOOP;

    GeneCreated := GeneCreated +1 ; 
    END LOOP;
    
    Commit work;

  END GeneCreate;

/*******************************************************************************/
/*******************************************************************************/

  procedure AnalyticsCreate(
    ExperimentID integer
    ,Analytics integer := 1000
  ) AS
    ExpressionID integer :=0;
    AnalyticsCreated integer :=0;
  BEGIN
  
    WHILE(AnalyticsCreated < analyticsCreate.Analytics) 
    LOOP
      Select A2_ExpressionAnalytics_Seq.nextval into ExpressionID from dual;   
      
      Insert into a2_ExpressionAnalytics( EXPRESSIONID
                                        ,EXPERIMENTID
                                        ,PROPERTYVALUEID
                                        ,TSTAT
                                        ,PVALADJ
                                        ,FPVAL
                                        ,FPVALADJ
                                        ,DESIGNELEMENTID)
      Select  ExpressionID
            , experimentid
            , GetRandomPropertyValueID()
            , null
            , null
            , null
            , null
            , GetRandomDesignElementID()
      from dual;      
       

    AnalyticsCreated := AnalyticsCreated +1 ; 
    END LOOP;
    
    Commit work;

  END AnalyticsCreate;

END DUMMYD;

/
