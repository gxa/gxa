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

import com.google.common.io.Closeables;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

/**
 * Class representing EFO hierarchy
 *
 * @author pashky
 */
public class EfoImpl implements Efo {
    private static final Logger log = LoggerFactory.getLogger(EfoImpl.class);

    private SortedSet<EfoNode> roots = new TreeSet<EfoNode>(EfoNode.termAlphaComp);
    private Directory indexDirectory;
    private IndexSearcher indexSearcher;
    private IndexReader indexReader;
    private File cache;
    private URI uri;

    private Map<String, EfoNode> efomap;
    private String version;
    private String versionInfo;

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setCache(File cache) {
        this.cache = cache;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        if (cache != null) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(cache));
                String ouri = ois.readUTF();
                if (uri.toString().equals(ouri)) {
                    version = ois.readUTF();
                    versionInfo = ois.readUTF();
                    efomap = (Map<String, EfoNode>) ois.readObject();
                    roots = (SortedSet<EfoNode>) ois.readObject();
                    return;
                }
            } catch (ClassNotFoundException e) {
                log.info("Can't read, give up: " + e.getMessage());
            } catch (FileNotFoundException e) {
                log.info("Can't read, give up: " + e.getMessage());
            } catch (IOException e) {
                log.info("Can't read, give up: " + e.getMessage());
            } finally {
                closeQuietly(ois);
            }
        }
        load();
        if (cache != null) {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(new FileOutputStream(cache, false));
                oos.writeUTF(uri.toString());
                oos.writeUTF(version);
                oos.writeUTF(versionInfo);
                oos.writeObject(efomap);
                oos.writeObject(roots);
            } catch (FileNotFoundException e) {
                log.info("Can't write, give up: " + e.getMessage());
            } catch (IOException e) {
                log.info("Can't write, give up: " + e.getMessage());
            } finally {
                closeQuietly(oos);
            }
        }
    }

    private Map<String, EfoNode> getMap() {
        triggerLoad();
        return efomap;
    }

    public String getVersion() {
        triggerLoad();
        return version;
    }

    public String getVersionInfo() {
        triggerLoad();
        return versionInfo;
    }

    private void triggerLoad() {
        if (efomap == null) {
            load();
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public Map<String, EfoNode> getEfomap() {
        return efomap;
    }

    public void load() {
        efomap = new HashMap<String, EfoNode>();
        new Loader().load(this, uri);

        EfoNode other = new EfoNode("Other", "other", true, Collections.<String>emptyList());
        for (EfoNode n : getMap().values()) {
            if (n.parents.isEmpty())
                (n.branchRoot ? roots : other.children).add(n);
        }

        for (EfoNode n : other.children)
            n.parents.add(other);
        efomap.put(other.id, other);
        roots.add(other);

        rebuildIndex();
    }

    /**
     * Helper factory method to make term classes
     *
     * @param node internal node to make it from
     * @return external term object
     */
    private EfoTerm newTerm(EfoNode node) {
        return new EfoTerm(node, roots.contains(node));
    }

    /**
     * Helper factory method to make term classes
     *
     * @param node  internal node to make it from
     * @param depth required depth
     * @return external term object
     */
    private EfoTerm newTerm(EfoNode node, int depth) {
        return new EfoTerm(node, depth, roots.contains(node));
    }

    /**
     * Check if term is here
     *
     * @param id term id
     * @return true if yes
     */
    public boolean hasTerm(String id) {
        EfoNode node = getMap().get(id);
        return node != null;
    }

    /**
     * Fetch term by id
     *
     * @param id term id
     * @return external term representation if found in ontology, null otherwise
     */
    public EfoTerm getTermById(String id) {
        EfoNode node = getMap().get(id);
        return node == null ? null : newTerm(node);
    }

    private void collectChildren(Collection<String> result, EfoNode node, int includeEfoDescendantGeneration) {
        if (includeEfoDescendantGeneration > 0)
            for (EfoNode child : node.children) {
                result.add(child.id);
                collectChildren(result, child, --includeEfoDescendantGeneration);
            }
    }

    /**
     * Returns collection of IDs of node itself and all its children recursively
     *
     * @param id                             term id
     * @param includeEfoDescendantGeneration Specifies the generation down to which this efo's descendants should be included
     * @return collection of IDs, empty if term is not found
     */
    public Collection<String> getTermAndAllChildrenIds(String id, int includeEfoDescendantGeneration) {
        EfoNode node = getMap().get(id);
        List<String> ids = new ArrayList<String>(node == null ? 0 : node.children.size());
        if (node != null) {
            collectChildren(ids, node, includeEfoDescendantGeneration);
            ids.add(node.id);
        }
        return ids;
    }

    /**
     * Returns collection of term's direct children
     *
     * @param id term id
     * @return collection of terms, null if term is not found
     */
    public Collection<EfoTerm> getTermChildren(String id) {
        EfoNode node = getMap().get(id);
        if (node == null)
            return null;

        List<EfoTerm> result = new ArrayList<EfoTerm>(node.children.size());
        for (EfoNode n : node.children)
            result.add(newTerm(n));

        return result;
    }

    /**
     * Returns collection of all terms (depth=0)
     *
     * @return collection of all terms
     */
    public Collection<EfoTerm> getAllTerms() {
        List<EfoTerm> result = new ArrayList<EfoTerm>(getMap().size());
        for (EfoNode n : getMap().values())
            result.add(newTerm(n));
        return result;
    }

    /**
     * Returns collection of all term IDs
     *
     * @return collection of EFO terms
     */
    public Set<String> getAllTermIds() {
        return new HashSet<String>(getMap().keySet());
    }

    /**
     * Searches for prefix in ontology
     *
     * @param prefix prefix to search
     * @return collection of efoTerms
     */
    public Collection<EfoTerm> searchTermPrefix(String prefix) {
        String lprefix = prefix.toLowerCase();
        Set<EfoNode> set = new HashSet<EfoNode>();
        List<EfoTerm> result = new ArrayList<EfoTerm>();
        for (EfoNode n : getMap().values()) {
            boolean added = false;
            if (n.term.toLowerCase().startsWith(lprefix) || n.id.toLowerCase().startsWith(lprefix)) {
                added = set.add(n);
            } else {
                for (String alt : n.alternativeTerms)
                    if (alt.toLowerCase().startsWith(lprefix)) {
                        added = set.add(n);
                        break;
                    }
            }

            if (added) {
                result.add(newTerm(n));
            }
        }

        return result;
    }

    private void rebuildIndex() {
        CloseableIndexWriter writer = null;
        try {
            indexDirectory = new RAMDirectory();
            writer = new CloseableIndexWriter(indexDirectory, new LowercaseAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
            writer.deleteDocuments(new MatchAllDocsQuery());

            for (EfoNode n : getMap().values()) {
                Document doc = new Document();
                doc.add(new Field("id", n.id, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field("text", n.id, Field.Store.NO, Field.Index.ANALYZED));
                doc.add(new Field("text", n.term, Field.Store.NO, Field.Index.ANALYZED));
                for (String alt : n.alternativeTerms)
                    doc.add(new Field("text", alt, Field.Store.NO, Field.Index.ANALYZED));
                writer.addDocument(doc);
            }

            writer.commit();
        } catch (IOException e) {
            throw logUnexpected("Unable to index documents", e);
        } finally {
            Closeables.closeQuietly(writer);
        }
    }


    /**
     * Searches for text in ontology
     *
     * @param text words to search
     * @return collection of terms
     */
    public Collection<EfoTerm> searchTerm(String text) {
        List<EfoTerm> result = new ArrayList<EfoTerm>();

        boolean tryAgain = false;
        do {
            try {
                if (indexSearcher == null) {
                    rebuildIndex();
                    indexReader = IndexReader.open(indexDirectory, true);
                    indexSearcher = new IndexSearcher(indexReader);
                }

                QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "text", new LowercaseAnalyzer());
                Query query = parser.parse(text);
                TopDocs hits = indexSearcher.search(query, 10000);

                for (ScoreDoc sdoc : hits.scoreDocs) {
                    Document doc = indexSearcher.doc(sdoc.doc);
                    String[] ids = doc.getValues("id");
                    result.add(newTerm(getMap().get(ids[0])));
                }
            } catch (CorruptIndexException e) {
                log.info("Corrupt index, rebuilding", e);
                rebuildIndex();
                tryAgain = true;
            } catch (IOException e) {
                throw logUnexpected("Cannot search", e);
            } catch (ParseException e) {
                log.info("do not do anything if can't parse query", e);
            }
        } while (tryAgain);

        return result;
    }

    private void collectPaths(EfoNode node, Collection<List<EfoTerm>> result, List<EfoTerm> current, boolean stopOnBranchRoot) {
        for (EfoNode p : node.parents) {
            List<EfoTerm> next = new ArrayList<EfoTerm>(current);
            next.add(newTerm(p));
            if (stopOnBranchRoot && p.branchRoot)
                result.add(next);
            else
                collectPaths(p, result, next, stopOnBranchRoot);
        }
        if (node.parents.isEmpty())
            result.add(current);
    }

    /**
     * Returns list of term parent paths (represented as list string from node ending at root)
     *
     * @param id               term id to search
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return list of lists of Term's
     */
    public List<List<EfoTerm>> getTermParentPaths(String id, boolean stopOnBranchRoot) {
        EfoNode node = getMap().get(id);
        if (node == null)
            return null;

        List<List<EfoTerm>> result = new ArrayList<List<EfoTerm>>();
        collectPaths(node, result, new ArrayList<EfoTerm>(), stopOnBranchRoot);
        return result;
    }

    /**
     * Returns set of term's direct parent IDs
     *
     * @param id term id
     * @return set of string IDs
     */
    public Set<String> getTermFirstParents(String id) {
        EfoNode node = getMap().get(id);
        if (node == null)
            return null;
        Set<String> parents = new HashSet<String>();
        for (EfoNode p : node.parents)
            parents.add(p.id);
        return parents;
    }

    /**
     * Returns set of term's parent IDs
     *
     * @param id               term id
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return set of string IDs
     */
    public Set<String> getTermParents(String id, boolean stopOnBranchRoot) {
        EfoNode node = getMap().get(id);
        if (node == null)
            return null;
        Set<String> parents = new HashSet<String>();
        collectParents(node, parents, stopOnBranchRoot);
        return parents;
    }

    private void collectParents(EfoNode node, Set<String> parents, boolean stopOnBranchRoot) {
        for (EfoNode p : node.parents) {
            parents.add(p.id);
            if (!stopOnBranchRoot || !p.branchRoot)
                collectParents(p, parents, stopOnBranchRoot);
        }
    }

    private void collectSubTree(EfoNode currentNode, List<EfoTerm> result, List<EfoTerm> pathres, Set<String> allNodes, Set<String> visited, int depth, boolean printing) {
        if (printing && !allNodes.contains(currentNode.id)) {
            printing = false;
        }

        boolean started = false;
        if (!printing && allNodes.contains(currentNode.id) && !visited.contains(currentNode.id)) {
            printing = true;
            pathres = new ArrayList<EfoTerm>();
            started = true;
        }

        if (printing) {
            EfoTerm efoTerm = newTerm(currentNode, depth);
            pathres.add(efoTerm);
            // The clause below prevents efo terms form showing twice in heatmap header:
            // if collectSubTree was called with printing == true and visited already contained
            // currentNode.id (i.e. that efo term was already included in heatmap header), remove it from result
            // and re-add it - so that children are shown to the right-most in heatmap header. This should make
            // the heatmap somewhat more readable to the user who will scan it from left to right and naturally
            // expect to see parents progress to children.
            if (visited.contains(currentNode.id))
                result.remove(efoTerm);
            else
                visited.add(currentNode.id);

            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, pathres, allNodes, visited, depth + 1, true);
        } else {
            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, null, allNodes, visited, 0, false);
        }

        if (started) {
            result.addAll(pathres);
        }
    }

    /**
     * Creates flat subtree representation ordered in natural print order,
     * each self-contained sub-tree starts from depth=0
     *
     * @param ids marked IDs
     * @return list of Term's
     */
    public List<EfoTerm> getSubTree(Set<String> ids) {
        List<EfoTerm> result = new ArrayList<EfoTerm>();

        Set<String> visited = new HashSet<String>();
        for (EfoNode root : roots) {
            collectSubTree(root, result, null, ids, visited, 0, false);
        }
        return result;
    }

    private void collectTreeDownTo(Iterable<EfoNode> nodes, Stack<EfoNode> path, List<EfoTerm> result, int depth) {
        EfoNode next = path.pop();
        for (EfoNode n : nodes) {
            result.add(newTerm(n, depth));
            if (n.equals(next) && !path.empty())
                collectTreeDownTo(n.children, path, result, depth + 1);
        }
    }

    /**
     * Creates flat subtree representation of tree "opened" down to specified node,
     * hence displaying all its parents first and then a tree level, containing specified node
     *
     * @param id term id
     * @return list of Term's
     */
    public List<EfoTerm> getTreeDownTo(String id) {
        List<EfoTerm> result = new ArrayList<EfoTerm>();

        Stack<EfoNode> path = new Stack<EfoNode>();
        EfoNode node = getMap().get(id);
        while (true) {
            path.push(node);
            if (node.parents.isEmpty())
                break;
            node = node.parents.first();
        }

        collectTreeDownTo(roots, path, result, 0);
        return result;
    }

    /**
     * Returns set of root node IDs
     *
     * @return set of root node IDs
     */
    public Set<String> getRootIds() {
        Set<String> result = new HashSet<String>();
        for (EfoNode n : roots) {
            result.add(n.id);
        }
        return result;
    }

    /**
     * Returns list of root terms
     *
     * @return list of terms
     */
    public List<EfoTerm> getRoots() {
        List<EfoTerm> result = new ArrayList<EfoTerm>(roots.size());
        for (EfoNode n : roots)
            result.add(newTerm(n));

        return result;
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        if (indexSearcher != null)
            try {
                indexSearcher.close();
            } catch (IOException e) {
                throw logUnexpected("Cannot close searcher", e);
            }

        if (indexReader != null)
            try {
                indexReader.close();
            } catch (IOException e) {
                throw logUnexpected("Cannot close reader", e);
            }
    }

    public String getSummary() {
        return "EFO version " + getVersion() + " (" + getVersionInfo() + ") loaded from " + uri.toString();
    }
}
