package com.qwefgh90.io.handyfinder.springweb.service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.qwefgh90.io.handfinder.springweb.model.Directory;
import com.qwefgh90.io.jsearch.JSearch;
import com.qwefgh90.io.jsearch.JSearch.ParseException;

public class LuceneHandler {

	private static Log log = LogFactory.getLog(LuceneHandler.class);

	// writer
	private org.apache.lucene.store.Directory dir;
	private Analyzer analyzer;
	private IndexWriterConfig iwc;
	private IndexWriter writer;

	// reader / searcher
	private IndexReader indexReader;
	private IndexSearcher searcher;

	private static ConcurrentHashMap<String, LuceneHandler> map = new ConcurrentHashMap<>();

	/**
	 * static factory method
	 * 
	 * @param indexWriterPath
	 *            : path where index stored
	 * @return
	 */
	public static LuceneHandler getInstance(Path indexWriterPath) {
		if (map == null)
			throw new RuntimeException("invalid state. After LuceneHandler.closeResources(), you can't get instances.");
		if (Files.isDirectory(indexWriterPath.getParent()) && Files.isWritable(indexWriterPath.getParent())) {
			String pathString = indexWriterPath.toAbsolutePath().toString();
			if (!map.containsKey(pathString)) {
				LuceneHandler newInstance = new LuceneHandler();
				newInstance.writerInit(indexWriterPath);
				map.put(pathString, newInstance);
			}
			return map.get(pathString);
		}
		throw new InvalidParameterException("invalid path for index writer. \n check directory and write permission.");
	}

	private LuceneHandler() {

	}

	private void writerInit(Path path) {
		try {
			dir = FSDirectory.open(path);
			analyzer = new StandardAnalyzer();
			iwc = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(dir, iwc);
			indexReader = DirectoryReader.open(writer); // commit() is important
														// for real-time search
			searcher = new IndexSearcher(indexReader);
		} catch (IOException e) {
			throw new RuntimeException("lucene IndexWriter initialization is failed");
		}
	}

	/**
	 * handyfinder object indexing API
	 * 
	 * @param list
	 * @throws IOException
	 */
	public void indexDirectories(List<Directory> list) throws IOException {
		checkIndexWriter();
		for (Directory dir : list) {
			Path tmp = Paths.get(dir.getPathString());
			if (dir.isRecusively())
				indexDirectory(tmp, true);
			else
				indexDirectory(tmp, false);
		}
	}

	/**
	 * single directory indexing API
	 * commit() call at end
	 * 
	 * @param path
	 * @param recursively
	 * @throws IOException
	 */
	public void indexDirectory(Path path, boolean recursively) throws IOException {
		checkIndexWriter();

		if (Files.isDirectory(path)) {
			Path rootDirectory = path;
			if (recursively) {
				Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (attrs.isRegularFile()) {
							try {
								index(file);
							} catch (ParseException e) {
								log.error(ExceptionUtils.getStackTrace(e));
							}
						}
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				Files.walkFileTree(rootDirectory, EnumSet.noneOf(FileVisitOption.class), 1,
						new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								if (attrs.isRegularFile()) {
									try {
										index(file);
									} catch (ParseException e) {
										log.error(ExceptionUtils.getStackTrace(e));
									}
								}
								return FileVisitResult.CONTINUE;
							}
						});
			}
		}
		writer.commit(); // commit() is important for real-time search
	}

	/**
	 * single file indexing API
	 * 
	 * @param path
	 * @throws IOException
	 * @throws ParseException
	 */
	private void index(Path path) throws IOException, ParseException {
		Document doc = new Document();
		Field pathString = new StringField("pathString", path.toAbsolutePath().toString(), Field.Store.YES);

		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		type.setStored(true);
		type.setStoreTermVectors(true);
		Field content = new Field("contents", JSearch.extractContentsFromFile(path.toFile()), type);

		doc.add(pathString);
		doc.add(content);
		writer.updateDocument(new Term("pathString", path.toAbsolutePath().toString()), doc);
		log.info("complete : " + path);
	}

	public TopDocs search(String fullString) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		checkDirectoryReader();
		MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] { "pathString", "contents" }, analyzer);
		// QueryParser parser = new QueryParser("contents", new
		// StandardAnalyzer());
		String[] partialQuery = fullString.split(" ");
		StringBuilder sb = new StringBuilder();
		for (String element : partialQuery) {
			sb.append(QueryParser.escape(element) + "* ");
		}
		Query query = parser.parse(sb.toString());// QueryParser.escape(

		TopDocs docs = searcher.search(query, 100);
		return docs;
	}

	public Document getDocument(int docid) throws IOException {
		checkDirectoryReader();
		return searcher.doc(docid);
	}

	public Map<String, Integer> getTermFrequencies(int docId) throws IOException {
		checkDirectoryReader();
		return getTermFrequencies(indexReader, docId);

	}

	private Map<String, Integer> getTermFrequencies(IndexReader reader, int docId) throws IOException {
		Terms vector = reader.getTermVector(docId, "contents");
		TermsEnum termsEnum = null;
		termsEnum = vector.iterator();
		Map<String, Integer> frequencies = new HashMap<>();
		BytesRef text = null;
		while ((text = termsEnum.next()) != null) {
			String term = text.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			frequencies.put(term, freq);
			// terms.add(term);
		}
		return frequencies;
	}

	private void checkIndexWriter() {
		if (writer == null) {
			throw new RuntimeException("invalid state. After LuceneHandler.closeResources(), you can't get instances.");
		}
	}

	private void checkDirectoryReader() {
		if (indexReader == null) {
			throw new RuntimeException("invalid state. After LuceneHandler.closeResources(), you can't search.");
		}

	}

	/**
	 * resource close (indexWriter)
	 * 
	 * @throws IOException
	 */
	private void close() throws IOException {
		writer.close();
		writer = null;
		indexReader.close();
		indexReader = null;
	}

	public void deleteAllIndexesFromFileSystem() throws IOException {
		writer.deleteAll();
		writer.commit();
	}

	/**
	 * after method called, you can't get instance.
	 */
	public static void closeResources() {
		Iterator<LuceneHandler> iter = map.values().iterator();
		while (iter.hasNext()) {
			try {
				iter.next().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		map = null;
	}
}
