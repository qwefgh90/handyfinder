package io.github.qwefgh90.handyfinder.lucene;

import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.TARGET_MODE;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerState.INDEX_WRITE_STATE;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerState.TransitionObserver;
import io.github.qwefgh90.handyfinder.lucene.Result.IndexResult;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.memory.monitor.FunctionalLatch;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;
import io.github.qwefgh90.jsearch.JSearch;

import java.io.File;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activity.InvalidActivityException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
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
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
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

/**
 * document indexing, search class based on Lucene
 * 
 * @author choechangwon
 * @since 16/05/13
 *
 */
public final class LuceneHandler implements Cloneable, AutoCloseable {

	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandler.class);
	
	// immutable infomation
	final private Path writerPath;	
	final File writerFile;
	final private org.apache.lucene.store.Directory dir;
	final private Analyzer analyzer;
	final private int minGramSize = 2;
	final private int maxGramSize = 8;
	final private long multiplyForNGram;
	final FunctionalLatch latch = new FunctionalLatch();
	final LuceneHandlerState state = LuceneHandlerState.self;
	final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	final ExecutorService mainExecutor = Executors.newCachedThreadPool();
	final ExecutorService indexExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	
	
	private AtomicInteger currentProgress = new AtomicInteger(0); // indexed documents count
	private AtomicInteger totalProcess = new AtomicInteger(0); // total documents count to be indexed
	
	// mutable config, writer, reader, searcher
	private IndexWriterConfig indexConfig;
	private IndexWriter writer;
	private DirectoryReader reader;
	private IndexSearcher searcher;
	
	private CommandInvoker invokerForCommand; // for command to client
	private BasicOption basicOption;
	private MimeOption mimeOption;
	
	private static final ConcurrentHashMap<String, LuceneHandler> map = new ConcurrentHashMap<>();

	/**
	 * static factory method
	 * 
	 * @param writerPath
	 *            : path where index stored
	 * @return object identified by path
	 */
	public static LuceneHandler getInstance(final Path writerPath,
			final CommandInvoker invoker, 
			final BasicOption basicOption,
			final MimeOption mimeOption) {
		if (Files.isDirectory(writerPath.getParent())
				&& Files.isWritable(writerPath.getParent())) {
			final String indexPathString = writerPath.toAbsolutePath().toString();
			if (!map.containsKey(indexPathString)) {
				final LuceneHandler newInstance = new LuceneHandler(writerPath);
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
	
	private LuceneHandler(final Path path) {
		try {
			final Map<String, Analyzer> perFieldAnalyzer = new TreeMap<>();
			perFieldAnalyzer.put("pathStringForQuery", getKeywordAnalyzer());
			analyzer = new PerFieldAnalyzerWrapper(getNgramAnalyzer(), perFieldAnalyzer);
			writerPath = path;
			writerFile = path.toFile();
			dir = FSDirectory.open(path);
			indexConfig = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(dir, indexConfig);
			if (writer.numDocs() == 0)
				writer.addDocument(new Document());
			writer.commit();

			reader = DirectoryReader.open(dir);
			searcher = new IndexSearcher(reader);

			long temp = 0;
			for(int i=minGramSize; i<maxGramSize; i++){
				temp += i;
			}
			multiplyForNGram = temp;
			state.addObserverOfTransition(new TransitionObserver(Optional.empty()
					, Optional.of(LuceneHandlerState.INDEX_WRITE_STATE.READY)
					, (self)->{
						currentProgress.set(0);
						totalProcess.set(0);
					}));
		} catch (IOException e) {
			throw new RuntimeException(
					"Objects Initialization of lucene failed "
							+ ExceptionUtils.getStackTrace(e));
		}
	}
	
	@PostConstruct
	public void initialize(){
		LOG.debug("initialize() is called");
		scheduledExecutor.schedule(() -> {
			if(Files.exists(AppStartupConfig.resetFilePath)){
				try {
					this.deleteAllIndexesFromFileSystem();
					Files.delete(AppStartupConfig.resetFilePath);
				} catch (IOException e) {
					LOG.warn(ExceptionUtils.getStackTrace(e));
				}
			}
			restartIndexAsync(basicOption.getDirectoryList());
		}
		, 3, TimeUnit.SECONDS); 
	}
	
	@PreDestroy
	public void destroy() throws InterruptedException, IOException, ExecutionException, TimeoutException{
		LOG.debug("destroy() is called");
		stopIndexAsync().get(20, TimeUnit.SECONDS);
		scheduledExecutor.shutdown();
		scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
		mainExecutor.shutdown();
		mainExecutor.awaitTermination(10, TimeUnit.SECONDS);
		indexExecutor.shutdown();
		indexExecutor.awaitTermination(10, TimeUnit.SECONDS);
		LuceneHandler.closeResources();
	}

	/**
	 * If some changes are in indexes, update it.
	 * @throws IOException
	 */
	private void updateIndexReader() throws IOException {
		DirectoryReader temp = DirectoryReader.openIfChanged(reader);
		if (temp != null) {
			reader = temp;
			searcher = new IndexSearcher(reader);
		}
	}
	
	/**
	 * If writer is close, recover writer
	 * @throws IOException
	 */
	private void recoverIndexWriter() throws IOException{
		if(writer != null && !writer.isOpen()){
			LOG.debug("Reopen IndexWriter");
			indexConfig = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(dir, indexConfig);
		}
	}
	
	/**
	 * return NGram analyzer
	 * @return
	 * @throws IOException
	 */
	private Analyzer getNgramAnalyzer() throws IOException{
		final Map<String, String> map = new HashMap<>();
		map.put("minGramSize", String.valueOf(minGramSize));
		map.put("maxGramSize", String.valueOf(maxGramSize));
		final Analyzer ngramAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(NGramTokenizerFactory.class, map)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.build();
		return ngramAnalyzer;
	}
	
	private Analyzer getKeywordAnalyzer() throws IOException {
		final Analyzer pathAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(KeywordTokenizerFactory.class)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.build();
		return pathAnalyzer;
	}

	/**
	 * Start to index all files in a list of directories
	 * @param list a list of directories
	 * @return a future of process of index
	 * @throws IOException
	 * @throws IllegalStateException if state cannot be changed to progress normally throw it. 
	 */
	public CompletableFuture<Integer> startIndexAsync(final List<Directory> list) throws IOException {
		compactAndCleanIndex();
		totalProcess.set(sizeOfindexDirectories(list));
		invokerForCommand.startProgress(totalProcess.get());
		
		LOG.info("First try");
		final List<CompletableFuture<IndexResult>> listOfFutureToBeIndexed = indexDirectoriesAsync(list);	//First try
		final CompletableFuture<Integer> futureOfAllTry = CompletableFuture.allOf(listOfFutureToBeIndexed.toArray(new CompletableFuture[listOfFutureToBeIndexed.size()]))
				.thenComposeAsync((result) -> {
					return CompletableFuture.supplyAsync(() ->{	//Second try
						//Get a result from futures
						final List<IndexResult> listOfFirstTry = listOfFutureToBeIndexed.stream()
								.map((CompletableFuture<IndexResult> future) -> {try {
									return future.get();
								} catch (InterruptedException e) {
									throw new IllegalStateException(e);
								} catch (ExecutionException e) {
									throw new IllegalStateException("A stream is failed. " + ExceptionUtils.getStackTrace(e));
								}}).collect(Collectors.toList());
						final long firstSuccessCount = listOfFirstTry.stream().filter((IndexResult _result) -> {
							return _result.code == IndexResult.IndexResultCode.SUCCESS;}).count();
						
						//Print a log
						LOG.info("First try to index is completed. " + firstSuccessCount + " / " + listOfFirstTry.size());

						//Get failed documents
						final Stream<Path> failedDocumentsStream = listOfFirstTry.stream()
								.filter((IndexResult _result) -> { return _result.code == IndexResult.IndexResultCode.EXCEPTION;})
								.map((IndexResult mapParam) -> { return mapParam.path.get(); });

						LOG.info("Second try");
						//Try it
						final List<CompletableFuture<IndexResult>> futureForSecondTry = indexFailedDocumentsAsync(failedDocumentsStream);
						try {
							//Await computing
							CompletableFuture.allOf(futureForSecondTry.toArray(new CompletableFuture[futureForSecondTry.size()])).get();

							//Get a result from futures
							final List<IndexResult> listOfSecondTry = futureForSecondTry.stream()
									.map((CompletableFuture<IndexResult> future) -> {try {
										return future.get();
									} catch (InterruptedException e) {
										throw new IllegalStateException(e);
									} catch (ExecutionException e) {
										throw new IllegalStateException("A stream is failed. " + ExceptionUtils.getStackTrace(e));
									}}).collect(Collectors.toList());

							final long secondSuccessCount = listOfSecondTry.stream().filter((IndexResult _result) -> {
								return _result.code == IndexResult.IndexResultCode.SUCCESS;}).count();

							//Print a log
							LOG.info("Second try to index is completed. "  + secondSuccessCount + " / " + listOfSecondTry.size());
							compactAndCleanIndex();
							invokerForCommand.terminateProgress(totalProcess.get());
							return (int)(firstSuccessCount + secondSuccessCount);						
						} catch (InterruptedException | ExecutionException e) {
							LOG.warn(ExceptionUtils.getStackTrace(e));
							return (int)(firstSuccessCount);
						} catch (IOException e) {
							LOG.warn(ExceptionUtils.getStackTrace(e));
							return (int)(firstSuccessCount);
						}

					});
				});
		return futureOfAllTry;
	}
	
	/**
	 * Stop and update the index
	 * @return if a process succeed return true, otherwise return false
	 */
	public CompletableFuture<Boolean> restartIndexAsync(List<Directory> list){
		CompletableFuture<Boolean> f = CompletableFuture.supplyAsync(() -> {
			try {
				stopIndexAsync().get(20, TimeUnit.SECONDS);
				if (!state.isReady())
					throw new IllegalStateException("Can't change a state to progress");
				if(state.progress()){
					try{
						startIndexAsync(list).get();
						updateIndexedDocuments(list);
					}finally{
						state.ready();
					}
				}
			} catch (IOException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
				return false;
			} catch (Exception e){
				LOG.warn(ExceptionUtils.getStackTrace(e));
				return false;
			}
			return true;
		});
		return f;
	}

	/**
	 * remove or update index
	 * 
	 * @param rootIndexDirectory index of a directory to be updated
	 * @return if a exception occurs return false, otherwise return true 
	 */
	public boolean updateIndexedDocuments(List<Directory> rootIndexDirectory) {
		int nonPresentCount = 0;
		int nonContainedCount = 0;
		int updateCount = 0;
			try {
				invokerForCommand.startUpdateSummary();
				Map.Entry<List<Document>, Integer> tempReturnValue;

				// clean non present file
				final List<Document> list = getDocumentList();
				tempReturnValue = cleanNonPresentInternalIndex(list);
				nonPresentCount = tempReturnValue.getValue();
				// clean non contained file
				tempReturnValue = cleanNonContainedInternalIndex(tempReturnValue.getKey(),
						new ArrayList<>(rootIndexDirectory));
				nonContainedCount = tempReturnValue.getValue();
				// update index
				updateCount = updateContentInternalIndex(tempReturnValue.getKey());
				// compact and clean
				compactAndCleanIndex();
			} catch (IOException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
				return false;
			} finally {
				invokerForCommand.terminateUpdateSummary(nonPresentCount,
						nonContainedCount, updateCount);
			}
		return true;
	}

	/**
	 * Stop to index asynchronously
	 * @return if state is changed return true, otherwise return false
	 */
	public CompletableFuture<Boolean> stopIndexAsync() {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		final TransitionObserver observer = new TransitionObserver(
				Optional.of(INDEX_WRITE_STATE.STOPPING)
				, Optional.of(INDEX_WRITE_STATE.READY)
				, (self) -> {
					result.complete(true);
					state.removeObserver(self);
				});
		if(state.stopping()){
			state.addObserverOfTransition(observer);
			latch.signalAll();
		}else
			result.complete(false);
		return result;
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
	public List<ScoreDoc> search(String fullString, int lowerBound) throws QueryNodeException,
	IOException {
		checkDirectoryReader();
		final TopDocs docs = searcher.search(getHandyFinderQuery(fullString),
				basicOption.getLimitCountOfResult());
		
		final List<ScoreDoc> docList = new ArrayList<>();
		for(int i=0; i < docs.scoreDocs.length; i++){
			if(docs.scoreDocs[i].score > lowerBound)
				docList.add(docs.scoreDocs[i]);
		}
		
		return docList;
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
		TopDocs results = searcher.search(new TermQuery(new Term("pathString", pathString)), 1);
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
		Query query = getHandyFinderQuery(queryString);
		Explanation explanation = searcher.explain(query, docid);

		return explanation;
	}

	/**
	 * 
	 * @return a count of indexed documents
	 */
	public int getDocumentCount() {
		checkDirectoryReader();
						final List<Document> list = getDocumentList();
		return (int) list.stream()
				.filter(doc -> doc.get("pathString") != null)
				.filter(doc -> !doc.get("pathString").trim().isEmpty()).count();
	}
	
	/**
	 * 
	 * @return path List
	 */
	public List<String> getDocumentPathList(){
		checkDirectoryReader();
		final List<Document> list = getDocumentList();
		return list.stream()
				.map(doc -> doc.get("pathString"))
				.filter(pathString -> pathString != null)
				.filter(pathString -> !pathString.trim().isEmpty())
				.collect(Collectors.toList());
		//reader
		
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
	 */
	public Callable<Optional<Map.Entry<String, String>>> highlight(String pathString, String queryString) throws org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidTokenOffsetsException, QueryNodeException{
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
	 */
	public Callable<Optional<Map.Entry<String, String>>> highlight(int docid, String queryString)
			throws org.apache.lucene.queryparser.classic.ParseException,
			IOException, InvalidTokenOffsetsException, QueryNodeException
	{
		checkDirectoryReader();

		if(docid >= reader.maxDoc() || docid < 0){
			return null;
		}

		final Document doc = getDocument(docid);
		final Query query = getHandyFinderQuery(queryString);
		final SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("","");
		final Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
		final String pathString = doc.get("pathString");

		if (!Files.exists(Paths.get(pathString)))
			throw new IOException(pathString + " does not exists.");
		
		final Callable<Optional<Map.Entry<String, String>>> getHighlightContent = () -> {
			final StringBuilder sb = new StringBuilder();
			try {
				final String contents = extractContentsFromFile(new File(pathString));
				
				Document tempDocument = new Document();
				FieldType type = new FieldType();
				type.setStored(true);
				Field contentsField = new Field("contents", contents.toString(), type);
				tempDocument.add(contentsField);


				//first, lucene find offset of sentence and highlight sentence from file
				try (@SuppressWarnings("deprecation")
				TokenStream tokenStream = TokenSources
						.getAnyTokenStream(reader, docid, "contents", tempDocument, analyzer)) {
					TextFragment[] frag = highlighter.getBestTextFragments(
							tokenStream, contents, false, 4);
					for (int j = 0; j < frag.length; j++) {
						if ((frag[j] != null) && (frag[j].getScore() > 0)) {
							sb.append(frag[j].toString());
						}
					}
				}

				if (sb.length() == 0)
				{
					try (@SuppressWarnings("deprecation")
					TokenStream tokenStream = TokenSources
							.getAnyTokenStream(reader, docid, "pathStringForQuery", analyzer)) {
						TextFragment[] frag = highlighter.getBestTextFragments(
								tokenStream, pathString, false, 4);// highlighter.getBestFragments(tokenStream,
						for (int j = 0; j < frag.length; j++) {
							if ((frag[j] != null) && (frag[j].getScore() > 0)) {
								sb.append(frag[j].toString());
							}
						}
					}
				}
				if(sb.length() == 0)
					sb.append(queryString);
				
			} catch (Exception e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
				return Optional.empty();
			}
			final String highlightedText = sb.toString();
			final String trimHighlightedText = highlightedText.substring(0,
					highlightedText.length() < 200 ? highlightedText.length()
							: 200);
			final Map.Entry<String, String> entry = new AbstractMap.SimpleImmutableEntry<>(pathString, trimHighlightedText);
			final Optional<Map.Entry<String, String>> result = Optional.of(entry);
			return result;
		};
		return getHighlightContent;
	}

	public void deleteAllIndexesFromFileSystem() throws IOException {
		checkAndRecoverIndexWriter();
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
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}
		map.clear();
	}

	@Override
	public void close() throws IOException {
		latch.terminate();
		if (writer != null){
			writer.commit();
			writer.close();
		}
		if (reader != null)
			reader.close();
		if (dir != null)
			dir.close();
		map.remove(writerPath.toAbsolutePath().toString());
		writer = null;
		reader = null;
	}
	
	List<CompletableFuture<IndexResult>> indexFailedDocumentsAsync(Stream<Path> failedPathList){
		if (state.isStopping()) {
			return Collections.emptyList();
		}
		if (!isDiskAvailable()){
			return Collections.emptyList();
		}
		List<CompletableFuture<IndexResult>> someFutureList = failedPathList
				.map(_path -> CompletableFuture.supplyAsync(() -> indexFile(_path), indexExecutor))
				.collect(Collectors.toList());
		return someFutureList;
	}

	ArrayList<CompletableFuture<IndexResult>> indexDirectoriesAsync(final List<Directory> list) {
		final ArrayList<CompletableFuture<IndexResult>> returnedList = new ArrayList<CompletableFuture<IndexResult>>(1000);
		final List<Directory> copiedList = new ArrayList<Directory>(list);
		for (Directory dir : copiedList) {
			if (state.isStopping()) {
				break;
			}
			if (!isDiskAvailable()){
				break;
			}
			Path path = Paths.get(dir.getPathString());
			if (dir.isRecursively()) {
				indexDirectoryAsync(path, true).ifPresent(futureList -> returnedList.addAll(futureList));
			} else {
				indexDirectoryAsync(path, false).ifPresent(futureList -> returnedList.addAll(futureList));
			}
		}
		return returnedList;
	}

	/**
	 * single directory index method
	 * 
	 * @param path
	 * @param recursively
	 * @throws IOException
	 */
	Optional<ArrayList<CompletableFuture<IndexResult>>> indexDirectoryAsync(final Path path, final boolean recursively) {
		if (Files.isDirectory(path)) {
			final ArrayList<CompletableFuture<IndexResult>> futureList = new ArrayList<CompletableFuture<IndexResult>>(100);
			final List<Path> pathList = new ArrayList<>(1000);
			final Path rootDirectory = path;
			if (recursively) {
				try {
					Files.walkFileTree(rootDirectory,
							new SimpleFileVisitor<Path>() {
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs)
										throws IOException {
							if (attrs.isRegularFile()) {
								if (Files.size(file) / (1000 * 1000) <= basicOption.getMaximumDocumentMBSize()
										&& !isExistsInLuceneIndex(file.toAbsolutePath().toString())){
									final CompletableFuture<IndexResult> f = CompletableFuture.supplyAsync(() -> indexFile(file), indexExecutor);
									futureList.add(f);
									//	}
								} else {
									LOG.trace("skip " + file.toString());
									currentProgress.incrementAndGet(); // STATE UPDATE
								}
								if (state.isStopping()) {
									return FileVisitResult.TERMINATE;
								}
								if (!isDiskAvailable()) {
									return FileVisitResult.TERMINATE;
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					LOG.error(ExceptionUtils.getStackTrace(e));
				}
			} else {
				try {
					Files.walkFileTree(rootDirectory,
							EnumSet.noneOf(FileVisitOption.class), 1,
							new SimpleFileVisitor<Path>() {
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs)
										throws IOException {
							if (attrs.isRegularFile()) {
								if (Files.size(file) / (1000 * 1000) <= basicOption.getMaximumDocumentMBSize()
										&& !isExistsInLuceneIndex(file.toAbsolutePath().toString())){
									final CompletableFuture<IndexResult> f = CompletableFuture.supplyAsync(() -> indexFile(file), indexExecutor);
									futureList.add(f);
								} else {
									LOG.trace("skip " + file.toString());
									currentProgress.incrementAndGet(); // STATE UPDATE
								}
								if (state.isStopping()) {
									return FileVisitResult.TERMINATE;
								}
								if (!isDiskAvailable()) {
									return FileVisitResult.TERMINATE;
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					LOG.error(ExceptionUtils.getStackTrace(e));
				}
			}
			
			if (state.isStopping()) {
				return Optional.of(futureList);
			}
			if (!isDiskAvailable()) {
				return Optional.of(futureList);
			}
			List<CompletableFuture<IndexResult>> someFutureList = pathList.stream()
					.map(_path -> CompletableFuture.supplyAsync(() -> indexFile(_path), indexExecutor))
					.collect(Collectors.toList());
			futureList.addAll(someFutureList);
			return Optional.of(futureList);
		}
		return Optional.empty();
	}
	
	final IndexResult indexFile(Path file) {
		if (state.isStopping()) {
			return IndexResult.STOPPED;
		}
		if (!isDiskAvailable()) {
			return IndexResult.DISK_IS_FULL;
		}
		try {
			if(!index(file)){
				LOG.warn("Indexing may be stopped in operation : " + file.toString());
				return IndexResult.STOPPED;
			}else{
				currentProgress.incrementAndGet();
				invokerForCommand.updateProgress(currentProgress.get(), file, totalProcess.get()); // STATE
				return IndexResult.SUCCESS;
			}
		} catch (Exception e) {
			//failedPathSet.add(file);
			LOG.warn("later, we will index again : " + file.toString());
			LOG.warn(ExceptionUtils.getStackTrace(e));
			return IndexResult.EXCEPTION(Optional.empty(), Optional.of(file));
		}
	}
	
	/**
	 * single file indexing API commit() call at end
	 * 
	 * @param path
	 * @throws IOException
	 */
	boolean index(final Path path) throws IOException {
		final MediaType mimeType = JSearch.getContentType(path.toFile(), path
				.getFileName().toString());
		
		final FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		type.setStoreTermVectors(true);
		type.setStoreTermVectorOffsets(true);

		final FieldType typeWithStore = new FieldType(type);
		typeWithStore.setStored(true);

		final BasicFileAttributes attr = Files.readAttributes(path,
				BasicFileAttributes.class);
		final StringField mimeTypeString = new StringField("mimeType",
				mimeType.toString(), Store.YES);
		final StringField title = new StringField("title", path.getFileName()
				.toString(), Store.YES);
		@SuppressWarnings("deprecation")
		final LegacyLongField createdTimeField = new LegacyLongField("createdTime",
				attr.creationTime().toMillis(), Store.YES);
		@SuppressWarnings("deprecation")
		final LegacyLongField lastModifiedTimeField = new LegacyLongField(
				"lastModifiedTime", attr.lastModifiedTime().toMillis(), Store.YES);
		final Field pathStringField = new StringField("pathString", path.toAbsolutePath().toString(), Store.YES);
		final Field pathStringForQueryField = new Field("pathStringForQuery", path.toAbsolutePath().toString(), typeWithStore);

		final Field contentsField = new Field("contents", extractContentsFromFile(path.toFile()), type);

		final Document doc = new Document();
		doc.add(mimeTypeString);
		doc.add(title);
		doc.add(createdTimeField);
		doc.add(lastModifiedTimeField);
		doc.add(pathStringField);
		doc.add(pathStringForQueryField);
		doc.add(contentsField);
		
		final long maxHeapSize = Runtime.getRuntime().maxMemory();					
		final long currentHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); 
		if((maxHeapSize) < (currentHeap + (Files.size(path) * multiplyForNGram))){	//if over size of max heap
			LOG.debug("Waiting. System hasn't enough memory space for running index");
			try {
				latch.await(() -> {
					try {
						final long _currentHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); 
						if((maxHeapSize) > (_currentHeap + (Files.size(path) * multiplyForNGram))){
							return true;	//have enough memory space
						}else {
							LOG.debug("A job is waiting for realease");
							return false;	//no have enough memory space
						}
					} catch (IOException e) {
						LOG.warn("Exception in functional latch\n" + ExceptionUtils.getStackTrace(e));
						return true;
					}
				}, 1, 10, TimeUnit.SECONDS);
				LOG.debug("go thread");
			} catch (InterruptedException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}

		LOG.debug("\n before update Document\n* Memory check - \n* limit : " +(maxHeapSize) + " / size : " + (currentHeap + (Files.size(path) * multiplyForNGram) 
				+ "\n* Ram buffer for index : " + indexConfig.getRAMBufferSizeMB() + "," + indexConfig.getMaxBufferedDeleteTerms() + "," + indexConfig.getMaxBufferedDocs()
				));
		
		if(state.isStopping())
			return false;
		
		checkAndRecoverIndexWriter();
		// WARNING) This is high memory cost operation
		writer.updateDocument(new Term("pathString", path.toAbsolutePath().toString()), doc);
		writer.commit(); // commit() is important for real-time search
		LOG.info("Indexed : " + path);
		return true;
	}
	
	/*
	private final Consumer<List<Path>> parallelIndex = (pathList) -> {
		final ExecutorService threads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		pathList.forEach((file) -> {
			threads.submit(new Runnable(){
				@Override
				public void run() {
					if (isStopping()) {
						return;
					}
					if (!isDiskAvailable()) {
						return;
					}
					try {
						if(!index(file)){
							LOG.warn("some changes in operation : " + file.toString());
						}else{
							currentProgress.incrementAndGet();
							invokerForCommand.updateProgress(currentProgress.get(), file, totalProcess.get()); // STATE
						}
					} catch (Exception e) {
						failedPathSet.add(file);
						LOG.warn("later, we will index again : " + file.toString());
						LOG.warn(ExceptionUtils.getStackTrace(e));
					}
				}
			});
		});
		threads.shutdown();
		try {
			threads.awaitTermination(24, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			threads.shutdownNow();
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
	};
	*/
	
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
		int maxDocId = reader.maxDoc();
		List<Document> list = new ArrayList<>();
		for (int i = 0; i < maxDocId; i++) {
			try {
				Bits liveDocs = MultiFields.getLiveDocs(reader);
				if (liveDocs == null || liveDocs.get(i)) {
					Document doc = reader.document(i);
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
		checkAndRecoverIndexWriter();
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
			final Document currentDoc = iteratorOfDeletedFiles.next();
			String pathString = currentDoc.get("pathString");
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
		checkAndRecoverIndexWriter();
		int countOfProcessed = 0;
		final Stream<Document> parallelStream = docList.parallelStream();
		final Map<Boolean, List<Document>> map = parallelStream
				.filter(document -> document.getField("pathString") != null)
				.collect(
						Collectors.partitioningBy(document -> { // True partition will
							// be excepted.
							String pathString = document.get("pathString");
							Path path = Paths.get(pathString);
							try {
								long size = Files.size(path);

								if (size / (1000 * 1000) > basicOption.getMaximumDocumentMBSize()) {
									return true;
								}
							} catch (Exception e) {
								LOG.error(e.toString());
							}
							return dirList.parallelStream().noneMatch(dir -> {
								if (dir.isRecursively()) {
									return path.startsWith(dir.getPathString());
								} else {
									return path.getParent().equals(
											dir.getPathString());
								}
							});
						}));
		final Iterator<Document> iteratorOfNonContainedFile = map.get(Boolean.TRUE).iterator();
		
		while (iteratorOfNonContainedFile.hasNext()) {
			final Document currentDoc = iteratorOfNonContainedFile.next();
			String pathString = currentDoc.get("pathString");
			try {
				countOfProcessed++;
				writer.deleteDocuments(new Term("pathString", pathString));
				LOG.debug("clean non contained index : " + pathString);
			} catch (Exception e) {
				LOG.error(ExceptionUtils.getStackTrace(e));
			}
		}
		try {
			writer.commit();
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
		return new AbstractMap.SimpleImmutableEntry<>(map.get(Boolean.FALSE), countOfProcessed);
	}

	/**
	 * 
	 * @param docList
	 *            a list of all pathString in Lucene System
	 */
	int updateContentInternalIndex(List<Document> docList) {
		int countOfProcessed = 0;
		final Stream<Document> parallelStream = docList.parallelStream();
		final Iterator<Path> iteratorForUpdate = parallelStream
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
	
	boolean isDiskAvailable(){	
		final long totalSpace = writerFile.getTotalSpace();
		final long usableSpace = writerFile.getUsableSpace(); 
		final long usingSpace = totalSpace - usableSpace;
		final double usagePercent = ((double)usingSpace  / (double)totalSpace) * 100;
		if(usagePercent < basicOption.getMaximumCapacityPercent()){
			LOG.trace("usable disk size : " + usagePercent + "%");
			return true;
		}else{
			LOG.info("usable disk is over limit : " + usagePercent + "%");
			return false;
		}
	}
	
	void compactAndCleanIndex() throws IOException{
		writer.forceMergeDeletes();
		writer.deleteUnusedFiles();
		writer.commit();
	}
	
	String extractContentsFromFile(File f) throws IOException{
		final String contents = JSearch
				.extractContentsFromFile(f)
				.replaceAll("[\n\t\r ]+", " "); // erase tab, new line, return, space
		return contents;
	}

	/**
	 * handyfinder object indexing API
	 * 
	 * @param list
	 * @throws IOException
	 */
	int sizeOfindexDirectories(List<Directory> list) throws IOException {
		LOG.debug("Calculating size of files to be indexed");
		Size size = new Size();
		List<Directory> depensiveCopy = new ArrayList<>(list);
		for (Directory dir : depensiveCopy) {
			Path tmp = Paths.get(dir.getPathString());
			if (dir.isRecursively())
				size.add(sizeOfindexDirectory(tmp, true));
			else
				size.add(sizeOfindexDirectory(tmp, false));
		}
		LOG.debug("Finish to calculate");
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
	Size sizeOfindexDirectory(Path path, boolean recursively) {
		Size size = new Size();
		if (Files.isDirectory(path) ) {
			Path rootDirectory = path;
			if (recursively) {
				try {
					Files.walkFileTree(rootDirectory,
							new SimpleFileVisitor<Path>() {
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs){
							if (attrs.isRegularFile()) {
								size.add();
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					LOG.error(ExceptionUtils.getStackTrace(e));
				}
			} else {
				try {
				Files.walkFileTree(rootDirectory,
						EnumSet.noneOf(FileVisitOption.class), 1,
						new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs){
						if (attrs.isRegularFile()) {
							size.add();
						}
						return FileVisitResult.CONTINUE;
					}
				});
				} catch (IOException e) {
					LOG.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return size;
	}

	BooleanQuery getHandyFinderQuery(String fullString)
			throws QueryNodeException {
		final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		final Iterator<Directory> dirIter = basicOption.getDirectoryList().iterator();
		
		//Directory filter
		final BooleanQuery.Builder dirQueryBuilder = new BooleanQuery.Builder();
		while(dirIter.hasNext()){
			final Directory dir = dirIter.next();
			if(dir.isUsed())
				dirQueryBuilder.add(new WildcardQuery( 
						new Term("pathStringForQuery", getEscapedTermList(dir.getPathString(), false, true, Optional.empty()).get(0)))
						, Occur.SHOULD);
		}
		queryBuilder.add(dirQueryBuilder.build(), Occur.FILTER);
		
		//Mime filter
		final Iterator<String> notAllowedMimeIter = mimeOption.getNotAllowedMimeList().iterator();
		while(notAllowedMimeIter.hasNext()){
			final String mime = notAllowedMimeIter.next();
			queryBuilder.add(new TermQuery(new Term("mimeType", mime)), Occur.MUST_NOT);
		}
		
		//Path string query
		if(basicOption.getTargetMode().contains(TARGET_MODE.PATH)){
			BooleanQuery.Builder pathQueryBuilder = new BooleanQuery.Builder();
			for(String e : getEscapedTermList(fullString, true, true, Optional.empty())){
				Query query = new WildcardQuery(new Term("pathStringForQuery", e)); 
				pathQueryBuilder.add(query, Occur.SHOULD);
			}
			queryBuilder.add(pathQueryBuilder.build(), Occur.SHOULD);
		}

		//Content string query
		if(basicOption.getTargetMode().contains(TARGET_MODE.CONTENT)){
			BooleanQuery.Builder contentsQueryBuilder = new BooleanQuery.Builder();
			for(String e : getEscapedTermList(fullString, false, false, Optional.of(maxGramSize))){
				Query query = new TermQuery(new Term("contents", e));// parser.parse(e, "contents");
				if(basicOption.getKeywordMode().equals(KEYWORD_MODE.OR))
					contentsQueryBuilder.add(query, Occur.SHOULD);
				else
					contentsQueryBuilder.add(query, Occur.MUST);
			}
			queryBuilder.add(contentsQueryBuilder.build(), Occur.SHOULD);
		}
		return queryBuilder.build();
	}

	private void checkAndRecoverIndexWriter() {
		if (writer == null) {
			throw new IllegalStateException(
					"invalid state. After LuceneHandler.closeResources() or close(), you can't get instances.");
		}
		try {
			recoverIndexWriter();
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private void checkDirectoryReader() {
		if (reader == null) {
			throw new IllegalStateException(
					"invalid state. After LuceneHandler.closeResources() or close(), you can't search.");
		}
		try {
			updateIndexReader();
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private List<String> getEscapedTermList(String fullString, boolean prefixWildcard, boolean postfixWildcard, Optional<Integer> trimSize) {
		final List<String> list = new ArrayList<>();
		final String[] partialQuery = fullString.toLowerCase().replaceAll(" +", " ").split(" ");
		for (String element : partialQuery) {
			final Integer currectSize = trimSize.map(size -> {
				if(element.length() < size)
					return element.length(); 
				else 
					return size;
				}).orElse(element.length());
			list.add((prefixWildcard == true ? "*" : "")
					+ element.substring(0, currectSize)
					.replaceAll("(\\\\)", ((prefixWildcard || postfixWildcard) == true ? "$1$1" : "$1"))//replace single backslash with double backslash in wildcard query
					.replaceAll("(\\*)", ((prefixWildcard || postfixWildcard) == true ? Matcher.quoteReplacement("\\*") : "*"))//make wildcard charactor to be eascaped in widlcard query 
					+ (postfixWildcard == true ? "*" : ""));
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
		}
		return frequencies;
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
		return getTermFrequenciesFromContents(reader, docId);
	}
}
