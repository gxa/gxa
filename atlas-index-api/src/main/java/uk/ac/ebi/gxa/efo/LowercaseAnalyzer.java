package uk.ac.ebi.gxa.efo;

import org.apache.lucene.analysis.*;

import java.io.Reader;
import java.io.IOException;

/**
 * @author pashky
*/
class LowercaseAnalyzer extends Analyzer {
    private static class LowercaseTokenizer extends WhitespaceTokenizer
    {
        public LowercaseTokenizer(Reader in)
        {
            super(in);
        }

        protected char normalize(char c)
        {
            return Character.toLowerCase(c);
        }
    }

    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        return new LowercaseTokenizer(reader);
    }

    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException
    {
        Tokenizer tokenizer = (Tokenizer)getPreviousTokenStream();
        if (tokenizer == null) {
            tokenizer = new LowercaseTokenizer(reader);
            setPreviousTokenStream(tokenizer);
        } else
            tokenizer.reset(reader);
        return tokenizer;
    }
}
