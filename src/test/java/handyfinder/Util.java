package handyfinder;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class Util {

	@Test
	public void path(){
		Path path = Paths.get("Ready to Run");
		System.out.println(path.toString());
	
		
		String test = "h     e    l    werwer    asdfa sdafasdf asdf asdf asdf asdf \n asdf asdf";
		System.out.println(test.replaceAll(" +", " "));
	}
	
	@Test public void open() throws URISyntaxException{

		if (Desktop.isDesktopSupported()) {
			try {
				URL url = getClass().getResource("/");
				Desktop.getDesktop().open(new File(url.toURI()));
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
}
