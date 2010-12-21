package uk.ac.ebi.gxa.efo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.Closeable;
import java.io.IOException;

public class CloseableIndexWriter extends IndexWriter implements Closeable {
    public CloseableIndexWriter(Directory d, Analyzer a, boolean create, MaxFieldLength mfl) throws CorruptIndexException, LockObtainFailedException, IOException {
        super(d, a, create, mfl);
    }
}
