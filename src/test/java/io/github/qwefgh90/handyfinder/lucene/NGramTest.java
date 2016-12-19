package io.github.qwefgh90.handyfinder.lucene;


import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestRuleLimitSysouts.Limit;

import com.google.common.io.Files;


/**
 * A very simple demo used in the API documentation (src/java/overview.html).
 *
 * Please try to keep src/java/overview.html up-to-date when making changes
 * to this class.
 */
@Limit(bytes=1000000)
public class NGramTest extends LuceneTestCase {

	public void testDemo() throws IOException, InvalidTokenOffsetsException, QueryNodeException {

		final Map<String, String> map = new HashMap<>();
		map.put("minGramSize", "1");
		map.put("maxGramSize", "10");
		final Analyzer ngramAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(NGramTokenizerFactory.class, map)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.build();
		
//		final Map<String, String> pathAttrMap = new HashMap<>();
//		pathAttrMap.put("delimiter", File.separator);
		final Analyzer pathAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(KeywordTokenizerFactory.class)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.build();
		WhitespaceAnalyzer a;
		final Map<String, Analyzer> perFieldAnalyzer = new TreeMap<>();
		perFieldAnalyzer.put("content", ngramAnalyzer);
		perFieldAnalyzer.put("hidingContentForTest", ngramAnalyzer);
		perFieldAnalyzer.put("length", ngramAnalyzer);
		perFieldAnalyzer.put("path", pathAnalyzer);
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(ngramAnalyzer, perFieldAnalyzer);
		
		final TokenStream ts = analyzer.tokenStream("path", new StringReader("c:\\handyfinder\\example"));
		final OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
		//reference : http://lucene.apache.org/core/6_3_0/core/org/apache/lucene/analysis/package-summary.html
		try {
			ts.reset(); // Resets this stream to the beginning. (Required)
			while (ts.incrementToken()) {
				// Use AttributeSource.reflectAsString(boolean)
				// for token stream debugging.
				System.out.println("token: " + ts.reflectAsString(true));

				System.out.println("token start offset: " + offsetAtt.startOffset());
				System.out.println("  token end offset: " + offsetAtt.endOffset());
			}
			ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
		} finally {
			ts.close(); // Release resources associated with this stream.
		}
		
 

		// Store the index in memory:
		//Directory directory = newDirectory();
		// To store an index on disk, use this instead:
		;
		Directory directory = FSDirectory.open(Files.createTempDir().toPath());
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		IndexWriter iwriter = new IndexWriter(directory, iwc);
		iwriter.deleteAll();
		iwriter.commit();
		Document doc = new Document();
		final String text1 = " This is the text to be indexed. 한글 입니다. 매우긴문장이니깐잘검색되라 textlongtermlongterm";
		final FieldType ft1 = new FieldType();
		ft1.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		ft1.setStoreTermVectors(true);
		ft1.setStoreTermVectorOffsets(true);
		ft1.setStored(true);
		doc.add(newField("content", text1, ft1));
		doc.add(newTextField("hidingContentForTest", text1, Field.Store.YES));
		doc.add(newTextField("length", String.valueOf(text1.length()), Field.Store.YES));
		doc.add(newStringField("path", "c:\\handyfinder\\example",  Field.Store.YES));
		
		iwriter.addDocument(doc);
		
		doc = new Document();
		final String text2 = "This is the be 한글 입니다.";
		final FieldType ft2 = new FieldType();
		ft2.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		ft2.setStoreTermVectors(true);
		ft2.setStoreTermVectorOffsets(true);
		ft2.setStored(true);
		doc.add(newField("content", text2 , ft2));
		doc.add(newTextField("hidingContentForTest", text2, Field.Store.YES));
		doc.add(newTextField("length", String.valueOf(text2.length()), Field.Store.YES));
		doc.add(newStringField("path", "c:\\lucene\\example",  Field.Store.YES));
		
		iwriter.addDocument(doc);
		iwriter.close();

		// Now search the index:
		IndexReader ireader = DirectoryReader.open(directory); // read-only=true
		IndexSearcher isearcher = newSearcher(ireader);
		
		
		TopDocs docs = isearcher.search(new TermQuery(new Term("path", "c:\\lucene\\example")), 1);
		assertThat(docs.scoreDocs.length, is(1));
		
		BooleanQuery.Builder b = new BooleanQuery.Builder();
		Query q1 = new TermQuery(new Term("content", "indexed"));//parser.parse("is the", "content");
		b.add(q1, Occur.SHOULD);
		
		Query q2 = new WildcardQuery(new Term("path", "*"+QueryParser.escape(":\\lucene")+"*"));
		//Query q2 = new TermQuery(new Term("path", "c:\\lucene"));// parser.parse("This", "content");// 
		b.add(q2, Occur.SHOULD);
		
		TopDocs hits = isearcher.search(b.build(),10);
		//assertEquals(1, hits.totalHits);
		// Iterate through the results:
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			Document hitDoc = isearcher.doc(hits.scoreDocs[i].doc);
			System.out.println("score : " + hits.scoreDocs[i].score + ", length : " + hitDoc.get("length"));
			
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(b.build()));
			
			StringBuffer sb = new StringBuffer();
			
			Document tempDocument = new Document();
			FieldType type = new FieldType();
			type.setStored(true);
			final Field field = new Field("content", hitDoc.get("hidingContentForTest"), type);
			final Field field2 = new Field("path", hitDoc.get("path"), type);
			tempDocument.add(field);
			tempDocument.add(field2);

			//first, lucene find offset of sentence and highlight sentence from file
			try (@SuppressWarnings("deprecation")
			TokenStream tokenStream = TokenSources
					.getAnyTokenStream(ireader, hits.scoreDocs[i].doc, "content", tempDocument, analyzer)) {
				TextFragment[] frag = highlighter.getBestTextFragments(
						tokenStream, hitDoc.get("hidingContentForTest"), false, 10);// highlighter.getBestFragments(tokenStream,
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						sb.append(frag[j].toString());
					}
				}
			}
			
			
			//first, lucene find offset of sentence and highlight sentence from file
			try (@SuppressWarnings("deprecation")
			TokenStream tokenStream = TokenSources
					.getAnyTokenStream(ireader, hits.scoreDocs[i].doc, "path", tempDocument, analyzer)) {
				TextFragment[] frag = highlighter.getBestTextFragments(
						tokenStream, hitDoc.get("path"), false, 10);// highlighter.getBestFragments(tokenStream,
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						sb.append(frag[j].toString());
					}
				}
			}
			System.out.println("highlight : " + sb.toString());
		}
		
		ireader.close();
		directory.close();
		analyzer.close();
	}
}