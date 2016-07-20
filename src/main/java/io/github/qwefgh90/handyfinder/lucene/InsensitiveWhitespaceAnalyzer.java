package io.github.qwefgh90.handyfinder.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public final class InsensitiveWhitespaceAnalyzer extends Analyzer {

	public InsensitiveWhitespaceAnalyzer() {
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer tokenizer = new WhitespaceTokenizer();
		TokenStream filter = new LowerCaseFilter(tokenizer);
		return new TokenStreamComponents(tokenizer, filter);
	}

}
