package com.qwefgh90.io.handyfinder.tikamime;

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

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.lucene.ILuceneHandlerMimeOption;

/**
 * instance is created by <b>TikaMimeXmlObjectFactory</b>
 * 
 * @author choechangwon
 *
 */
public final class TikaMimeXmlObject implements ILuceneHandlerMimeOption {
	private final static Logger LOG = LoggerFactory.getLogger(TikaMimeXmlObject.class);

	private TikaMimeXmlObject() {
	}

	private Map<String, Set<String>> mimeToGlobListMap = new HashMap<>();
	private Map<String, Boolean> globMap = new TreeMap<>();

	public Iterator<String> getMimeIterator() {
		return mimeToGlobListMap.keySet().iterator();
	}

	public Iterator<String> getGlobIterator(String mime) {
		Set<String> str = mimeToGlobListMap.get(mime);
		if(str == null)
			return null;
		return str.iterator();
	}

	public Iterator<String> getGlobIterator() {
		return globMap.keySet().iterator();
	}

	public Map<String, Boolean> getGlobMap() {
		return Collections.unmodifiableMap(globMap);
	}

	/**
	 * if already exist, no change in globMap
	 * 
	 * @param mimetype
	 * @param glob
	 */
	public void addGlobType(String mimetype, String glob) {
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

	public void setGlob(String glob, boolean b) {
		if (!globMap.containsKey(glob)) {
			LOG.warn("key(glob) is not exists.");
			return;
		}
		Boolean value = Boolean.valueOf(b);
		globMap.put(glob, value);
	}

	public Boolean getGlobUsing(String glob) {
		return globMap.get(glob);
	}

	public int getCountofGlob() {
		return globMap.size();
	}

	/**
	 * get mimetype count from glob
	 * 
	 * @param glob
	 * @return -1 if not exist.
	 */
	public int getCountofGlob(String mime) {
		if (!mimeToGlobListMap.keySet().contains(mime)) {
			return -1;
		} else {
			return mimeToGlobListMap.get(mime).size();
		}
	}

	public void initGlobTrue(){
		Iterator<String> iter = globMap.keySet().iterator();
		while(iter.hasNext()){
			globMap.put(iter.next(), Boolean.TRUE);
		}
	}

	@Override
	public boolean isAllowMime(String mime) {
		Iterator<String> iter = getGlobIterator(mime);
		if(iter == null)
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
	public void updateGlobPropertiesFile() throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		Iterator<String> iter = getGlobIterator();
		while (iter.hasNext()) {
			String glob = iter.next();
			Boolean b = globMap.get(glob);
			properties.setProperty(glob, Boolean.toString(b.booleanValue()));
		}
		Path propertiesPath = AppStartupConfig.propertiesPath;
		if (Files.exists(propertiesPath)) {
			Files.delete(propertiesPath);
		}

		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(propertiesPath.toFile()))) {
			properties.store(bos, String.valueOf(System.currentTimeMillis()));
		}
	}

	/**
	 * factory class of <b>TikaMimeXmlObject</b>
	 * 
	 * @author choechangwon
	 *
	 */
	public static class TikaMimeXmlObjectFactory {
		private static Map<String, TikaMimeXmlObject> container = new HashMap<String, TikaMimeXmlObject>();

		private TikaMimeXmlObjectFactory() {

		}

		public static TikaMimeXmlObject getInstanceFromXml(String xmlPath)
				throws ParserConfigurationException, SAXException, IOException {
			if (container.keySet().contains(xmlPath)) {
				return container.get(xmlPath);
			}
			TikaMimeXmlObject obj = new TikaMimeXmlObject();

			Properties p = new Properties();
			// load from property file
			if (Files.exists(AppStartupConfig.propertiesPath)) {
				try (BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(AppStartupConfig.propertiesPath.toFile()))) {
					p.load(bis);
					Iterator<Object> iter = p.keySet().iterator();
					while (iter.hasNext()) {
						String key = (String) iter.next();
						obj.globMap.put(key, Boolean.valueOf(p.getProperty(key)));
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
			addCustomMimeAndGlob(obj);
			return obj;
		}

		private static void addCustomMimeAndGlob(TikaMimeXmlObject obj) {
			try {
				Properties p = new Properties();
				InputStream is = new FileInputStream(AppStartupConfig.customTikaPropertiesPath.toFile());
				p.load(is);
				for (Map.Entry<Object, Object> entry : p.entrySet()) {
					obj.addGlobType((String) entry.getKey(), (String) entry.getValue());
				}
			} catch (FileNotFoundException e) {
				LOG.info(e.toString());
			} catch (IOException e) {
				LOG.info(e.toString());
			}

		}

	}
}
