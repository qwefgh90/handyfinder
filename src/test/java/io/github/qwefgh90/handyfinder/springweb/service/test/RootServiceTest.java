package io.github.qwefgh90.handyfinder.springweb.service.test;

import static org.junit.Assert.assertTrue;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.config.RootContext;
import io.github.qwefgh90.handyfinder.springweb.config.ServletContextTest;
import io.github.qwefgh90.handyfinder.springweb.repository.MetaRespository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
// ApplicationContext will be loaded from
// "classpath:/com/example/OrderServiceTest-context.xml"
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class RootServiceTest {
	private final static Logger LOG = LoggerFactory.getLogger(RootServiceTest.class);
	@Autowired
	MetaRespository indexProperty;
	Directory dir1;
	Directory dir2;
	
	@Before
	public void setup() throws SQLException, IOException {
		indexProperty.deleteDirectories();
		dir1 = new Directory();
		dir1.setPathString("path1");
		dir1.setUsed(true);
		dir1.setRecursively(false);
		dir2 = new Directory();
		dir2.setPathString("path2");
		dir2.setUsed(true);
		dir2.setRecursively(false);
	}
	
	@After
	public void clean() throws SQLException{
		indexProperty.deleteDirectories();
	}

	@Test
	public void directoryTest1() throws SQLException {
		Directory dir = new Directory();
		dir.setPathString("path0");
		dir.setUsed(true);
		dir.setRecursively(false);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		indexProperty.saveOne(dir);
		Assert.assertThat(indexProperty.selectDirectory().size(),Matchers.is(1));
		indexProperty.deleteDirectories();
		Assert.assertThat(indexProperty.selectDirectory().size(),Matchers.is(0));
	}

	@Test
	public void directoryTest2() throws SQLException {
		List<Directory> list = new ArrayList<>();
		list.add(dir1);
		list.add(dir2);
		indexProperty.save(list);
		Assert.assertThat(indexProperty.selectDirectory().size(),Matchers.is(2));

		indexProperty.deleteDirectories();
		Assert.assertThat(indexProperty.selectDirectory().size(),Matchers.is(0));
	}
}
