/**
 * QueryServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ae3.ols.webservice.axis;

public class QueryServiceLocator extends org.apache.axis.client.Service implements ae3.ols.webservice.axis.QueryService {

    public QueryServiceLocator() {
    }


    public QueryServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public QueryServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for OntologyQuery
    private java.lang.String OntologyQuery_address = "http://www.ebi.ac.uk/ontology-lookup/services/OntologyQuery";

    public java.lang.String getOntologyQueryAddress() {
        return OntologyQuery_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String OntologyQueryWSDDServiceName = "OntologyQuery";

    public java.lang.String getOntologyQueryWSDDServiceName() {
        return OntologyQueryWSDDServiceName;
    }

    public void setOntologyQueryWSDDServiceName(java.lang.String name) {
        OntologyQueryWSDDServiceName = name;
    }

    public ae3.ols.webservice.axis.Query getOntologyQuery() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(OntologyQuery_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getOntologyQuery(endpoint);
    }

    public ae3.ols.webservice.axis.Query getOntologyQuery(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            ae3.ols.webservice.axis.OntologyQuerySoapBindingStub _stub = new ae3.ols.webservice.axis.OntologyQuerySoapBindingStub(portAddress, this);
            _stub.setPortName(getOntologyQueryWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setOntologyQueryEndpointAddress(java.lang.String address) {
        OntologyQuery_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (ae3.ols.webservice.axis.Query.class.isAssignableFrom(serviceEndpointInterface)) {
                ae3.ols.webservice.axis.OntologyQuerySoapBindingStub _stub = new ae3.ols.webservice.axis.OntologyQuerySoapBindingStub(new java.net.URL(OntologyQuery_address), this);
                _stub.setPortName(getOntologyQueryWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("OntologyQuery".equals(inputPortName)) {
            return getOntologyQuery();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.ebi.ac.uk/ontology-lookup/OntologyQuery", "QueryService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.ebi.ac.uk/ontology-lookup/OntologyQuery", "OntologyQuery"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("OntologyQuery".equals(portName)) {
            setOntologyQueryEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
