package io.github.qwefgh90.handyfinder.lucene;

import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import io.github.qwefgh90.handyfinder.gui.AppStartup;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.TARGET_MODE;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.config.AppDataConfig;
import io.github.qwefgh90.handyfinder.springweb.config.RootContext;
import io.github.qwefgh90.handyfinder.springweb.config.ServletContextTest;
import io.github.qwefgh90.handyfinder.springweb.repository.MetaRespository;
import io.github.qwefgh90.handyfinder.springweb.websocket.MessageController;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class,
		AppDataConfig.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)

public class LuceneHandlerTest {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerTest.class);
	@Autowired
	MessageController invoker;

	@Autowired
	MetaRespository metaRepository;
	
	@Autowired
	MimeOption mimeOption;
	@Autowired
	BasicOption basicOption;

	LuceneHandler handler;
	LuceneHandler handler2;
	static {
		try {
			AppStartup.parseArguments(new String[] { "--no-gui" });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (org.apache.commons.cli.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//test files
	Path temptxt;
	Path temp2txt;
	
	//indexed file
	Path testFilesPath;
	List<Directory> indexDirList;
	@Before
	public void setup() throws IOException {
		mimeOption.initGlobTrue();
		basicOption.setLimitCountOfResult(100);
		basicOption.setMaximumDocumentMBSize(100);
		basicOption.setTargetMode(EnumSet.of(TARGET_MODE.PATH, TARGET_MODE.CONTENT));
		basicOption.setKeywordMode(KEYWORD_MODE.OR.toString());
		handler = LuceneHandler.getInstance(AppStartup.pathForIndex,
				invoker, basicOption, mimeOption);
		handler.deleteAllIndexesFromFileSystem();

		testFilesPath = AppStartup.deployedPath.resolve("index-test-files");
		
		Directory testFileiDir = new Directory();
		testFileiDir.setRecursively(true);
		testFileiDir.setUsed(true);
		testFileiDir.setPathString(testFilesPath.toAbsolutePath().toString());;
		
		indexDirList = new ArrayList<>();
		indexDirList.add(testFileiDir);
		indexDirList.forEach(dir -> {
			basicOption.addDirectory(dir);
		});
		
		//create new files
		temp2txt = testFilesPath.resolve("temp2.txt");
		temptxt = testFilesPath.resolve("temp.txt");

		try (BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(temptxt.toFile()))) {
			os.write("안녕?".getBytes());
		}
		try (BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(temp2txt.toFile()))) {
			os.write("안녕?".getBytes());
		}
	}
	
	@After
	public void clean() throws IOException {
		if(Files.exists(temptxt))
			Files.delete(temptxt);
		if(Files.exists(temp2txt))
			Files.delete(temp2txt);
		mimeOption.initGlobTrue();
		LuceneHandler.closeResources();
	}
	
	@Test
	public void searchTest() throws IOException,
			org.apache.lucene.queryparser.classic.ParseException,
			InvalidTokenOffsetsException, QueryNodeException, InterruptedException, ExecutionException {
		handler.restartIndexAsync(basicOption.getDirectoryList()).get();
		List<ScoreDoc> docs = handler.search("javageek", 0);
		Assert.assertThat(docs.size(), Matchers.is(5));
	}

	@Test
	public void factoryMethodTest() {
		handler2 = LuceneHandler.getInstance(AppStartup.pathForIndex,
				invoker, basicOption, mimeOption);
		assertTrue(handler == handler2);
	}

	
	
	@Test
	public void searchTestWithMime() throws IOException,
			org.apache.lucene.queryparser.classic.ParseException,
			InvalidTokenOffsetsException, QueryNodeException, InterruptedException, ExecutionException {
		handler.restartIndexAsync(basicOption.getDirectoryList()).get();

		mimeOption.setGlob("*.txt", false);
		
		List<ScoreDoc> docs = handler.search("javageek", 0);
		Assert.assertThat(docs.size(), Matchers.is(0));
	}

	@Test
	public void searchTest2() throws IOException,
			org.apache.lucene.queryparser.classic.ParseException,
			InvalidTokenOffsetsException, QueryNodeException, InterruptedException, ExecutionException {
		handler.restartIndexAsync(basicOption.getDirectoryList()).get();

		List<ScoreDoc> docs = handler.search("PageBase", 0);
		Assert.assertThat(docs.size(), Matchers.is(1));
	}
	
	@Test
	public void deleteAndUpdateIndexTest() throws IOException, InterruptedException, ExecutionException {
		handler.restartIndexAsync(basicOption.getDirectoryList()).get();
		StringBuilder sb = new StringBuilder();
		List<Document> listBefore = handler.getDocumentList();
		listBefore.forEach(doc -> sb.append(doc.get("title") + ", "));
		LOG.info(sb.toString());

		Files.delete(temp2txt);
		temptxt.toFile().delete();
		
		handler.state.ready();
		handler.updateIndexedDocuments(indexDirList);
		int countAfter = handler.getDocumentCount();
		
		sb.setLength(0);
		List<Document> listAfter = handler.getDocumentList();
		listAfter.forEach(doc -> sb.append(doc.get("title") + ", "));
		LOG.info(sb.toString());
		
		Assert.assertThat(listAfter.size(), Matchers.is(listBefore.size()-2));
		Assert.assertThat(listAfter.size(), Matchers.is(countAfter));

		handler.updateIndexedDocuments(Collections.emptyList());
		int clearCount = handler.getDocumentCount();
		Assert.assertThat(clearCount, Matchers.is(0));
		LOG.info("clear count : " + clearCount);
	}

	@Test
	public void fileURITest()
			throws org.apache.lucene.queryparser.classic.ParseException,
			QueryNodeException, InvalidTokenOffsetsException,
			IOException, InterruptedException, ExecutionException {
		handler.restartIndexAsync(basicOption.getDirectoryList()).get();

		List<ScoreDoc> docs = handler.search("http",0);
		Assert.assertThat(docs.size(), Matchers.is(1));
	}
}
