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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activity.InvalidActivityException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

	// indexing state (startIndex(), stopIndex() use state)
	public enum INDEX_WRITE_STATE {
		PROGRESS, STOPPING, READY
	}

	private int currentProgress = 0; // indexed documents count
	private int totalProcess = 0; // total documents count to be indexed
	private CommandInvoker invokerForCommand; // for command to client
	private ILuceneHandlerBasicOptionView basicOption;
	private ILuceneHandlerMimeOptionView mimeOption;

	private INDEX_WRITE_STATE writeStateInternal = INDEX_WRITE_STATE.READY; // no directly access

	/**
	 * update state synchronously
	 * one thread change value to progress state, other thread change 
	 * It's private API
	 * 
	 * @param state
	 */
	private synchronized boolean updateWriteState(INDEX_WRITE_STATE state) {
		// progress
		switch(state){			
		case READY:{
			currentProgress = 0;
			totalProcess = 0;
			this.writeStateInternal = state;
			LOG.debug("LuceneHandler is READY");
			break;
		}

		case PROGRESS:{
			if(writeStateInternal != INDEX_WRITE_STATE.STOPPING){
				this.writeStateInternal = state;
				LOG.debug("LuceneHandler is PROGRESS");
			}
			else
				return false;
			break;
			
		}
		case STOPPING:{
			if(writeStateInternal != INDEX_WRITE_STATE.READY){
				this.writeStateInternal = state;
				LOG.debug("LuceneHandler is STOPPING");
			}
			else
				return false;
			break;
		}
		}
		return true;
	}
	
	/**
	 * 
	 * It's private API
	 * @return
	 */
	public synchronized boolean isStopping(){
		if(writeStateInternal == INDEX_WRITE_STATE.STOPPING)
			return true;
		return false;
	}
	
	/**
	 * 
	 * It's private API
	 * @return
	 */
	public synchronized boolean isReady(){
		if(writeStateInternal == INDEX_WRITE_STATE.PROGRESS 
				||writeStateInternal == INDEX_WRITE_STATE.STOPPING)
			return false;
		return true;
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
			CommandInvoker invoker, 
			 ILuceneHandlerBasicOptionView basicOption,
			 ILuceneHandlerMimeOptionView mimeOption) {
		if (Files.isDirectory(indexWriterPath.getParent())
				&& Files.isWritable(indexWriterPath.getParent())) {
			String indexPathString = indexWriterPath.toAbsolutePath().toString();
			if (!map.containsKey(indexPathString)) {
				LuceneHandler newInstance = new LuceneHandler();
				newInstance.writerInit(indexWriterPath);
				newInstance.invokerForCommand = invoker;
				newInstance.basicOption = basicOption;
				newInstance.mimeOption = mimeOption;
				map.put(indexPathString, newInstance);
			}
			return map.get(indexPathString);
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
			analyzer = new WhitespaceAnalyzer();
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
		if (!isReady())
			throw new IllegalStateException("already indexing");
		// checkIndexWriter();
		if(updateWriteState(INDEX_WRITE_STATE.PROGRESS)){
			try {
				totalProcess = sizeOfindexDirectories(list);
				invokerForCommand.startProgress(totalProcess);
				indexDocuments(list);
			} finally {
				updateWriteState(INDEX_WRITE_STATE.READY);
				invokerForCommand.terminateProgress(totalProcess);
			}
		}
	}

	/**
	 * remove or update indexed documents
	 * 
	 * @param rootIndexDirectory
	 *            a list of index directory on top
	 */
	public void updateIndexedDocuments(List<Directory> rootIndexDirectory) {
		if (!isReady())
			throw new IllegalStateException("already indexing");
		// checkDirectoryReader();
		// checkIndexWriter();
		// count variables
		int nonPresentCount = 0;
		int nonContainedCount = 0;
		int updateCount = 0;
		if(updateWriteState(INDEX_WRITE_STATE.PROGRESS)){
			try {
				invokerForCommand.startUpdateSummary();
				Map.Entry<List<Document>, Integer> returnValue;
	
				// clean non present file
				List<Document> list = getDocumentList();
				returnValue = cleanNonPresentInternalIndex(list);
				list = returnValue.getKey();
				nonPresentCount = returnValue.getValue();
				// clean non contained file
				returnValue = cleanNonContainedInternalIndex(list,
						rootIndexDirectory);
				list = returnValue.getKey();
				nonContainedCount = returnValue.getValue();
				// update file
				updateCount = updateContentInternalIndex(list);
			} finally {
				updateWriteState(INDEX_WRITE_STATE.READY);
				invokerForCommand.terminateUpdateSummary(nonPresentCount,
						nonContainedCount, updateCount);
			}
		}
	}

	/**
	 * stop indexing
	 */
	public void stopIndex() {
		updateWriteState(LuceneHandler.INDEX_WRITE_STATE.STOPPING);
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
		StandardQueryParser parser = new StandardQueryParser();	//not thread safe, object is known as lightweight thing
		parser.setAnalyzer(analyzer);
		parser.setAllowLeadingWildcard(true);
		parser.setLowercaseExpandedTerms(false);
		
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		
		List<String> list = getBiWildcardList(fullString);
		for(String e : list){
			Query query = parser.parse(e, "lowercasePathString");
			builder.add(query, Occur.SHOULD);
		}
		
		list = getWildcardList(fullString);
		for(String e : list){
			Query query = parser.parse(e, "contents");
			builder.add(query, Occur.SHOULD);
		}

		BooleanQuery query = builder.build();
		TopDocs docs = searcher.search(query,
				basicOption.getLimitCountOfResult());
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
	 * 
	 * @param pathString
	 * @return document id in lucene
	 * @throws IOException
	 */
	public Optional<Integer> getDocument(String pathString) throws IOException{
		checkDirectoryReader();
		TopDocs results = searcher.search(new TermQuery(new Term("pathString",
				pathString)), 1);
		if (results.totalHits == 0) {
			return Optional.empty();

		}
		return Optional.of(Integer.valueOf(results.scoreDocs[0].doc));
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
	 * highlight content of document. return null, if path is not valid
	 * @param pathString
	 * @param queryString
	 * @return
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws IOException
	 * @throws InvalidTokenOffsetsException
	 * @throws QueryNodeException
	 * @throws ParseException
	 */
	public Callable<Optional<Map.Entry<String, String>>> highlight(String pathString, String queryString) throws org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidTokenOffsetsException, QueryNodeException, ParseException{
		Optional<Integer> docResult = getDocument(pathString);
		LOG.trace("highlight : " + pathString);
		if(!docResult.isPresent()){
			return null;
		}
		return highlight(docResult.get(), queryString);
	}
	
	/**
	 * return null, if docid is not valid ,otherwise return callable function
	 * @param docid
	 * @param queryString
	 * @return
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws IOException lucene low level error
	 * @throws InvalidTokenOffsetsException
	 * @throws QueryNodeException
	 * @throws ParseException
	 */
	public Callable<Optional<Map.Entry<String, String>>> highlight(int docid, String queryString)
			throws org.apache.lucene.queryparser.classic.ParseException,
			IOException, InvalidTokenOffsetsException, QueryNodeException,
			ParseException {
		checkDirectoryReader();
		
		if(docid >= indexReader.maxDoc() || docid < 0){
			return null;
		}
		
		final Document doc = getDocument(docid);
		Query query = getBooleanQuery(queryString);
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
		Highlighter highlighter = new Highlighter(htmlFormatter,
				new QueryScorer(query));

		final String pathString = doc.get("pathString");
		final String lowercasePathString = doc.get("lowercasePathString");
		LOG.debug("highlight pathString : " + pathString);
		
		if (!Files.exists(Paths.get(pathString)))
			throw new IOException(pathString + " does not exists.");
		Callable<Optional<Map.Entry<String, String>>> getHighlightContent = () -> {
			StringBuilder sb = new StringBuilder();
			String contents;
			try {
				contents = JSearch.extractContentsFromFile(pathString);

				Document tempDocument = new Document();
				FieldType type = new FieldType();
				type.setStored(true);
				Field contentsField = new Field("contents", contents, type);
				Field lowercasePathStringField = new Field("lowercasePathString", lowercasePathString, type);
				tempDocument.add(contentsField);
				tempDocument.add(lowercasePathStringField);

				try (TokenStream tokenStream = TokenSources
						.getAnyTokenStream(indexReader, docid, "contents", tempDocument, analyzer)) {
					TextFragment[] frag = highlighter.getBestTextFragments(
							tokenStream, contents, false, 4);// highlighter.getBestFragments(tokenStream,
					for (int j = 0; j < frag.length; j++) {
						if ((frag[j] != null) && (frag[j].getScore() > 0)) {
							sb.append(frag[j].toString());
						}
					}
					
				}

				if (sb.length() == 0)
				{
					try (TokenStream tokenStream = TokenSources
							.getAnyTokenStream(indexReader, docid, "lowercasePathString", tempDocument, analyzer)) {
						TextFragment[] frag = highlighter.getBestTextFragments(
								tokenStream, pathString, false, 4);// highlighter.getBestFragments(tokenStream,
						for (int j = 0; j < frag.length; j++) {
							if ((frag[j] != null) && (frag[j].getScore() > 0)) {
								sb.append(frag[j].toString());
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
				return Optional.empty();
			}
			//sb.append(contents.substring(0,
			//		contents.length() < 200 ? contents.length()
			//				: 200));
			contents = sb.toString();
			contents = contents.substring(0,
							contents.length() < 200 ? contents.length()
									: 200);
			Map.Entry<String, String> entry = new AbstractMap.SimpleImmutableEntry<>(pathString, contents);
			Optional<Map.Entry<String, String>> result = Optional.of(entry);
			return result;
		};
		return getHighlightContent;
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
		if (writer != null)
			writer.close();
		if (indexReader != null)
			indexReader.close();
		if (dir != null)
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
			if (isStopping()) {
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
									if (isStopping()) {
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
									if (isStopping()) {
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
								if (isStopping()) {
									return;
								}
								// check file size
								try {
									if (Files.size(file) / (1000 * 1000) <= basicOption
											.getMaximumDocumentMBSize()
											&& !isExistsInLuceneIndex(file.toAbsolutePath()
													.toString())){
										index(file);
										synchronized (this) {
											currentProgress++; // STATE UPDATE
											invokerForCommand
													.updateProgress(currentProgress,
															file, totalProcess); // STATE
										}
									}else
										LOG.debug("skip " + file.toString());
								} catch (Exception e) {
									LOG.warn(ExceptionUtils.getStackTrace(e));
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
	boolean isExistsInLuceneIndex(String pathString) throws IOException {
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
	Map.Entry<List<Document>, Integer> cleanNonPresentInternalIndex(
			List<Document> docList) {
		checkIndexWriter();
		int countOfProcessed = 0;
		Stream<Document> parallelStream = docList.parallelStream();
		Map<Boolean, List<Document>> map = parallelStream.filter(
				document -> document.getField("pathString") != null).collect(
				Collectors.partitioningBy(document -> {
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

		return new AbstractMap.SimpleImmutableEntry<>(map.get(Boolean.TRUE),
				countOfProcessed);
	}

	/**
	 * 
	 * @param docList
	 *            a list of all pathString in Lucene System
	 * @param dirList
	 *            a list of parent directory indexed
	 * @return
	 */
	Map.Entry<List<Document>, Integer> cleanNonContainedInternalIndex(
			List<Document> docList, List<Directory> dirList) {
		checkIndexWriter();
		int countOfProcessed = 0;
		Stream<Document> parallelStream = docList.parallelStream();
		Map<Boolean, List<Document>> map = parallelStream.filter(
				document -> document.getField("pathString") != null).collect(
				Collectors.partitioningBy(document -> { // True partition will
														// be excepted.
							String pathString = document.get("pathString");
							Path path = Paths.get(pathString);
							try {
								MediaType type = FileExtension.getContentType(
										path.toFile(), path.toAbsolutePath()
												.toString());
								if (false == mimeOption.isAllowMime(type
										.toString()))
									return true;
								long size = Files.size(path);

								if (size / (1000 * 1000) > basicOption
										.getMaximumDocumentMBSize()) {
									return true;
								}
							} catch (Exception e) {
								LOG.warn(e.toString());
							}
							return dirList.parallelStream().noneMatch(dir -> { // all
																				// test
																				// false
																				// ->
																				// return
																				// true
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

		return new AbstractMap.SimpleImmutableEntry<>(map.get(Boolean.FALSE),
				countOfProcessed);
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
				countOfProcessed++;
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
		if (!mimeOption.isAllowMime(mimeType.toString()))
			return;

		Document doc = new Document();

		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		// type.setStored(true);
		type.setStoreTermVectors(true);
		type.setStoreTermVectorOffsets(true);

		FieldType typeWithStore = new FieldType(type);
		typeWithStore.setStored(true);
		
		String contents;

		try {
			contents = JSearch.extractContentsFromFile(path.toFile());
			contents = contents.replaceAll(" +", " "); // erase space
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

		Field lowerPathStringField = new Field("lowercasePathString", path.toAbsolutePath().toString().toLowerCase(), typeWithStore);
		Field contentsField = new Field("contents", contents, type);
		doc.add(createdTimeField);
		doc.add(title);
		doc.add(pathStringField);
		doc.add(lowerPathStringField);
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
		StandardQueryParser parser = new StandardQueryParser();	//not thread safe, object is known as lightweight thing
		parser.setAnalyzer(analyzer);
		parser.setAllowLeadingWildcard(true);
		parser.setLowercaseExpandedTerms(false);
		
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		
		List<String> list = getBiWildcardList(fullString);
		for(String e : list){
			Query query = parser.parse(e, "lowercasePathString");
			builder.add(query, Occur.SHOULD);
		}
		
		list = getWildcardList(fullString);
		for(String e : list){
			Query query = parser.parse(e, "contents");
			builder.add(query, Occur.SHOULD);
		}

		BooleanQuery query = builder.build();
		return query;
	}

	private String getWildcardString(String fullString) {
		fullString = fullString.replaceAll(" +", " ");
		String[] partialQuery = fullString.split(" ");
		StringBuilder sb = new StringBuilder();
		for (String element : partialQuery) {
			if(sb.length() != 0)
				sb.append(" ");
			sb.append(QueryParser.escape(element)+"*");
		}
		return sb.toString();
	}

	private List<String> getWildcardList(String fullString) {
		List<String> list = new ArrayList<>();
		fullString = fullString.replaceAll(" +", " ");
		String[] partialQuery = fullString.split(" ");
		for (String element : partialQuery) {
			list.add(QueryParser.escape(element)+"*");
		}
		return list;
	}
	
	private List<String> getBiWildcardList(String fullString) {
		List<String> list = new ArrayList<>();
		fullString = fullString.replaceAll(" +", " ");
		String[] partialQuery = fullString.split(" ");
		for (String element : partialQuery) {
			list.add("*"+QueryParser.escape(element)+"*");
		}
		return list;
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
