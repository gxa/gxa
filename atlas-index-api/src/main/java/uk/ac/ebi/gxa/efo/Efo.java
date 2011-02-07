/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.efo;

import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URI;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * Interface API to EFO hierarchy
 *
 * @author rpetry
 */
public interface Efo extends InitializingBean {

    public URI getUri();

    public void setUri(URI uri);

    public void setCache(File cache);

    public String getVersion();

    public String getVersionInfo();

    public void setVersion(String version);

    public void setVersionInfo(String versionInfo);

    public Map<String, EfoNode> getEfomap();

    public void load();


    /**
     * Check if term is here
     *
     * @param id term id
     * @return true if yes
     */
    public boolean hasTerm(String id);

    /**
     * Fetch term by id
     *
     * @param id term id
     * @return external term representation if found in ontology, null otherwise
     */
    public EfoTerm getTermById(String id);

    /**
     * Returns collection of IDs of node itself and all its children recursively
     *
     * @param id term id
     * @return collection of IDs, empty if term is not found
     */
    public Collection<String> getTermAndAllChildrenIds(String id);

    /**
     * Returns collection of term's direct children
     *
     * @param id term id
     * @return collection of terms, null if term is not found
     */
    public Collection<EfoTerm> getTermChildren(String id);

    /**
     * Returns collection of all terms (depth=0)
     *
     * @return collection of all terms
     */
    public Collection<EfoTerm> getAllTerms();

    /**
     * Returns collection of all term IDs
     *
     * @return set of all term IDs
     */
    public Set<String> getAllTermIds();

    /**
     * Searches for prefix in ontology
     *
     * @param prefix prefix to search
     * @return set of string IDs
     */
    public Set<String> searchTermPrefix(String prefix);


    /**
     * Searches for text in ontology
     *
     * @param text words to search
     * @return collection of terms
     */
    public Collection<EfoTerm> searchTerm(String text);


    /**
     * Returns list of term parent paths (represented as list string from node ending at root)
     *
     * @param id               term id to search
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return list of lists of Term's
     */
    public List<List<EfoTerm>> getTermParentPaths(String id, boolean stopOnBranchRoot);

    /**
     * Returns set of term's direct parent IDs
     *
     * @param id term id
     * @return set of string IDs
     */
    public Set<String> getTermFirstParents(String id);

    /**
     * Returns set of term's parent IDs
     *
     * @param id               term id
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return set of string IDs
     */
    public Set<String> getTermParents(String id, boolean stopOnBranchRoot);


    /**
     * Creates flat subtree representation ordered in natural print order,
     * each self-contained sub-tree starts from depth=0
     *
     * @param ids marked IDs
     * @return list of Term's
     */
    public List<EfoTerm> getSubTree(Set<String> ids);


    /**
     * Creates flat subtree representation of tree "opened" down to specified node,
     * hence displaying all its parents first and then a tree level, containing specified node
     *
     * @param id term id
     * @return list of Term's
     */
    public List<EfoTerm> getTreeDownTo(String id);

    /**
     * Returns set of root node IDs
     *
     * @return set of root node IDs
     */
    public Set<String> getRootIds();

    /**
     * Returns list of root terms
     *
     * @return list of terms
     */
    public List<EfoTerm> getRoots();

    public void close();

}
