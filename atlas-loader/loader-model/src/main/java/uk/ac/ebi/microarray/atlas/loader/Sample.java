package uk.ac.ebi.microarray.atlas.loader;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 27, 2009
 * Time: 10:32:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class Sample{
     public String Accession;
     public List<String> AssayAccessions;
     public List<Property> Properties;
     public String Species;
     public String Channel;

     public void AddAssayAccession(String AssayAccession){
         if(null==AssayAccessions)
                  AssayAccessions = new ArrayList<String>();

         AssayAccessions.add(AssayAccession);
     }

     //shortcut to add property
     public Property AddProperty(String Accession, String Name, String Value, Boolean IsFactorValue){
           Property result = new Property();
           result.Accession = Accession;
           result.Name = Name;
           result.Value = Value;
           result.IsFactorValue  = IsFactorValue;

           if(null==Properties)
             Properties = new ArrayList<Property>();

           Properties.add(result);

           return result;
     }
 }
