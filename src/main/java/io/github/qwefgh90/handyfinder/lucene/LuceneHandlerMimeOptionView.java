package io.github.qwefgh90.handyfinder.lucene;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;

/**
 * instance is created by <b>TikaMimeXmlObjectFactory</b>
 * 
 * @author choechangwon
 *
 */
public final class LuceneHandlerMimeOptionView implements
		ILuceneHandlerMimeOptionView {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerMimeOptionView.class);

	private LuceneHandlerMimeOptionView() {
	}

	private Map<String, Set<String>> mimeToGlobListMap = new HashMap<>();
	private Map<String, Boolean> globMap = new TreeMap<>();

	public Map<String, Boolean> getImmutableGlobMap() {
		return Collections.unmodifiableMap(globMap);
	}

	public void setGlob(String glob, boolean b) {
		if (!globMap.containsKey(glob)) {
			LOG.warn("key(glob) is not exists.");
			return;
		}
		Boolean value = Boolean.valueOf(b);
		globMap.put(glob, value);
	}

	public void initGlobTrue() {
		Iterator<String> iter = globMap.keySet().iterator();
		while (iter.hasNext()) {
			globMap.put(iter.next(), Boolean.TRUE);
		}
	}

	@Override
	public boolean isAllowMime(String mime) {
		Iterator<String> iter = getGlobIterator(mime);
		if (iter == null)
			return true;
		while (iter.hasNext()) {
			Boolean used = getGlobUsing(iter.next());
			if (used == false)
				return false;
		}
		return true;
	}

	/**
	 * update <b>glob properties file</b> in file system.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void updateGlobPropertiesFile() throws FileNotFoundException,
			IOException {
		Properties properties = new Properties();
		Iterator<String> iter = getGlobIterator();
		while (iter.hasNext()) {
			String glob = iter.next();
			Boolean b = globMap.get(glob);
			properties.setProperty(glob, Boolean.toString(b.booleanValue()));
		}
		if (Files.exists(propertiesPath)) {
			Files.delete(propertiesPath);
		}

		try (BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(propertiesPath.toFile()))) {
			properties.store(bos, String.valueOf(System.currentTimeMillis()));
		}
	}

	Boolean getGlobUsing(String glob) {
		return globMap.get(glob);
	}

	int getCountofGlob() {
		return globMap.size();
	}

	/**
	 * get glob count
	 * 
	 * @param mime
	 * @return -1 if not exist.
	 */
	@Override
	public Set<String> getGlobSet(String mime) {
		return Collections.unmodifiableSet(mimeToGlobListMap.get(mime));
	}

	Iterator<String> getGlobIterator(String mime) {
		Set<String> str = mimeToGlobListMap.get(mime);
		if (str == null)
			return null;
		return str.iterator();
	}

	Iterator<String> getGlobIterator() {
		return globMap.keySet().iterator();
	}

	/**
	 * if already exist, no change in globMap
	 * 
	 * @param mimetype
	 * @param glob
	 */
	void addGlobType(String mimetype, String glob) {
		if (!globMap.containsKey(glob))
			globMap.put(glob, Boolean.TRUE); // if not exist, put True into map

		if (!mimeToGlobListMap.keySet().contains(mimetype)) {
			Set<String> values = new TreeSet<String>();
			values.add(glob);
			mimeToGlobListMap.put(mimetype, values);
		} else {
			mimeToGlobListMap.get(mimetype).add(glob);
		}
	}

	private Path propertiesPath;

	/**
	 * factory class of <b>TikaMimeXmlObject</b>
	 * 
	 * @author choechangwon
	 *
	 */
	public static class TikaMimeXmlObjectFactory {
		private static Map<String, LuceneHandlerMimeOptionView> container = new HashMap<String, LuceneHandlerMimeOptionView>();

		private TikaMimeXmlObjectFactory() {
		}

		public static LuceneHandlerMimeOptionView getInstanceFromXml(
				String xmlPath, Path propertiesPath,
				Path customTikaGlobPropertiesPath)
				throws ParserConfigurationException, SAXException, IOException {
			if (container.keySet().contains(xmlPath)) {
				return container.get(xmlPath);
			}
			LuceneHandlerMimeOptionView obj = new LuceneHandlerMimeOptionView();
			obj.propertiesPath = propertiesPath;

			Properties p = new Properties();
			// load from property file
			if (Files.exists(propertiesPath)) {
				try (BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(propertiesPath.toFile()))) {
					p.load(bis);
					Iterator<Object> iter = p.keySet().iterator();
					while (iter.hasNext()) {
						String key = (String) iter.next();
						obj.globMap.put(key,
								Boolean.valueOf(p.getProperty(key)));
					}
				} catch (IOException e) {
					LOG.info(e.toString());
				}
			}

			// after load default add... if exist not update
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			container.put(xmlPath, obj);
			saxParser.parse(xmlPath, new TikaMimeTypesSaxHandler(obj));

			// after load default, add custom
			addCustomMimeAndGlob(obj, customTikaGlobPropertiesPath);
			return obj;
		}

		private static void addCustomMimeAndGlob(
				LuceneHandlerMimeOptionView obj,
				Path customTikaGlobPropertiesPath) {
			try {
				Properties p = new Properties();
				InputStream is = new FileInputStream(
						customTikaGlobPropertiesPath.toFile());
				p.load(is);
				for (Map.Entry<Object, Object> entry : p.entrySet()) {
					obj.addGlobType((String) entry.getKey(),
							(String) entry.getValue());
				}
			} catch (FileNotFoundException e) {
				LOG.info(e.toString());
			} catch (IOException e) {
				LOG.info(e.toString());
			}

		}

	}
}
