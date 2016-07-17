package io.github.qwefgh90.handyfinder.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.*;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.WebApplicationContext;

import com.sun.jna.Platform;

/**
 * local file contents search engine with javafx webview and spring restful api
 * 
 * @author choechangwon
 */
public class AppStartupConfig{

	// initial variable
	public static boolean TEST_MODE = false;

	// exchagable to TEST_APP_DATA_DIR_NAME
	public final static String APP_DATA_DIR_NAME = "appdata";
	public final static String DB_NAME = "handyfinderdb";
	public final static String INDEX_DIR_NAME = "index";
	public final static String WEB_APP_DIRECTORY_NAME = "app";
	public final static Path deployedPath;
	public final static Path parentOfClassPath;
	public final static Path pathForAppdata;
	public final static Path pathForDatabase;
	public final static Path pathForIndex;
	public final static Path tomatLoggingFilePath;
	public final static Path appLoggingFilePath;
	public final static Path tikaXmlFilePath;
	public final static Path customTikaGlobPropertiesPath;
	public final static Path propertiesPath;
	public final static Path appDataJsonPath;
	public final static String address;
	public final static int port;
	public final static String homeUrl;

	public final static String RESOURCE_LOADING_PAGE = "/" + APP_DATA_DIR_NAME
			+ "/" + WEB_APP_DIRECTORY_NAME + "/loading.html";
	public final static String PAGE = "/" + WEB_APP_DIRECTORY_NAME
			+ "/index.html";
	public final static String REDIRECT_PAGE = "/" + WEB_APP_DIRECTORY_NAME
			+ "/redirect.html";
	
	private final static Logger LOG = LoggerFactory
			.getLogger(AppStartupConfig.class);

