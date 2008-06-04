/**
 * DataHolder.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ae3.ols.webservice.axis;

public class DataHolder  implements java.io.Serializable {
    private java.lang.Double annotationNumberValue;

    private java.lang.String annotationStringValue;

    private java.lang.String annotationType;

    private java.lang.String termId;

    private java.lang.String termName;

    public DataHolder() {
    }

    public DataHolder(
           java.lang.Double annotationNumberValue,
           java.lang.String annotationStringValue,
           java.lang.String annotationType,
           java.lang.String termId,
           java.lang.String termName) {
           this.annotationNumberValue = annotationNumberValue;
           this.annotationStringValue = annotationStringValue;
           this.annotationType = annotationType;
           this.termId = termId;
           this.termName = termName;
    }


    /**
     * Gets the annotationNumberValue value for this DataHolder.
     * 
     * @return annotationNumberValue
     */
    public java.lang.Double getAnnotationNumberValue() {
        return annotationNumberValue;
    }


    /**
     * Sets the annotationNumberValue value for this DataHolder.
     * 
     * @param annotationNumberValue
     */
    public void setAnnotationNumberValue(java.lang.Double annotationNumberValue) {
        this.annotationNumberValue = annotationNumberValue;
    }


    /**
     * Gets the annotationStringValue value for this DataHolder.
     * 
     * @return annotationStringValue
     */
    public java.lang.String getAnnotationStringValue() {
        return annotationStringValue;
    }


    /**
     * Sets the annotationStringValue value for this DataHolder.
     * 
     * @param annotationStringValue
     */
    public void setAnnotationStringValue(java.lang.String annotationStringValue) {
        this.annotationStringValue = annotationStringValue;
    }


    /**
     * Gets the annotationType value for this DataHolder.
     * 
     * @return annotationType
     */
    public java.lang.String getAnnotationType() {
        return annotationType;
    }


    /**
     * Sets the annotationType value for this DataHolder.
     * 
     * @param annotationType
     */
    public void setAnnotationType(java.lang.String annotationType) {
        this.annotationType = annotationType;
    }


    /**
     * Gets the termId value for this DataHolder.
     * 
     * @return termId
     */
    public java.lang.String getTermId() {
        return termId;
    }


    /**
     * Sets the termId value for this DataHolder.
     * 
     * @param termId
     */
    public void setTermId(java.lang.String termId) {
        this.termId = termId;
    }


    /**
     * Gets the termName value for this DataHolder.
     * 
     * @return termName
     */
    public java.lang.String getTermName() {
        return termName;
    }


    /**
     * Sets the termName value for this DataHolder.
     * 
     * @param termName
     */
    public void setTermName(java.lang.String termName) {
        this.termName = termName;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DataHolder)) return false;
        DataHolder other = (DataHolder) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.annotationNumberValue==null && other.getAnnotationNumberValue()==null) || 
             (this.annotationNumberValue!=null &&
              this.annotationNumberValue.equals(other.getAnnotationNumberValue()))) &&
            ((this.annotationStringValue==null && other.getAnnotationStringValue()==null) || 
             (this.annotationStringValue!=null &&
              this.annotationStringValue.equals(other.getAnnotationStringValue()))) &&
            ((this.annotationType==null && other.getAnnotationType()==null) || 
             (this.annotationType!=null &&
              this.annotationType.equals(other.getAnnotationType()))) &&
            ((this.termId==null && other.getTermId()==null) || 
             (this.termId!=null &&
              this.termId.equals(other.getTermId()))) &&
            ((this.termName==null && other.getTermName()==null) || 
             (this.termName!=null &&
              this.termName.equals(other.getTermName())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getAnnotationNumberValue() != null) {
            _hashCode += getAnnotationNumberValue().hashCode();
        }
        if (getAnnotationStringValue() != null) {
            _hashCode += getAnnotationStringValue().hashCode();
        }
        if (getAnnotationType() != null) {
            _hashCode += getAnnotationType().hashCode();
        }
        if (getTermId() != null) {
            _hashCode += getTermId().hashCode();
        }
        if (getTermName() != null) {
            _hashCode += getTermName().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DataHolder.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "DataHolder"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("annotationNumberValue");
        elemField.setXmlName(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "annotationNumberValue"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "double"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("annotationStringValue");
        elemField.setXmlName(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "annotationStringValue"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("annotationType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "annotationType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("termId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "termId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("termName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://model.web.ook.ebi.ac.uk", "termName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
