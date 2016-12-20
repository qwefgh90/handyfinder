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

import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.lucene.BasicOption;
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
public class BasicOptionTest {

	@Autowired
	BasicOption basicOption;

	private Directory testDir;

	@Before
	public void setup() throws IOException {
		basicOption.deleteAppDataFromDisk();
		basicOption.deleteDirectories();
		testDir = new Directory();
		testDir.setPathString("hello path");
		testDir.setRecursively(false);
		testDir.setUsed(false);
	}

	@After
	public void clean() throws IOException {
		basicOption.deleteAppDataFromDisk();
		basicOption.deleteDirectories();
	}

	@Test
	public void methodTest() {
		basicOption.addDirectory(testDir);
		Assert.assertThat(basicOption.getDirectoryList().size(),
				Matchers.is(1));
		
		basicOption.deleteDirectories();
		Assert.assertThat(basicOption.getDirectoryList().size(),
				Matchers.is(0));

		basicOption.addDirectory(testDir);
		basicOption.deleteDirectory(testDir);
		Assert.assertThat(basicOption.getDirectoryList().size(),
				Matchers.is(0));

		basicOption.addDirectory(testDir);
		testDir.setPathString("modified");
		basicOption.setDirectory(testDir);
		Assert.assertThat(basicOption.getDirectoryList().size(),
				Matchers.is(1));
		Assert.assertThat(basicOption.getDirectoryList().get(0).getPathString()
				,Matchers.is("modified"));
		
		basicOption.setMaximumDocumentMBSize(1);
		Assert.assertThat(1,
				Matchers.is(basicOption.getMaximumDocumentMBSize()));
		
		basicOption.setLimitCountOfResult(1);
		Assert.assertThat(1,
				Matchers.is(basicOption.getLimitCountOfResult()));
	}

}
