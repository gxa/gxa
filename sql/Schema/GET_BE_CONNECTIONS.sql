create or replace
FUNCTION GET_BE_CONNECTIONS (
   BEID NUMBER)
RETURN BE_ALL_CONNECTIONS_TABLE
AS
  r BE_ALL_CONNECTIONS_TABLE;

  v_results BE_ALL_CONNECTIONS_TABLE := BE_ALL_CONNECTIONS_TABLE();
   BEGIN

   --collect children
    FOR rc IN
 	  (SELECT  be2be.bioentityidto as connectionid
    	  FROM	  a2_bioentity2bioentity be2be
 	      START   WITH be2be.bioentityidfrom = BEID
   	    CONNECT BY PRIOR  be2be.bioentityidto = be2be.bioentityidfrom)
    LOOP
   	 v_results.EXTEND;
   	 v_results (v_results.COUNT) :=
  	   BE_ALL_CONNECTIONS (beid, rc.connectionid);
     END LOOP;

     --collect parents
     FOR rc IN
 	  (SELECT  be2be.bioentityidfrom as connectionid
    	  FROM	  a2_bioentity2bioentity be2be
 	      START   WITH be2be.bioentityidto = BEID
   	    CONNECT BY PRIOR  be2be.bioentityidfrom =be2be.bioentityidto)
    LOOP
   	 v_results.EXTEND;
   	 v_results (v_results.COUNT) :=
  	   BE_ALL_CONNECTIONS (beid, rc.connectionid);
     END LOOP;


     RETURN v_results;

END GET_BE_CONNECTIONS;