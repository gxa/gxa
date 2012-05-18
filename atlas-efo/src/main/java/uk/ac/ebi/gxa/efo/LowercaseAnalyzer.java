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

import org.apache.lucene.analysis.*;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

import java.io.Reader;
import java.io.IOException;

/**
 * @author pashky
*/
class LowercaseAnalyzer extends Analyzer {
   /* private static class LowercaseTokenizer extends CharTokenizer
    {
        private LowercaseTokenizer(Version matchVersion, Reader input) {
            super(matchVersion, input);
        }

        private LowercaseTokenizer(Version matchVersion, AttributeSource source, Reader input) {
            super(matchVersion, source, input);
        }

        private LowercaseTokenizer(Version matchVersion, AttributeFactory factory, Reader input) {
            super(matchVersion, factory, input);
        }

        @Override
        protected int normalize(int c)
        {
            return Character.toLowerCase(c);
        }

        @Override
        protected boolean isTokenChar(int c) {
            return
        }
    }*/

    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        return new LowerCaseTokenizer(Version.LUCENE_31, reader);
    }

    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException
    {
        Tokenizer tokenizer = (Tokenizer)getPreviousTokenStream();
        if (tokenizer == null) {
            tokenizer = new LowerCaseTokenizer(Version.LUCENE_31, reader);
            setPreviousTokenStream(tokenizer);
        } else
            tokenizer.reset(reader);
        return tokenizer;
    }
}