	/**
	 * application system variable is initialized. deploy resources.
	 */
	static {
		// Application Path
		deployedPath = getCurrentBuildPath();
		parentOfClassPath = deployedPath.getParent();
		pathForAppdata = parentOfClassPath.resolve(APP_DATA_DIR_NAME);
		pathForDatabase = pathForAppdata.resolve(DB_NAME);
		pathForIndex = pathForAppdata.resolve(INDEX_DIR_NAME);
		tomatLoggingFilePath = pathForAppdata.resolve("catalina.out");
		appLoggingFilePath = pathForAppdata.resolve("handyfinder.log");
		tikaXmlFilePath = pathForAppdata.resolve("tika-mimetypes.xml");
		propertiesPath = pathForAppdata.resolve("glob-used.properties");
		appDataJsonPath = pathForAppdata.resolve("appdata.json");
		customTikaGlobPropertiesPath = pathForAppdata
				.resolve("custom-tika-mimetypes.properties");

		address = "127.0.0.1";
		port = findFreePort();
		homeUrl = "http://" + address + ":" + port;

		// create appdata dir
		if (!Files.isWritable(parentOfClassPath)) {
			LOG.error("can't write resource classpath");
			throw new RuntimeException("can't write resource classpath");
		} else if (Files.exists(pathForAppdata)) {
			// Pass
		} else {
			try {
				Files.createDirectory(pathForAppdata);
			} catch (IOException e) {
				LOG.error("fail to create resource directory");
				throw new RuntimeException(ExceptionUtils.getStackTrace(e));
			}
		}

		// deploy basic files
		try {
			if (isJarStart()) { // jar start
				// resources which is in jar copy to appdata deployed.
				copyDirectoryInJar(deployedPath.toString(), APP_DATA_DIR_NAME,
						parentOfClassPath.toFile());
			} else { // no jar start
				// all files copied in classpath
				Path classsPath = deployedPath.getParent().resolve("classes");
				FileUtils.copyDirectory(classsPath.toFile(),
						parentOfClassPath.toFile());
			}
			// tika-mimetypes.xml copy to appdata
			copyTikaXml();
		} catch (URISyntaxException e) {
			LOG.error("fail to copy resource to app directory");
			throw new RuntimeException(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			LOG.error("fail to copy resource to app directory");
			throw new RuntimeException(ExceptionUtils.getStackTrace(e));
		}

		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append("\n")
				.append("handyfinder environment is initialized").append("\n")
				.append("current classpath: ").append(getCurrentBuildPath())
				.append("\n").append("appdata: ")
				.append(pathForAppdata.toString());
		LOG.info(logBuilder.toString());

		logBuilder.setLength(0);
		/*
		 * String[] allPath = allClassPath(); for (int i = 0; i <
		 * allPath.length; i++) {
		 * logBuilder.append("classpath: ").append("\n").append(String.valueOf(i
		 * + 1)).append(") ").append(allPath[i]); }
		 * 
		 * LOG.debug(logBuilder.toString());
		 */
	}

	public static GUIApplication getGuiApp(){return GUIApplication.getSingleton();}
	private static boolean parameterInit = false;
	private static boolean SERVER_ONLY = false;
	/**
	 * lazy init.. warning
	 * @return
	 */
	public static boolean getServerOnlyMode() {
		//
		if(parameterInit == false)
			throw new IllegalStateException("SERVER_ONLY variable is not initialized");
		return SERVER_ONLY;
	}
	
	/**
	 * this method must be called in main() method
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public static boolean parseArguments(String[] args) throws IOException,
			ParseException {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// create the Options
		Options options = new Options();
		options.addOption("n", "no-gui", false,
				"execute server only without GUI (User Interface)");
		options.addOption("h", "help", false, "print help text");

		if (args != null) {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("handyfinder", options);
				return false;
			}
			if (line.hasOption("no-gui")) {
				SERVER_ONLY = true;
			}
		}
		
		return true;
	}
	
	public static void main(String[] args) throws LifecycleException,
			ServletException, IOException, URISyntaxException, ParseException,
			InterruptedException {
		if (!parseArguments(args))
			return; // failed
		else
			parameterInit = true;

		getGuiApp().start(args); // sync function // can't bean in spring container.
	}

	/**
	 * Handy finder App API
	 * terminate applicationf
	 */
	public static void terminateProgram() {
		try {
			if (!getGuiApp().isStop()) {
				getGuiApp().stop(); //second terminate is ignored.
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}

	private static void copyTikaXml() throws URISyntaxException, IOException {
		String metaXmlUrl = MimeTypes.class.getResource("tika-mimetypes.xml")
				.toURI().toString();
		if (metaXmlUrl.startsWith("jar")) {
			Pattern pat = Pattern.compile("jar:file:(.+)!(.+)");
			Matcher matcher = pat.matcher(metaXmlUrl);
			String jarPath = null;
			String resourceName = null;
			if (matcher.matches()) {
				jarPath = matcher.group(1);
				if (SystemUtils.IS_OS_WINDOWS)
					jarPath = jarPath.substring(1);
				resourceName = matcher.group(2);
				AppStartupConfig.copyFileInJar(jarPath, resourceName,
						tikaXmlFilePath.getParent().toFile(), true);
			}
		}
	}

	/**
	 * Returns a free port number on localhost, or throw runtime exception if
	 * unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free
	 *         port
	 * @since 3.0
	 */
	public static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
		}
		throw new RuntimeException("no available ports.");
	}

	/**
	 * get exection jar path
	 * 
	 * @return String - path
	 */
	public static Path getCurrentBuildPath() {
		if (getResourcePath("") == null) {
			URI uri;
			try {
				uri = AppStartupConfig.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
			return Paths.get(uri);
		} else {
			return Paths.get(getResourcePath(""));
		}
	}

	public static String[] allClassPath() {
		ArrayList<String> arr = new ArrayList<String>();
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			arr.add(url.getFile());
		}

		return arr.toArray(new String[arr.size()]);
	}

	public static boolean isJarStart() {
		return getCurrentBuildPath().toString().endsWith(".jar");
	}

	/**
	 * Handle resourceName, whether File.separator exists or not
	 * 
	 * @param dirName
	 * @return
	 */
	public static String getResourcePath(String dirName) {
		try {
			return new ClassPathResource(dirName).getFile().getAbsolutePath();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * jar files and Copy to destination. If a file exists, overwrite it.<br>
	 * relative reference - http://cs.dvc.edu/HowTo_ReadJars.html jar resource
	 * use forward slash (/)
	 * 
	 * @param jarPath
	 * @param resourceDirInJar
	 *            - "/config" or "/config/" or "config" or ""
	 * @param destinationRoot
	 * @throws URISyntaxException
	 * @throws IOException
	 * @author qwefgh90
	 */
	public static void copyDirectoryInJar(String jarPath,
			String resourceDirInJar, File destinationRoot)
			throws URISyntaxException, IOException {
		if (resourceDirInJar.startsWith("/")) { // jar url start with /
												// replace to jar
												// entry style which
												// is not start with
												// '/'
			resourceDirInJar = resourceDirInJar.substring(1);
		}
		if (resourceDirInJar.length() != 0
				&& resourceDirInJar.getBytes()[resourceDirInJar.length() - 1] != File.separator
						.getBytes()[0]) // add
										// rightmost
										// seperator
			resourceDirInJar = resourceDirInJar + "/";

		LOG.trace("package extract info : " + "\nFile.separator : "
				+ File.separator + "\nresourceDirInJar : " + resourceDirInJar
				+ "\njarPath : " + jarPath + "\ndestinationRoot"
				+ destinationRoot);

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		// loop entry
		while (entry != null) {
			LOG.trace("extract from java : " + entry.getName());
			if (entry.getName().startsWith(resourceDirInJar) // Directory in jar
					&& entry.getName().endsWith("/")) {
				LOG.trace("create start : " + entry.getName());
				Files.createDirectories(new File(destinationRoot, entry
						.getName()).toPath());
			} else if (entry.getName().startsWith(resourceDirInJar) // File in
																	// jar
					&& !entry.getName().endsWith("/")) {

				LOG.trace("copy start : " + entry.getName());
				File tempFile = extractTempFile(getResourceInputstream(entry
						.getName()));
				FileUtils.copyFile(
						tempFile,
						new File(destinationRoot.getAbsolutePath(), entry
								.getName())); // copy
				tempFile.delete();
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}

	public static void copyFileInJar(String jarPath, String resourcePathInJar,
			File destinationRootDir, boolean ignoreHierarchyOfResource)
			throws URISyntaxException, IOException {
		if (resourcePathInJar.startsWith("/")) { // jar url start with /
													// replace to jar
													// entry style which
													// is not start with
													// '/'
			resourcePathInJar = resourcePathInJar.substring(1);
		}

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		// loop entry
		while (entry != null) {
			if (entry.getName().startsWith(resourcePathInJar) // File in jar
					&& entry.getName().getBytes()[entry.getName().length() - 1] != File.separator
							.getBytes()[0]) {
				File tempFile = extractTempFile(getResourceInputstream(entry
						.getName()));
				if (ignoreHierarchyOfResource)
					FileUtils.copyFile(
							tempFile,
							new File(destinationRootDir.getAbsolutePath(),
									entry.getName().substring(
											entry.getName().lastIndexOf("/"))));
				else
					FileUtils.copyFile(tempFile,
							new File(destinationRootDir.getAbsolutePath(),
									entry.getName())); // copy
														// from
														// source
														// file
														// to
														// destination
														// file
				tempFile.delete();
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}

	/**
	 * Resource input stream in jar
	 * 
	 * @param resourceName
	 * @return
	 */
	public static InputStream getResourceInputstream(String resourceName) {
		return AppStartupConfig.class.getClassLoader().getResourceAsStream(
				resourceName);
	}

	/**
	 * This method is responsible for extracting resource files from within the
	 * .jar to the temporary directory.
	 * 
	 * @param input
	 *            - returned value of
	 *            getClassLoader().getResourceAsStream("config/help.txt");
	 * @return Temp file created by stream
	 * @throws IOException
	 */
	public static File extractTempFile(InputStream input) throws IOException {
		File f = File.createTempFile("Thistempfile", "willdelete");
		FileOutputStream tempFileos = new FileOutputStream(f);
		byte[] byteArray = new byte[1024];
		int i;
		// While the input stream has bytes
		while ((i = input.read(byteArray)) > 0) {
			// Write the bytes to the output stream
			tempFileos.write(byteArray, 0, i);
		}
		// Close streams to prevent errors
		input.close();
		tempFileos.close();
		return f;

	}

	/*
	 * dirty hack use it if can't annotated base injection
	 */
	private static WebApplicationContext rootAppContext;
	private static WebApplicationContext servletAppContext;

	public static <T> T getBean(Class<T> c) {
		T svc = servletAppContext.getBean(c);
		return svc;
	}

	public static void setRootAppContext(WebApplicationContext rootAppContext) {
		AppStartupConfig.rootAppContext = rootAppContext;
	}

	public static void setServletAppContext(
			WebApplicationContext servletAppContext) {
		AppStartupConfig.servletAppContext = servletAppContext;
	}


}
