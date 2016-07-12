package io.github.qwefgh90.handyfinder.gui;

import static io.github.qwefgh90.handyfinder.gui.Java2JavascriptUtils.connectBackendObject;
import io.github.qwefgh90.handyfinder.exception.TomcatInitFailException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
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
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * local file contents search engine with javafx webview and spring restful api
 * 
 * @author choechangwon
 */
@SuppressWarnings("restriction")
public class AppStartupConfig extends Application {

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
	public static AppStartupConfig app;
	public static Stage primaryStage;
	private static boolean SERVER_ONLY = false;
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

	private static BlockingQueue<String> paragrahQueue = new LinkedBlockingQueue<>();
	final private static Thread was;
	static {
		was = new Thread(() -> {
			// local tomcat server initializing...
				try {
					AppStartupConfig.paragrahQueue.put("server initialzing...");
					Tomcat tomcat = new Tomcat();

					AppStartupConfig.tomcat = tomcat;
					tomcat.getConnector().setAttribute("address", address);
					tomcat.getConnector().setAttribute("port", port);

					Context context = tomcat.addWebapp("", pathForAppdata
							.toAbsolutePath().toString());
					// https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html#addWebapp(org.apache.catalina.Host,%20java.lang.String,%20java.lang.String)

				context.setJarScanner(new FastJarScanner());
				context.addWelcomeFile(REDIRECT_PAGE);
				tomcat.init();
				AppStartupConfig.paragrahQueue.put("server startup...");
				tomcat.start();
				AppStartupConfig.paragrahQueue.put("server health checking...");
				healthCheck();
				if (SERVER_ONLY == false) {
					Platform.runLater(() -> AppStartupConfig.app
							.showUI(AppStartupConfig.app::setWebviewAfterLoading));
				}
				LOG.info("tomcat started completely : " + homeUrl);
			} catch (Exception e) {
				LOG.error(e.toString());
				terminateProgram();
			} finally {
				try {
					AppStartupConfig.paragrahQueue.put("");
				} catch (Exception e) {
					LOG.error(e.toString());
				}
			}
		});
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

		was.start();

		if (SERVER_ONLY == false) {
			launch(args); // sync function // can't bean in spring container.
			was.join();
			tomcat.stop();
		}
	}

	@Override
	public void start(Stage primaryStage) {
		AppStartupConfig.primaryStage = primaryStage;
		AppStartupConfig.app = this;
		try {
			AppStartupConfig.paragrahQueue.put("GUI initialzing...");
		} catch (InterruptedException e1) {
			LOG.error(e1.toString());
		}
		primaryStage.setOnCloseRequest(event -> {
			Preferences userPrefs = Preferences
					.userNodeForPackage(AppStartupConfig.class);
			userPrefs.putDouble("stage.x", primaryStage.getX());
			userPrefs.putDouble("stage.y", primaryStage.getY());
			userPrefs.putDouble("stage.width", primaryStage.getWidth());
			userPrefs.putDouble("stage.height", primaryStage.getHeight());
		});
		LOG.info("javafx is initialized ");
		showUI(this::setWebviewBeforeLoading);
		Thread pragraphWorker = new Thread(
				() -> {
					while (true) {
						try {
							final String pragraph = AppStartupConfig.paragrahQueue
									.take();
							if (pragraph.equals(""))
								break;
							if (SERVER_ONLY == false) {
								Platform.runLater(() -> {
									setLoadingParagraph(pragraph);
								});
							}
						} catch (Exception e) {
							LOG.error(e.toString());
							break;
						}
					}
				});
		pragraphWorker.start();
	}

	private WebView currentView = null;

	private void showUI(Supplier<WebView> run) {
		if (AppStartupConfig.primaryStage == null
				|| AppStartupConfig.app == null || SERVER_ONLY == true)
			throw new IllegalStateException(
					"Javafx startup is not nomally initialized");
		currentView = run.get();
		primaryStage.show();
	}

