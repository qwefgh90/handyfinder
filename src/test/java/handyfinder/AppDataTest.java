package handyfinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.qwefgh90.io.handyfinder.gui.GlobalAppDataView;
import com.qwefgh90.io.handyfinder.springweb.AppDataConfig;
import com.qwefgh90.io.handyfinder.springweb.RootContext;
import com.qwefgh90.io.handyfinder.springweb.ServletContextTest;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class,
		AppDataConfig.class })
public class AppDataTest {
	
	@Autowired
	GlobalAppDataView globalAppDataView;

	Directory d;

	@Before
	public void setup() throws IOException {
		globalAppDataView.deleteAppDataFromDisk();
		globalAppDataView.deleteDirectories();
		d = new Directory();
		d.setPathString("hello path");
		d.setRecursively(false);
		d.setUsed(false);
	}

	@After
	public void clean() throws IOException {
		globalAppDataView.deleteAppDataFromDisk();
		globalAppDataView.deleteDirectories();
	}

	@Test
	public void test() {
		globalAppDataView.addDirectory(d);
		assertTrue(globalAppDataView.getLimitOfSearch() == 100);
		Iterator<Directory> iter = globalAppDataView.getDirectoryList().iterator();
		Directory tmp = iter.next();
		assertTrue(tmp.getPathString().equals("hello path"));
		assertTrue(false == tmp.isUsed());
		assertFalse(iter.hasNext());

		d.setUsed(true);
		globalAppDataView.setDirectory(d);
		iter = globalAppDataView.getDirectoryList().iterator();
		tmp = iter.next();
		assertTrue(true == tmp.isUsed());
		assertFalse(iter.hasNext());

		globalAppDataView.writeAppDataToDisk();

	}
}
