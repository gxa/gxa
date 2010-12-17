package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node;

import java.util.HashMap;
import java.util.Map;

/**
 * Models "Feature" nodes in the ADF graph, according to the MAGE-TAB
 * specification.  Features comprise block column, block row, column and row
 * integer values, designating the position of this feature on the microarray
 * chip.  The can also have comments attached.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class FeatureNode extends AbstractADFNode {
  public int blockColumn;
  public int blockRow;
  public int column;
  public int row;
  public Map<String, String> comments = new HashMap<String, String>();

  public boolean equals(Object obj) {
    if (obj instanceof FeatureNode) {
      FeatureNode that = (FeatureNode) obj;
      boolean classesEqual = that.getClass().equals(this.getClass());
      boolean bcEqual = that.blockColumn == this.blockColumn;
      boolean brEqual = that.blockRow == this.blockRow;
      boolean colEqual = that.column == this.column;
      boolean rowEqual = that.row == this.row;

      return classesEqual && bcEqual && brEqual && colEqual && rowEqual;
    }
    else {
      return false;
    }

  }

  public int hashCode() {
    int classesHash = this.getClass().hashCode();

    // multiply blocks by units of 100, this should be plenty for all array designs
    int colHash = (1000000 * blockColumn) + (10000) * column;
    int rowHash = (100 * blockRow) + row;

    return 27 * (classesHash + colHash + rowHash);
  }

  public String[] headers() {
    return new String[0];
  }

  public String[] values() {
    return new String[0];
  }
}
