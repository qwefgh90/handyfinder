package handyfinder;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.qwefgh90.io.handyfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.repository.GlobalAppData.GlobalAppDataView;

public class AppDataTest {

	Directory d;

	@Before
	public void setup() {
		d = new Directory();
		d.setPathString("hello path");
		d.setRecursively(false);
		d.setUsed(false);
	}

	@After
	public void clean() throws IOException {
		GlobalAppDataView.clearAppDataFile();
	}

	@Test
	public void test(){
		GlobalAppDataView.addDirectory(d);
		assertTrue(GlobalAppDataView.limitOfSearch()==100);
		Iterator<Directory> iter = GlobalAppDataView.directoryList();
		Directory tmp = iter.next();
		assertTrue(tmp.getPathString().equals("hello path"));
		assertTrue(false==tmp.isUsed());
		assertFalse(iter.hasNext());
		
		d.setUsed(true);
		GlobalAppDataView.setDirectory(d);
		iter = GlobalAppDataView.directoryList();
		tmp = iter.next();
		assertTrue(true==tmp.isUsed());
		assertFalse(iter.hasNext());
		
		
		GlobalAppDataView.updateAppDataFile();
		
		
	}
}
