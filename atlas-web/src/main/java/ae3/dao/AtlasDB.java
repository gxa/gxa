package ae3.dao;

import ae3.model.AtlasGene;

import java.util.List;
import java.util.ArrayList;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Connection;

import ds.utils.DS_DBconnection;


/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 3, 2009
 * Time: 4:55:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDB {

     public static class Gene{
         public String Id;
         public String Name;

         public String getId(){
             return Id;
         }

         public String getName(){
             return Name;
         }
     }

     public static List<Gene> getGenes(String StartLetter, int FirstRow, int PageSize) throws Exception {

         List<Gene> result = new ArrayList<Gene>();

         Connection conn = DS_DBconnection.instance().getConnection();

         CallableStatement sql = conn.prepareCall("{call ATLAS_PKG.Gene_GetList(?,?,?,?,?,?)}");

         sql.setString(1, StartLetter);
         sql.setInt(2, FirstRow);
         sql.setInt(3, PageSize);
         sql.setString(4, "GeneID");
         sql.setInt(5, 10);
                 //
         sql.registerOutParameter(6, oracle.jdbc.OracleTypes.CURSOR);

         sql.execute();

         ResultSet expRS = (ResultSet) sql.getObject(6);

        while(expRS.next()){
            Gene g = new Gene();

            g.Id    = expRS.getString("GeneID");
            g.Name  = expRS.getString("GeneName");

            result.add(g);
       }

       expRS.close();
       sql.close();
       conn.close();

       return result;
     }
}
