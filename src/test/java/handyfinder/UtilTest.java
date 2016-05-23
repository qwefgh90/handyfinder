package handyfinder;

import static org.junit.Assert.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilTest {

	private final static Logger LOG = LoggerFactory.getLogger(UtilTest.class);
	@Test
	public void path(){
		Path path = Paths.get("Ready to Run");
		assertTrue(path.toString().equals("Ready to Run"));
		
		String test = "h     e    l    werwer    asdfa sdafasdf asdf asdf asdf asdf \n asdf asdf";
		System.out.println(test.replaceAll(" +", " "));
	}
	
	@Test 
	public void open() throws URISyntaxException{

		if (Desktop.isDesktopSupported()) {
			try {
				URL url = getClass().getResource("/");
				Desktop.getDesktop().open(new File(url.toURI()));
				URL url2 = getClass().getResource("/index-test-files/text.txt");
				Desktop.getDesktop().open(new File(url2.toURI()));
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}

	@Test
	public void printCurrentClassPath(){

        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
        	LOG.debug(url.getFile());
        }
	}
}