	/**
	 * 
	 * @param paragraph
	 * @throws IllegalStateException
	 *             if not called setWebviewBeforeLoading() or if
	 *             setWebviewAfterLoading() is already called
	 */
	private void setLoadingParagraph(String paragraph) {
		if (currentView == null) {
			throw new IllegalStateException("currentView is not initializaed");
		}
		WebEngine eg = currentView.getEngine();
		Element element = (Element) eg
				.executeScript("document.getElementById('loading')");
		if (element == null) {
			throw new IllegalStateException(
					"can't set paragraph. setWebviewAfterLoading() is already called.");
		}
		element.setTextContent(paragraph);
	}

	private WebView setWebviewBeforeLoading() {
		// create the JavaFX webview
		final WebView webView = new WebView();
		webView.getEngine().load(
				AppStartupConfig.class.getResource(RESOURCE_LOADING_PAGE)
						.toExternalForm());
		Scene scene = new Scene(webView);

		primaryStage.setScene(scene);
		primaryStage.setTitle("Loading...");
		primaryStage.setWidth(300);
		primaryStage.setHeight(330);
		primaryStage.centerOnScreen();

		return webView;
	}

	private WebView setWebviewAfterLoading() {//
		// create the JavaFX webview
		final WebView webView = new WebView();
		// show "alert" Javascript messages in stdout (useful to debug)
		webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
			@Override
			public void handle(WebEvent<String> arg0) {
				System.err.println("alertwb1: " + arg0.getData());
			}
		});

		// load index.html
		// webView.getEngine().load(getClass().getResource(page).toExternalForm());
		webView.getEngine().load(homeUrl);
		webView.getEngine().documentProperty()
				.addListener(new ChangeListener<Document>() {
					@Override
					public void changed(
							ObservableValue<? extends Document> prop,
							Document oldDoc, Document newDoc) {
						connectBackendObject(webView.getEngine(), "guiService",
								new GUIService(), true);
					}
				});

		primaryStage.setScene(new Scene(webView));
		primaryStage.setTitle("Your Assistant");

		Preferences userPrefs = Preferences
				.userNodeForPackage(AppStartupConfig.class);
		// get window location from user preferences: use x=100, y=100,
		// width=400, height=400 as default
		double x = userPrefs.getDouble("stage.x", 100);
		double y = userPrefs.getDouble("stage.y", 100);
		double w = userPrefs.getDouble("stage.width", 400);
		double h = userPrefs.getDouble("stage.height", 400);
		primaryStage.setX(x);
		primaryStage.setY(y);
		primaryStage.setWidth(w);
		primaryStage.setHeight(h);

		LOG.info("\nhandyfinder started completely " + "\n"
				+ "Webengine load: " + homeUrl); // handyfinder
		return webView;
	}

	/**
	 * terminate application
	 * 
	 * @throws Exception
	 */
	private static Tomcat tomcat;

	public static void terminateProgram() {
		try {
			was.join();
			if (AppStartupConfig.app != null) {
				app.stop();
			} else {
				AppStartupConfig.tomcat.stop();
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}

	public static Thread getWasThread() {
		return was;
	}

	public static void healthCheck() throws TomcatInitFailException {
		String strUrl = "http://" + address + ":" + port + "/health";

		URL url;
		try {
			url = new URL(strUrl);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			urlConn.connect();

			if (HttpURLConnection.HTTP_OK != urlConn.getResponseCode()) {
				throw new TomcatInitFailException(
						"tomcat initialization failed.");
			}
		} catch (MalformedURLException e) {
			LOG.error(e.toString());
			assert false; // dead "-ea" jvm option
		} catch (IOException e) {
			LOG.error(e.toString());
			assert false; // dead "-ea" jvm option
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

	/**
	 * Fast Jar Scanner scans one kind of jar like handyfinder.jar
	 * 
	 * @author cheochangwon
	 *
	 */
	private static class FastJarScanner extends StandardJarScanner {
		@Override
		public void scan(JarScanType scanType, ServletContext context,
				JarScannerCallback callback) {
			StandardJarScanFilter filter = new StandardJarScanFilter();
			filter.setDefaultTldScan(false);
			filter.setPluggabilitySkip("*.jar");
			filter.setPluggabilityScan("*handyfinder*");
			setJarScanFilter(filter);

			super.scan(scanType, context, callback);
		}

		@Override
		public void setJarScanFilter(JarScanFilter jarScanFilter) {
			super.setJarScanFilter(jarScanFilter);
		}
	}

}
