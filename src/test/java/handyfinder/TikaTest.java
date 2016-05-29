package handyfinder;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.sax.TikaMimeXmlObject;
import com.qwefgh90.io.handyfinder.sax.TikaMimeXmlObject.TikaMimeXmlObjectFactory;

public class TikaTest {
	private final static Logger LOG = LoggerFactory.getLogger(TikaTest.class);
	URL url;
	String urlStr;
	Pattern pat;
	Matcher matcher;
	String jarPath;
	String resourceName;
	URL afterUrl;

	static{
		try {
			AppStartupConfig.initializeEnv(new String[]{});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Before
	public void setup() throws URISyntaxException, IOException {
		// Patterns
		url = MimeTypes.class.getResource("tika-mimetypes.xml");
		urlStr = url.toString();
		pat = Pattern.compile("jar:file:(.+)!(.+)");
		matcher = pat.matcher(urlStr);
		jarPath = null;
		resourceName = null;

		if (matcher.matches()) {
			jarPath = matcher.group(1);
			resourceName = matcher.group(2);
			System.out.println(matcher.group(1));
			System.out.println(matcher.group(2));
			AppStartupConfig.copyFileInJar(jarPath, resourceName, new File(getClass().getResource("/").toURI()), true);
			LOG.info(getClass().getResource("/tika-mimetypes.xml").toString());
		}
		afterUrl  = MimeTypes.class.getResource("/tika-mimetypes.xml");
	}

	@Test
	public void parseTest() throws ParserConfigurationException, SAXException, IOException, URISyntaxException{
		TikaMimeXmlObject obj = TikaMimeXmlObjectFactory.createInstanceFromXml(Paths.get(afterUrl.toURI()).toAbsolutePath().toString());

		LOG.info(String.valueOf(obj.getCountofGlob("application/vnd.ms-excel")));
		assertTrue(obj.getGlobIterator().hasNext());
		assertTrue(obj.getCountofGlob("application/vnd.ms-excel")==8);
		LOG.info(String.valueOf(obj.getCountofGlob()));
		LOG.info(String.valueOf(obj.getGlobIterator("application/vnd.ms-excel").next().toString()));
		TikaMimeXmlObject obj2 = TikaMimeXmlObjectFactory.createInstanceFromXml(Paths.get(afterUrl.toURI()).toAbsolutePath().toString());
		assertTrue(obj == obj2);
		obj.setGlob("*.xml", false);
		
	}
	
	@After
	public void clean() throws URISyntaxException{
		File f = new File(afterUrl.toURI());
		f.delete();
	}

	@Test
	public void test() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
			URISyntaxException, IOException {
		LOG.info(afterUrl.toString());
		assertTrue(afterUrl.toString().startsWith("file:"));

		Set<MediaType> s = MimeTypes.getDefaultMimeTypes().getMediaTypeRegistry().getTypes();
		LOG.info("size: " + s.size());
		assertTrue(s.size() > 0);
		Iterator<MediaType> iter = s.iterator();
		while (iter.hasNext()) {
			MediaType mt = iter.next();

		//	LOG.info(ToStringBuilder.reflectionToString(mt) + ":" + mt.getParameters().values().toString());
		}
	}
}
