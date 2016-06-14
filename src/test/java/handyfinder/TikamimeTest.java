package handyfinder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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
import org.apache.commons.lang.SystemUtils;
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
import com.qwefgh90.io.jsearch.FileExtension;

import io.github.qwefgh90.handyfinder.lucene.TikaMimeXmlObject;
import io.github.qwefgh90.handyfinder.lucene.TikaMimeXmlObject.TikaMimeXmlObjectFactory;

public class TikamimeTest {
	private final static Logger LOG = LoggerFactory.getLogger(TikamimeTest.class);
	URL url;
	String urlStr;
	Pattern pat;
	Matcher matcher;
	String jarPath;
	String resourceName;
	URL afterUrl;

	static {
		try {
			AppStartupConfig.parseArguments(new String[] {});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Before
	public void setup() throws URISyntaxException, IOException, ParserConfigurationException, SAXException {
		// Patterns
		url = MimeTypes.class.getResource("tika-mimetypes.xml");
		urlStr = url.toString();
		pat = Pattern.compile("jar:file:(.+)!(.+)");
		matcher = pat.matcher(urlStr);
		jarPath = null;
		resourceName = null;

		if (matcher.matches()) {
			jarPath = matcher.group(1);
			if(SystemUtils.IS_OS_WINDOWS)
				jarPath = jarPath.substring(1);
			resourceName = matcher.group(2);
			System.out.println(jarPath);
			System.out.println(resourceName);
			AppStartupConfig.copyFileInJar(jarPath, resourceName, new File(getClass().getResource("/").toURI()), true);
			LOG.info(getClass().getResource("/tika-mimetypes.xml").toString());
		}
		afterUrl = MimeTypes.class.getResource("/tika-mimetypes.xml");
		obj = TikaMimeXmlObjectFactory.getInstanceFromXml(Paths.get(afterUrl.toURI()).toAbsolutePath().toString()
				,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
		LOG.info("*.hwp:" + obj.getGlobUsing("*.hwp"));
		LOG.info("*.xml:" + obj.getGlobUsing("*.xml"));
		obj.setGlob("*.hwp", false);
		obj.setGlob("*.xml", false);
		
		assertFalse(obj.isAllowMime("application/x-hwp"));
		MediaType mt = FileExtension.getContentType(new File(afterUrl.toURI()), "tika-mimetypes.xml");
		LOG.info(mt.toString());
	}

	TikaMimeXmlObject obj;

	@Test
	public void parseTest() throws ParserConfigurationException, SAXException, IOException, URISyntaxException {

		LOG.info(String.valueOf(obj.getCountofGlob("application/vnd.ms-excel")));
		assertTrue(obj.getGlobIterator().hasNext());
		assertTrue(obj.getCountofGlob("application/vnd.ms-excel") == 8);
		LOG.info(String.valueOf(obj.getCountofGlob()));
		LOG.info(String.valueOf(obj.getGlobIterator("application/vnd.ms-excel").next().toString()));
		TikaMimeXmlObject obj2 = TikaMimeXmlObjectFactory
				.getInstanceFromXml(Paths.get(afterUrl.toURI()).toAbsolutePath().toString()
						,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
		assertTrue(obj == obj2);
		obj.updateGlobPropertiesFile();
	}

	@After
	public void clean() throws URISyntaxException {
		File f = new File(afterUrl.toURI());
		f.delete();
	}

}
