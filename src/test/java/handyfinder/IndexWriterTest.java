package handyfinder;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.RootContext;
import com.qwefgh90.io.handyfinder.springweb.ServletContextTest;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler;
import com.qwefgh90.io.jsearch.JSearch.ParseException;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
// ApplicationContext will be loaded from
// "classpath:/com/example/OrderServiceTest-context.xml"
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class })
public class IndexWriterTest {
	Log log = LogFactory.getLog(IndexWriterTest.class);

	LuceneHandler handler;
	Path indexStoredPath;
	static {
		try {
			AppStartupConfig.pathForAppdata = Paths.get(new ClassPathResource("").getFile().getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Before
	public void setup() throws IOException {
		indexStoredPath = Paths.get(new ClassPathResource("").getFile().getAbsolutePath()).resolve("index_");
		handler = LuceneHandler.getInstance(indexStoredPath);
		handler.deleteAllIndexesFromFileSystem();
	}

	@Test
	public void writeTest() throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException {
		handler.indexDirectory(AppStartupConfig.pathForAppdata.resolve("index-test-files"), true);

		TopDocs docs = handler.search("고언 자바");
		for (int i = 0; i < docs.scoreDocs.length; i++) {
			Document doc = handler.getDocument(docs.scoreDocs[i].doc);
			Map<String, Integer> freqMap = handler.getTermFrequencies(docs.scoreDocs[i].doc);
			Iterator<Entry<String,Integer>> iter = freqMap.entrySet().iterator();
			StringBuilder sb = new StringBuilder();
			while(iter.hasNext()){
				Entry<String,Integer> entry = iter.next();
				sb.append("("+entry.getKey() + ":" + entry.getValue() + ") ");
			}
			String info = "["+docs.scoreDocs[i].score+"]"+doc.get("pathString") + " : " + doc.get("contents") +sb.toString() + "\n" ;
			log.info(info);
		
		}

		LuceneHandler.closeResources();

		try {
			handler.indexDirectory(AppStartupConfig.pathForAppdata.resolve("index-test-files"), true);
		} catch (RuntimeException e) {
			assertTrue(true);
			return;
		}
		assertTrue(false);

	}

}
