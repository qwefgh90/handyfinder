package io.github.qwefgh90.handyfinder.lucene;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import io.github.qwefgh90.handyfinder.lucene.ILuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.config.AppDataConfig;
import io.github.qwefgh90.handyfinder.springweb.config.RootContext;
import io.github.qwefgh90.handyfinder.springweb.config.RootWebSocketConfig;
import io.github.qwefgh90.handyfinder.springweb.config.ServletContextTest;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class,
		AppDataConfig.class, RootWebSocketConfig.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class LuceneHandlerBasicOptionViewTest {

	@Autowired
	ILuceneHandlerBasicOptionView globalAppDataView;

	private Directory testDir;

	@Before
	public void setup() throws IOException {
		globalAppDataView.deleteAppDataFromDisk();
		globalAppDataView.deleteDirectories();
		testDir = new Directory();
		testDir.setPathString("hello path");
		testDir.setRecursively(false);
		testDir.setUsed(false);
	}

	@After
	public void clean() throws IOException {
		globalAppDataView.deleteAppDataFromDisk();
		globalAppDataView.deleteDirectories();
	}

	@Test
	public void methodTest() {
		globalAppDataView.addDirectory(testDir);
		Assert.assertThat(globalAppDataView.getDirectoryList().size(),
				Matchers.is(1));
		
		globalAppDataView.deleteDirectories();
		Assert.assertThat(globalAppDataView.getDirectoryList().size(),
				Matchers.is(0));

		globalAppDataView.addDirectory(testDir);
		globalAppDataView.deleteDirectory(testDir);
		Assert.assertThat(globalAppDataView.getDirectoryList().size(),
				Matchers.is(0));

		globalAppDataView.addDirectory(testDir);
		testDir.setPathString("modified");
		globalAppDataView.setDirectory(testDir);
		Assert.assertThat(globalAppDataView.getDirectoryList().size(),
				Matchers.is(1));
		Assert.assertThat(globalAppDataView.getDirectoryList().get(0).getPathString()
				,Matchers.is("modified"));
		
		globalAppDataView.setMaximumDocumentMBSize(1);
		Assert.assertThat(1,
				Matchers.is(globalAppDataView.getMaximumDocumentMBSize()));
		
		globalAppDataView.setLimitCountOfResult(1);
		Assert.assertThat(1,
				Matchers.is(globalAppDataView.getLimitCountOfResult()));
	}

}
