package uk.ac.ebi.microarray.atlas.loader;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 27, 2009
 * Time: 10:31:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class Assay {
        public String Accession;
        public String ExperimentAccession;
        public String ArrayDesignAcession;
        public List<Property> Properties;
        public List<ExpressionValue> ExpressionValues;

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

        public ExpressionValue AddExpressionValue(String DesignElementAccession, float Value){
              ExpressionValue result = new ExpressionValue();
              result.DesignElementAccession = DesignElementAccession;
              result.Value = Value;

              if(null==ExpressionValues)
                 ExpressionValues = new ArrayList<ExpressionValue>();

              ExpressionValues.add(result);

              return result;
        }
}
