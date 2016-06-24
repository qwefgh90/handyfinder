package io.github.qwefgh90.handyfinder.lucene;

import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidParameterException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.activity.InvalidActivityException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwefgh90.io.jsearch.FileExtension;
import com.qwefgh90.io.jsearch.JSearch;
import com.qwefgh90.io.jsearch.JSearch.ParseException;

/**
 * document indexing, search class based on Lucene
 * 
 * @author choechangwon
 * @since 16/05/13
 *
 */
@SuppressWarnings("deprecation")
public class LuceneHandler implements Cloneable, AutoCloseable {

	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandler.class);

	// writer
	private Path indexWriterPath;
	private org.apache.lucene.store.Directory dir;
	private Analyzer analyzer;
	private IndexWriterConfig iwc;
	private IndexWriter writer;

	// reader / searcher
	private DirectoryReader indexReader;
	private IndexSearcher searcher;
	private StandardQueryParser parser;

	// indexing state (startIndex(), stopIndex() use state)
	public enum INDEX_WRITE_STATE {
		PROGRESS, STOPPING, READY
	}

	private INDEX_WRITE_STATE writeState = INDEX_WRITE_STATE.READY; // current state
	private int currentProgress = 0; // indexed documents count
	private int totalProcess = 0; // total documents count to be indexed
	private CommandInvoker invokerForCommand; // for command to client
	private LuceneHandlerOption option;

	/**
	 * manage state private API
	 * 
	 * @param state
	 */
	private void updateHandlerState(INDEX_WRITE_STATE state) {
		// progress
		if (state == INDEX_WRITE_STATE.PROGRESS)
			this.writeState = state;

		// terminate
		if (state == INDEX_WRITE_STATE.READY) {
			currentProgress = 0;
			totalProcess = 0;
			this.writeState = state;
		}

		// current progress
		if (state == LuceneHandler.INDEX_WRITE_STATE.STOPPING) {
			if (writeState == LuceneHandler.INDEX_WRITE_STATE.PROGRESS) {
				this.writeState = state;
			}
		}
	}

	/**
	 * current indexing state
	 * 
	 * @return
	 */
	public INDEX_WRITE_STATE getWriteState() {
		return writeState;
	}

	private static ConcurrentHashMap<String, LuceneHandler> map = new ConcurrentHashMap<>();

	/**
	 * static factory method
	 * 
	 * @param indexWriterPath
	 *            : path where index stored
	 * @return object identified by path
	 */
	public static LuceneHandler getInstance(Path indexWriterPath,
			CommandInvoker invoker, LuceneHandlerOption option) {
		if (Files.isDirectory(indexWriterPath.getParent())
				&& Files.isWritable(indexWriterPath.getParent())) {
			String pathString = indexWriterPath.toAbsolutePath().toString();
			if (!map.containsKey(pathString)) {
				LuceneHandler newInstance = new LuceneHandler();
				newInstance.writerInit(indexWriterPath);
				newInstance.invokerForCommand = invoker;
				newInstance.option = option;
				map.put(pathString, newInstance);
			}
			return map.get(pathString);
		}
		throw new InvalidParameterException(
				"invalid path for index writer. \n check directory and write permission.");
	}

	private LuceneHandler() {
	}

	/**
	 * object initialization identified by path
	 * 
	 * @param path
	 * @throws RuntimeException
	 *             index directory error in file system
	 */
	private void writerInit(Path path) {
		try {
			indexWriterPath = path;
			dir = FSDirectory.open(path);
			analyzer = new StandardAnalyzer();
			iwc = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(dir, iwc);
			if (writer.numDocs() == 0)
				writer.addDocument(new Document());
			writer.commit();
			@SuppressWarnings("unused")
			int count = writer.numDocs();
			indexReader = DirectoryReader.open(dir); // commit() is important
			// for real-time search
			searcher = new IndexSearcher(indexReader);
			parser = new StandardQueryParser();
			parser.setAllowLeadingWildcard(true);
		} catch (IOException e) {
			throw new RuntimeException(
					"lucene IndexWriter initialization is failed"
							+ ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * handyfinder object indexing API
	 * 
	 * @param list
	 * @throws IOException
	 * @throws IllegalStateException
	 *             already start index
	 */
	public void startIndex(List<Directory> list) throws IOException {
		if (INDEX_WRITE_STATE.PROGRESS == writeState)
			throw new IllegalStateException("already indexing");
//		checkIndexWriter();
		try {
			totalProcess = sizeOfindexDirectories(list);
			updateHandlerState(INDEX_WRITE_STATE.PROGRESS);
			invokerForCommand.startProgress(totalProcess);
			indexDocuments(list);
		} finally {
			updateHandlerState(INDEX_WRITE_STATE.READY);
			invokerForCommand.terminateProgress(totalProcess);
		}
	}

	/**
	 * remove or update indexed documents
	 * 
	 * @param rootIndexDirectory
	 *            a list of index directory on top
	 */
	public void updateIndexedDocuments(List<Directory> rootIndexDirectory) {
		if (INDEX_WRITE_STATE.PROGRESS == writeState)
			throw new IllegalStateException("already indexing");
//		checkDirectoryReader();
//		checkIndexWriter();
		//count variables
		int nonPresentCount = 0;
		int nonContainedCount = 0;
		int updateCount = 0;
		try {
			updateHandlerState(INDEX_WRITE_STATE.PROGRESS);
			invokerForCommand.startUpdateSummary();
			Map.Entry<List<Document>, Integer> returnValue;
			
			//clean non present file
			List<Document> list = getDocumentList();
			returnValue = cleanNonPresentInternalIndex(list);
			list = returnValue.getKey();
			nonPresentCount = returnValue.getValue();
			//clean non contained file
			returnValue = cleanNonContainedInternalIndex(list, rootIndexDirectory);
			list = returnValue.getKey();
			nonContainedCount = returnValue.getValue();
			//update file
			updateCount = updateContentInternalIndex(list);
		} finally {
			updateHandlerState(INDEX_WRITE_STATE.READY);
			invokerForCommand.terminateUpdateSummary(nonPresentCount, nonContainedCount, updateCount);
		}
	}

	/**
	 * stop indexing
	 */
	public void stopIndex() {
		updateHandlerState(LuceneHandler.INDEX_WRITE_STATE.STOPPING);
	}

	/**
	 * search full string which contains space charactor. it's translated to
	 * Query
	 * 
	 * @param fullString
	 * @return
	 * @throws IOException
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws QueryNodeException
	 * @throws InvalidActivityException
	 *             - now indexing
	 * @throws IOException
	 * @throws IndexException
	 */
	public TopDocs search(String fullString) throws QueryNodeException,
			IOException {
		// if (INDEX_WRITE_STATE.PROGRESS == writeState)
		// throw new IndexException("now indexing");
		checkDirectoryReader();

		Query q1 = parser.parse(addBiWildcardString(fullString), "pathString");
		Query q2 = parser.parse(addWildcardString(fullString), "contents");

		BooleanQuery query = new BooleanQuery.Builder().add(q1, Occur.SHOULD)
				.add(q2, Occur.SHOULD).build();
		TopDocs docs = searcher.search(query,
				option.basicOption.getLimitCountOfResult());
		return docs;
	}

	/**
	 * get Document by docid
	 * 
	 * @param docid
	 * @return Document object
	 * @throws IOException
	 */
	public Document getDocument(int docid) throws IOException {
		checkDirectoryReader();
		return searcher.doc(docid);
	}

	/**
	 * get explanation object
	 * 
	 * @param docid
	 * @param queryString
	 * @return
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws IOException
	 * @throws QueryNodeException
	 */
	public Explanation getExplanation(int docid, String queryString)
			throws org.apache.lucene.queryparser.classic.ParseException,
			IOException, QueryNodeException {
		checkDirectoryReader();
		Query query = getBooleanQuery(queryString);
		Explanation explanation = searcher.explain(query, docid);

		return explanation;
	}

	/**
	 * 
	 * @return a count of indexed documents
	 */
	public int getDocumentCount() {
		checkDirectoryReader();
		return indexReader.numDocs();
	}

	/**
	 * if there is no matched Field, return null.
	 * 
	 * @param docid
	 * @param queryString
	 * @return matched field name
	 * @throws IOException
	 */
	public String getMatchedField(int docid, String queryString)
			throws IOException {
		checkDirectoryReader();
		Document doc = getDocument(docid);
		for (IndexableField field : doc.getFields()) {
			Query query = new WildcardQuery(new Term(field.name(),
					addWildcardString(queryString)));
			Explanation ex = searcher.explain(query, docid);
			if (ex.isMatch()) {
				return field.name();
			}
		}
		return null;
	}

	/**
	 * highlight best summary to be returned
	 * 
	 * @param docid
	 * @param queryString
	 * @return
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws IOException
	 * @throws InvalidTokenOffsetsException
	 * @throws QueryNodeException
	 */
	public String highlight(int docid, String queryString)
			throws org.apache.lucene.queryparser.classic.ParseException,
			IOException, InvalidTokenOffsetsException, QueryNodeException,
			ParseException {
		checkDirectoryReader();
		StringBuilder sb = new StringBuilder();
		Document doc = searcher.doc(docid);
		Query query = getBooleanQuery(queryString);
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
		Highlighter highlighter = new Highlighter(htmlFormatter,
				new QueryScorer(query));
		String pathString = doc.get("pathString");
		if(!Files.exists(Paths.get(pathString)))
			throw new IOException(pathString+" does not exists.");
		String contents = JSearch.extractContentsFromFile(pathString);
		Document tempDocument = new Document();
		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		type.setStored(true);
		Field contentsField = new Field("contents", contents, type);
		tempDocument.add(contentsField);

		try (TokenStream tokenStream = TokenSources.getAnyTokenStream(
				indexReader, docid, "contents", tempDocument, analyzer)) {
			TextFragment[] frag = highlighter.getBestTextFragments(tokenStream,
					contents, false, 2);// highlighter.getBestFragments(tokenStream,
			for (int j = 0; j < frag.length; j++) {
				if ((frag[j] != null) && (frag[j].getScore() > 0)) {
					sb.append(frag[j].toString());
				}
			}
		}

		if (sb.length() != 0) {
			return sb.toString();
		} else {
			try (TokenStream tokenStream = TokenSources.getAnyTokenStream(
					indexReader, docid, "pathString", analyzer)) {
				TextFragment[] frag = highlighter.getBestTextFragments(
						tokenStream, pathString, false, 2);// highlighter.getBestFragments(tokenStream,
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						sb.append(frag[j].toString());
					}
				}
			}
			int length = 200 - sb.toString().length();
			sb.append(contents.substring(0,
					contents.length() < length ? contents.length() : length));
		}
		return sb.toString();
	}

	/**
	 * get term vectors from "contents" field
	 * 
	 * @param docId
	 * @return
	 * @throws IOException
	 */
	public Map<String, Integer> getTermFrequenciesFromContents(int docId)
			throws IOException {
		checkDirectoryReader();
		return getTermFrequenciesFromContents(indexReader, docId);
	}

	public void deleteAllIndexesFromFileSystem() throws IOException {
		checkIndexWriter();
		writer.deleteAll();
		writer.commit();
	}

	/**
	 * after method called, you can't get same instance.
	 * 
	 * @throws Exception
	 */
	public static void closeResources() throws IOException {
		Iterator<LuceneHandler> iter = map.values().iterator();
		while (iter.hasNext()) {
			try {
				iter.next().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		map.clear();
	}

	@Override
	public void close() throws IOException {
		checkIndexWriter();
		if(writer!= null)
			writer.close();
		if(indexReader!=null)
			indexReader.close();
		if(dir!=null)
			dir.close();
		map.remove(indexWriterPath.toAbsolutePath().toString());
		writer = null;
		indexReader = null;
		dir = null;
	}

	void indexDocuments(List<Directory> list) throws IOException {
		for (Directory dir : list) {
			Path tmp = Paths.get(dir.getPathString());
			if (dir.isRecursively()) {
				indexDirectory(tmp, true);
			} else {
				indexDirectory(tmp, false);
			}
			if (writeState == LuceneHandler.INDEX_WRITE_STATE.STOPPING) {
				break;
			}
		}
	}

	void updateIndexReaderAndSearcher() throws IOException {
		DirectoryReader temp = DirectoryReader.openIfChanged(indexReader);
		if (temp != null) {
			indexReader = temp;
			searcher = new IndexSearcher(indexReader);
		}
	}

	/**
	 * single directory index method
	 * 
	 * @param path
	 * @param recursively
	 * @throws IOException
	 */
	void indexDirectory(Path path, boolean recursively) throws IOException {
		checkIndexWriter();
		if (Files.isDirectory(path)) {
			List<Path> pathList = new ArrayList<>();
			Path rootDirectory = path;
			if (recursively) {
				Files.walkFileTree(rootDirectory,
						new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path file,
									BasicFileAttributes attrs)
									throws IOException {
								if (attrs.isRegularFile()) {
									pathList.add(file); // UPDATE
									if (writeState == LuceneHandler.INDEX_WRITE_STATE.STOPPING) {
										return FileVisitResult.TERMINATE;
									}
								}
								return FileVisitResult.CONTINUE;
							}
						});
			} else {
				Files.walkFileTree(rootDirectory,
						EnumSet.noneOf(FileVisitOption.class), 1,
						new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path file,
									BasicFileAttributes attrs)
									throws IOException {
								if (attrs.isRegularFile()) {
									pathList.add(file);
									// check file size // UPDATE
									if (writeState == LuceneHandler.INDEX_WRITE_STATE.STOPPING) {
										return FileVisitResult.TERMINATE;
									}
								}
								return FileVisitResult.CONTINUE;
							}
						});
			}
			pathList.parallelStream()
					.forEach(
							file -> {
								if (writeState == LuceneHandler.INDEX_WRITE_STATE.STOPPING) {
									return;
								}
								// check file size
								try {
									if (Files.size(file) / (1000 * 1000) < option.basicOption
											.getMaximumDocumentMBSize()
											&& !isExists(file.toAbsolutePath()
													.toString()))
										index(file);
									else
										LOG.debug("skip " + file.toString());
								} catch (Exception e) {
									LOG.warn(ExceptionUtils.getStackTrace(e));
								}

								synchronized (this) {
									currentProgress++; // STATE UPDATE
									invokerForCommand
											.updateProgress(currentProgress,
													file, totalProcess); // STATE
								}
							});

		}
	}

	/**
	 * check if indexed. function time test : 1000 of indexed documents consume
	 * 200 millis. maybe 500 micro seconds
	 * 
	 * @param pathString
	 * @return
	 * @throws IOException
	 */
	boolean isExists(String pathString) throws IOException {
		checkDirectoryReader();
		TopDocs results = searcher.search(new TermQuery(new Term("pathString",
				pathString)), 1);
		if (results.totalHits == 0) {
			return false;

		}
		return true;
	}

	/**
	 * 
	 * @return live documents
	 */
	List<Document> getDocumentList() {
		checkDirectoryReader();
		int maxDocId = indexReader.maxDoc();
		List<Document> list = new ArrayList<>();
		for (int i = 0; i < maxDocId; i++) {
			try {
				Bits liveDocs = MultiFields.getLiveDocs(indexReader);
				if (liveDocs == null || liveDocs.get(i)) {
					Document doc = indexReader.document(i);
					// String pathString = doc.get("pathString");
					list.add(doc);
				}
			} catch (IOException e) {
				LOG.warn(e.toString());
			}
		}
		return list;
	}

	/**
	 * 
	 * @param docList
	 *            a list of all pathString in Lucene System
	 * @return
	 */
	Map.Entry<List<Document>,Integer> cleanNonPresentInternalIndex(List<Document> docList) {
		checkIndexWriter();
		int countOfProcessed = 0;
		Stream<Document> parallelStream = docList.parallelStream();
		Map<Boolean, List<Document>> map = parallelStream
				.filter(document -> document.getField("pathString") != null)
				.collect(Collectors
				.partitioningBy(document -> {
					String pathString = document.get("pathString");
					return Files.exists(Paths.get(pathString));
				}));
		Iterator<Document> iteratorOfDeletedFiles = map.get(Boolean.FALSE)
				.iterator();
		while (iteratorOfDeletedFiles.hasNext()) {
			String pathString = iteratorOfDeletedFiles.next().get("pathString");
			try {
				countOfProcessed++;
				writer.deleteDocuments(new Term("pathString", pathString));
				writer.commit();
				LOG.debug("clean non present index : " + pathString);
			} catch (Exception e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}

		return new AbstractMap.SimpleImmutableEntry<>(map.get(Boolean.TRUE),countOfProcessed);
	}

	/**
	 * 
	 * @param docList
	 *            a list of all pathString in Lucene System
	 * @param dirList
	 *            a list of parent directory indexed
	 * @return
	 */
	Map.Entry<List<Document>,Integer> cleanNonContainedInternalIndex(List<Document> docList,
			List<Directory> dirList) {
		checkIndexWriter();
		int countOfProcessed = 0;
		Stream<Document> parallelStream = docList.parallelStream();
		Map<Boolean, List<Document>> map = parallelStream
				.filter(document -> document.getField("pathString") != null)
				.collect(Collectors
				.partitioningBy(document -> {
					String pathString = document.get("pathString");
					Path path = Paths.get(pathString);
					return dirList.parallelStream()
							.noneMatch(
									dir -> { // all test false -> return true
										if (!dir.isUsed())
											return false;
										if (dir.isRecursively()) {
											return path.startsWith(dir
													.getPathString());
										} else {
											return path.getParent().equals(
													dir.getPathString());
										}
									});

				}));
		Iterator<Document> iteratorOfNonContainedFile = map.get(Boolean.TRUE)
				.iterator();
		while (iteratorOfNonContainedFile.hasNext()) {
			String pathString = iteratorOfNonContainedFile.next().get(
					"pathString");
			try {
				countOfProcessed++;
				writer.deleteDocuments(new Term("pathString", pathString));
				writer.commit();
				LOG.debug("clean non contained index : " + pathString);
			} catch (Exception e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}

		return new AbstractMap.SimpleImmutableEntry<>(map.get(Boolean.FALSE),countOfProcessed);
	}

	/**
	 * 
	 * @param docList
	 *            a list of all pathString in Lucene System
	 */
	int updateContentInternalIndex(List<Document> docList) {
		int countOfProcessed = 0;
		Stream<Document> parallelStream = docList.parallelStream();
		Iterator<Path> iteratorForUpdate = parallelStream
				.filter(document -> document.getField("pathString") != null)
				.filter(document -> {
					String pathString = document.get("pathString");
					Path path = Paths.get(pathString);
					if (!Files.exists(path))
						return false;

					long savedLastModifiedTime = document
							.getField("lastModifiedTime").numericValue()
							.longValue();
					long lastModifiedTime = -1;
					try {
						lastModifiedTime = Files.getLastModifiedTime(path)
								.toMillis();
					} catch (Exception e) {
						LOG.warn(ExceptionUtils.getStackTrace(e));
					}
					return lastModifiedTime != savedLastModifiedTime;
				}).map(document -> Paths.get(document.get("pathString")))
				.iterator();
		while (iteratorForUpdate.hasNext()) {
			Path path = iteratorForUpdate.next();
			try {
				countOfProcessed ++;
				LOG.debug("change detected : " + path);
				index(path);
			} catch (Exception e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}
		return Integer.valueOf(countOfProcessed);
	}

	/**
	 * single file indexing API commit() call at end
	 * 
	 * @param path
	 * @throws IOException
	 * @throws ParseException
	 */
	void index(Path path) throws IOException {
		checkIndexWriter();
		MediaType mimeType = FileExtension.getContentType(path.toFile(), path
				.getFileName().toString());
		if (!option.mimeOption.isAllowMime(mimeType.toString()))
			return;

		Document doc = new Document();

		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		// type.setStored(true);
		type.setStoreTermVectors(true);
		type.setStoreTermVectorOffsets(true);

		String contents;

		try {
			contents = JSearch.extractContentsFromFile(path.toFile());
			contents.replaceAll(" +", ""); // erase space
		} catch (ParseException e) {
			LOG.info(ExceptionUtils.getStackTrace(e));
			return;
		}

		BasicFileAttributes attr = Files.readAttributes(path,
				BasicFileAttributes.class);

		StringField mimeTypeString = new StringField("mimeType",
				mimeType.toString(), Store.YES);
		StringField title = new StringField("title", path.getFileName()
				.toString(), Store.YES);
		StringField pathStringField = new StringField("pathString", path
				.toAbsolutePath().toString(), Store.YES);
		LegacyLongField createdTimeField = new LegacyLongField("createdTime",
				attr.creationTime().toMillis(), Store.YES);
		LegacyLongField lastModifiedTimeField = new LegacyLongField(
				"lastModifiedTime", attr.lastModifiedTime().toMillis(),
				Store.YES);

		Field contentsField = new Field("contents", contents, type);
		doc.add(createdTimeField);
		doc.add(title);
		doc.add(pathStringField);
		doc.add(contentsField);
		doc.add(mimeTypeString);
		doc.add(lastModifiedTimeField);
		writer.updateDocument(new Term("pathString", path.toAbsolutePath()
				.toString()), doc);
		LOG.info("indexed : " + path);
		writer.commit(); // commit() is important for real-time search
	}

	/**
	 * handyfinder object indexing API
	 * 
	 * @param list
	 * @throws IOException
	 */
	int sizeOfindexDirectories(List<Directory> list) throws IOException {
		Size size = new Size();
		for (Directory dir : list) {
			Path tmp = Paths.get(dir.getPathString());
			if (dir.isRecursively())
				size.add(sizeOfindexDirectory(tmp, true));
			else
				size.add(sizeOfindexDirectory(tmp, false));
		}
		return size.getSize();
	}

	private class Size {
		int size = 0;

		public void add() {
			size++;
		}

		public void add(Size sizeObj) {
			this.size = this.size + sizeObj.getSize();
		}

		public int getSize() {
			return size;
		}

	}

	/**
	 * single directory indexing API
	 * 
	 * @param path
	 * @param recursively
	 * @throws IOException
	 */
	Size sizeOfindexDirectory(Path path, boolean recursively)
			throws IOException {
		Size size = new Size();
		if (Files.isDirectory(path)) {
			Path rootDirectory = path;
			if (recursively) {
				Files.walkFileTree(rootDirectory,
						new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path file,
									BasicFileAttributes attrs)
									throws IOException {
								if (attrs.isRegularFile()) {
									size.add();
								}
								return FileVisitResult.CONTINUE;
							}
						});
			} else {
				Files.walkFileTree(rootDirectory,
						EnumSet.noneOf(FileVisitOption.class), 1,
						new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path file,
									BasicFileAttributes attrs)
									throws IOException {
								if (attrs.isRegularFile()) {
									size.add();
								}
								return FileVisitResult.CONTINUE;
							}
						});
			}
		}
		return size;
	}

	private BooleanQuery getBooleanQuery(String fullString)
			throws QueryNodeException {

		Query q1 = parser.parse(addBiWildcardString(fullString), "pathString");
		Query q2 = parser.parse(addWildcardString(fullString), "contents");

		BooleanQuery query = new BooleanQuery.Builder().add(q1, Occur.SHOULD)
				.add(q2, Occur.SHOULD).build();
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
		return query;
	}

	private String addWildcardString(String fullString) {

		String[] partialQuery = fullString.split(" ");
		StringBuilder sb = new StringBuilder();
		for (String element : partialQuery) {
			if (sb.length() != 0)
				sb.append(' '); // space added for OR SEARHCHING
			sb.append(QueryParser.escape(element));
			sb.append("*");
		}
		return sb.toString();
	}

	private String addBiWildcardString(String fullString) {
		String[] partialQuery = fullString.split(" ");
		StringBuilder sb = new StringBuilder();
		for (String element : partialQuery) {
			if (sb.length() != 0)
				sb.append(' '); // space added for OR SEARHCHING
			sb.append("*");
			sb.append(QueryParser.escape(element));
			sb.append("*");
		}
		return sb.toString();
	}

	private Map<String, Integer> getTermFrequenciesFromContents(
			IndexReader reader, int docId) throws IOException {
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
			throw new IllegalStateException(
					"invalid state. After LuceneHandler.closeResources() or close(), you can't get instances.");
		}
	}

	private void checkDirectoryReader() {
		if (indexReader == null) {
			throw new IllegalStateException(
					"invalid state. After LuceneHandler.closeResources() or close(), you can't search.");
		}
		try {
			updateIndexReaderAndSearcher();
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
	}
}
