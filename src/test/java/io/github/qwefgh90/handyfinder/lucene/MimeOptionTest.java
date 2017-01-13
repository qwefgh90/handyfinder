package io.github.qwefgh90.handyfinder.lucene;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.MimeOption;
import io.github.qwefgh90.handyfinder.lucene.MimeOption.MimeXmlObjectFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.SystemUtils;
import org.apache.tika.mime.MimeTypes;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class MimeOptionTest {
	private final static Logger LOG = LoggerFactory.getLogger(MimeOptionTest.class);
	URL xmlUrl;
	String xmlUrlStr;
	Pattern pat;
	Matcher matcher;
	String jarPathString;
	String resourceName;
	URL afterXmlUrl;

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
		xmlUrl = MimeTypes.class.getResource("tika-mimetypes.xml");
		xmlUrlStr = xmlUrl.toString();
		pat = Pattern.compile("jar:file:(.+)!(.+)");
		matcher = pat.matcher(xmlUrlStr);
		jarPathString = null;
		resourceName = null;

		if (matcher.matches()) {
			jarPathString = matcher.group(1);
			if(SystemUtils.IS_OS_WINDOWS)
				jarPathString = jarPathString.substring(1);
			resourceName = matcher.group(2);
			File xmlDestFile = new File(getClass().getResource("/").toURI());
			AppStartupConfig.copyFileInJar(jarPathString, resourceName, xmlDestFile, (file, entry) -> !file.exists());
			LOG.info(getClass().getResource("/tika-mimetypes.xml").toString());
			LOG.info(jarPathString);
			LOG.info(resourceName);
		}
		afterXmlUrl = MimeTypes.class.getResource("/tika-mimetypes.xml");
		obj = MimeXmlObjectFactory.getInstanceFromXml(Paths.get(afterXmlUrl.toURI()).toAbsolutePath().toString()
				,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
	}

	MimeOption obj;

	@Test
	public void parseTest() throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
		obj.setGlob("*.hwp", false);
		obj.setGlob("*.xml", false);
		assertFalse(obj.isAllowMime("application/x-hwp"));

		MimeOption obj2 = MimeXmlObjectFactory
				.getInstanceFromXml(Paths.get(afterXmlUrl.toURI()).toAbsolutePath().toString()
						,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
		assertTrue(obj == obj2);
		
		Assert.assertThat(obj.getGlobSet("application/vnd.ms-excel").size(), Matchers.is(8));
	}

	@After
	public void clean() throws URISyntaxException, FileNotFoundException, IOException {
		File f = new File(afterXmlUrl.toURI());
		f.delete();
		obj.initGlobTrue();
		obj.updateGlobPropertiesFile();
	}

}
