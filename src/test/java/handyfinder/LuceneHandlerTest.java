package handyfinder;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.RootContext;
import com.qwefgh90.io.handyfinder.springweb.ServletContextTest;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler.IndexException;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
// ApplicationContext will be loaded from
// "classpath:/com/example/OrderServiceTest-context.xml"
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class })
public class LuceneHandlerTest {
	private final static Logger LOG = LoggerFactory.getLogger(LuceneHandlerTest.class);
	@Autowired
	CommandInvoker invoker;
	
	LuceneHandler handler;
	LuceneHandler handler2;
	static {
		try {
			AppStartupConfig.initializeEnv(new String[]{"--test-mode","--no-gui"});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Before
	public void setup() throws IOException {
		handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invoker);
		handler2 = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invoker);
		assertTrue(handler == handler2);
		handler.deleteAllIndexesFromFileSystem();
	}

	@After
	public void clean() throws IOException {
		LuceneHandler.closeResources();

		try {
			handler.indexDirectory(AppStartupConfig.pathForAppdata.resolve("notexists"), true);
		} catch (RuntimeException e) { // after close, throw RuntimeException
			assertTrue(true);
			return;
		}
		assertTrue(false);
	}

	@Test
	public void writeTest() throws IOException, org.apache.lucene.queryparser.classic.ParseException,
			InvalidTokenOffsetsException, QueryNodeException, IndexException {
		handler.indexDirectory(AppStartupConfig.deployedPath.resolve("index-test-files"), true);

		TopDocs docs = handler.search("고언어 자바");
		for (int i = 0; i < docs.scoreDocs.length; i++) {
			Document doc = handler.getDocument(docs.scoreDocs[i].doc);

//			String info = "[" + docs.scoreDocs[i].score + "]" + doc.get("pathString") + " : \n" + doc.get("contents")
//					.substring(0, doc.get("contents").length() > 100 ? 100 : doc.get("contents").length()) + "\n";
//			log.info(info);

			Explanation exp = handler.getExplanation(docs.scoreDocs[i].doc, "고언어 자바");
			LOG.info(exp.toString());

			LOG.info(handler.highlight(docs.scoreDocs[i].doc, "고언어 자바"));
		}
		assertTrue(docs.scoreDocs.length == 7);

		docs = handler.search("HTTP");
		for (int i = 0; i < docs.scoreDocs.length; i++) {
			Document doc = handler.getDocument(docs.scoreDocs[i].doc);

//			String info = "[" + docs.scoreDocs[i].score + "]" + doc.get("pathString") + " : \n" + doc.get("contents")
//					.substring(0, doc.get("contents").length() > 100 ? 100 : doc.get("contents").length()) + "\n";
//			log.info(info);

			Explanation exp = handler.getExplanation(docs.scoreDocs[i].doc, "HTTP");
			LOG.info(exp.toString());

			LOG.info(handler.highlight(docs.scoreDocs[i].doc, "HTTP"));
		}
		assertTrue(docs.scoreDocs.length == 1);

		docs = handler.search("부트로더 Proto");
		for (int i = 0; i < docs.scoreDocs.length; i++) {
			Document doc = handler.getDocument(docs.scoreDocs[i].doc);

			String info = "[" + docs.scoreDocs[i].score + "]" + doc.get("pathString") + " : \n" + doc.get("contents")
					.substring(0, doc.get("contents").length() > 100 ? 100 : doc.get("contents").length()) + "\n";
			LOG.info(info);

			Explanation exp = handler.getExplanation(docs.scoreDocs[i].doc, "부트로더 Proto");
			LOG.info(exp.toString());

			LOG.info(handler.highlight(docs.scoreDocs[i].doc, "부트로더 Proto"));
		}
		assertTrue(docs.scoreDocs.length == 2);

	}

	@Test
	public void fileURITest() throws IOException, org.apache.lucene.queryparser.classic.ParseException,
			QueryNodeException, InvalidTokenOffsetsException, IndexException {
		handler.indexDirectory(AppStartupConfig.deployedPath.resolve("index-test-files"), true);

		TopDocs docs = handler.search("부트로더");
		for (int i = 0; i < docs.scoreDocs.length; i++) {
			Document doc = handler.getDocument(docs.scoreDocs[i].doc);

//			String info = "[" + docs.scoreDocs[i].score + "]" + doc.get("pathString") + " : \n" + doc.get("contents")
//					.substring(0, doc.get("contents").length() > 100 ? 100 : doc.get("contents").length()) + "\n";
//			log.info(info);

			Explanation exp = handler.getExplanation(docs.scoreDocs[i].doc, "/depth/ homec/choe");
			LOG.info(exp.toString());

			LOG.info(handler.highlight(docs.scoreDocs[i].doc, "/depth/ homec/choe"));
		}
		assertTrue(docs.scoreDocs.length > 0);
	}
}
