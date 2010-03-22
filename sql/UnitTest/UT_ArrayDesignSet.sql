/*
call UT_ARRAYDESIGNSET();

delete from a2_arraydesign
delete from a2_gene
delete from a2_genepropertyvalue
select count(1) from a2_designelement
commit
*/
create or replace procedure UT_ARRAYDESIGNSET
as
 ACCESSION1 VARCHAR2(32767);
 TYPE1 VARCHAR2(32767);
 NAME1 VARCHAR2(32767);
 PROVIDER1 VARCHAR2(32767);
 DESIGNELEMENTS DESIGNELEMENTTABLE; 
 MAX_DE INTEGER := 20000;
 MAX_PROPERTIES INTEGER := 20;
 ACCESSION varchar2(255);
 EntryName varchar2(255);
 EntryValue varchar2(255);
 Property  varchar2(255);
 PropertyValue  varchar2(255);

BEGIN 
 ACCESSION1 := 'AD-1';
 TYPE1:= 'some type';
 NAME1 := 'some array design';
 PROVIDER1 := 'some provider';
 DESIGNELEMENTS := new DESIGNELEMENTTABLE();

 DESIGNELEMENTS.extend(MAX_DE*MAX_PROPERTIES);

 FOR i IN 1..MAX_DE
 LOOP
    Accession := 'DE-' || i;
    EntryName := 'EMBL';
    EntryValue := 'FAKE-EMBL-' || i;
    
    DESIGNELEMENTS(i*MAX_PROPERTIES) := new DESIGNELEMENT2(Accession
                                                          ,EntryName
                                                          ,EntryValue);
                                           
    FOR j IN 1..(MAX_PROPERTIES - 1 )            
    LOOP
      Property := 'Some Property' || j;
      PropertyValue := 'Some PV ' || j || '(' || i || ')';

      DESIGNELEMENTS(i*MAX_PROPERTIES-j) := new DESIGNELEMENT2(Accession
                                                  ,Property
                                                  ,PropertyValue);    
    END LOOP;                                            

 END LOOP;

 ATLASLDR.A2_ARRAYDESIGNSET ( ACCESSION1
                             , TYPE1
                             , NAME1
                             , PROVIDER1
                             , DESIGNELEMENTS );

 dbms_output.put_line('>>>>>hoper>>>>>invest!!!');

   for r in (select * 
             from a2_DesignElement de
             join a2_arraydesign ad on ad.arraydesignid = de.ArrayDesignID
             where ad.accession = 'AD-1' ) loop
     null;
     --dbms_output.put_line( 'GeneID:' || r.GeneID );
   end loop;
 COMMIT; 
END;
