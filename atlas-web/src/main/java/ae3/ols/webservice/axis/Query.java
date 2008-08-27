/**
 * Query.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ae3.ols.webservice.axis;

public interface Query extends java.rmi.Remote {
    public java.lang.String getVersion() throws java.rmi.RemoteException;
    public java.lang.String getTermById(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getTermMetadata(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getTermXrefs(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getOntologyNames() throws java.rmi.RemoteException;
    public java.lang.String getOntologyLoadDate(java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getAllTermsFromOntology(java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getRootTerms(java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getTermsByName(java.lang.String partialName, java.lang.String ontologyName, boolean reverseKeyOrder) throws java.rmi.RemoteException;
    public java.util.HashMap getTermsByExactName(java.lang.String exactName, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getPrefixedTermsByName(java.lang.String partialName, boolean reverseKeyOrder) throws java.rmi.RemoteException;
    public java.util.HashMap getTermParents(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getTermChildren(java.lang.String termId, java.lang.String ontologyName, int distance, int[] relationTypes) throws java.rmi.RemoteException;
    public java.util.HashMap getTermRelations(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public java.util.HashMap getChildrenFromRoot(java.lang.String rootTermId, java.lang.String ontologyName, java.util.Vector childrenIds) throws java.rmi.RemoteException;
    public boolean isObsolete(java.lang.String termId, java.lang.String ontologyName) throws java.rmi.RemoteException;
    public ae3.ols.webservice.axis.DataHolder[] getTermsByAnnotationData(java.lang.String ontologyName, java.lang.String annotationType, java.lang.String strValue, double fromDblValue, double toDblValue) throws java.rmi.RemoteException;
}
