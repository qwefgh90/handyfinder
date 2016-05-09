package com.qwefgh90.io.handyfinder.gui;

import static com.qwefgh90.io.handyfinder.gui.Java2JavascriptUtils.connectBackendObject;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * local file contents search engine with javafx webview and spring restful api
 * 
 * @author choechangwon
 */
public class AppStartupConfig extends Application implements Runnable {

	public final static String APP_DIRECTORY_NAME = "app";
	public final static String PAGE = "/" + APP_DIRECTORY_NAME + "/index.html";
	public final static String REDIRECT_PAGE = "/" + APP_DIRECTORY_NAME + "/redirect.html";
	public static String URL;
	public static Stage primaryStage;
	public static Application app;

	private final static String TEST_URL = "http://127.0.0.1:8020/app/index.html#/index";

	private static final Log LOG = LogFactory.getLog(AppStartupConfig.class);
	/**
	 * this method must be called in main() method
	 * 
	 * @throws IOException
	 */
	public static void initializeEnv() throws IOException {
		// Application Path
		deployedPath = getCurrentBuildPath();
		parentOfClassPath = deployedPath.getParent();
		pathForAppdata = parentOfClassPath.resolve("appdata");
		tomatLoggingFilePath = pathForAppdata.resolve("catalina.out");
		appLoggingFilePath = pathForAppdata.resolve("handyfinder.log");

		if (!Files.isWritable(parentOfClassPath)) {
			LOG.error("can't write resource");
			return;
		} else if (Files.exists(pathForAppdata)) {
			// Pass
		} else {
			Files.createDirectory(pathForAppdata);
		}

		address = "127.0.0.1";
		port = findFreePort();

		LOG.info("\nhandyfinder environment is initialized" + "\n" + "classpath: " + getCurrentBuildPath() + "\n"
				+ "appdata: " + pathForAppdata.toString());
	}
	
	@Override
	public void start(Stage primaryStage) {
		createWebView(primaryStage, PAGE);
	}

	private void createWebView(Stage primaryStage, String page) {

		AppStartupConfig.primaryStage = primaryStage;
		AppStartupConfig.app = this;

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
//		webView.getEngine().load(getClass().getResource(page).toExternalForm());
		webView.getEngine().load(URL);
		webView.getEngine().documentProperty().addListener(new ChangeListener<Document>() {
			@Override
			public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
				connectBackendObject(webView.getEngine(), "guiService", new GUIService(), true);
			}
		});
		
		primaryStage.setScene(new Scene(webView));
		primaryStage.setTitle("Your Assistant");
		primaryStage.show();

		LOG.info("\nhandyfinder started completely " + "\n" + "Webengine load: "
				+ getClass().getResource(page).toExternalForm()); // handyfinder
																	// app data
	}

	// initize variable
	public static Path deployedPath;
	public static Path parentOfClassPath;
	public static Path pathForAppdata;
	public static Path tomatLoggingFilePath;
	public static Path appLoggingFilePath;
	public static String address;
	public static int port;


	public static void main(String[] args)
			throws LifecycleException, ServletException, IOException, URISyntaxException {
		initializeEnv();

		// System.setProperty("prism.lcdtext", "false"); // enhance fonts
		// local server
		Tomcat tomcat = new Tomcat();
		tomcat.getConnector().setAttribute("address", address);
		tomcat.getConnector().setAttribute("port", port);

		File pathForAppdataFile = pathForAppdata.toFile();

		if (isJarStart())
			copyDirectoryInJar(deployedPath.toString(), APP_DIRECTORY_NAME, pathForAppdataFile);
		else
			FileUtils.copyDirectory(new File(deployedPath.toString()), pathForAppdataFile);

		Context ctx = tomcat.addWebapp("", pathForAppdataFile.getAbsolutePath());
		// https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html#addWebapp(org.apache.catalina.Host,%20java.lang.String,%20java.lang.String)

		ServletContext context = ctx.getServletContext();
		ctx.addWelcomeFile(REDIRECT_PAGE);
		tomcat.init();
		tomcat.start();
		URL = "http://" + address + ":" + port;
		try {
			healthCheck();
		} catch (TomcatInitFailException e) {
			//handling
			
			LOG.fatal(e.toString());
			assert false;	//dead "-ea" jvm option
		}
		LOG.info("tomcat started completely : " + URL); // handyfinder app data
		launch(args); // sync function // can't bean in spring container.
		tomcat.stop();
	}

	public static void healthCheck() throws TomcatInitFailException {
		String strUrl = "http://" + address + ":"+port+"/health";

		URL url;
		try {
			url = new URL(strUrl);
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			urlConn.connect();

			if (HttpURLConnection.HTTP_OK != urlConn.getResponseCode()) {
				throw new TomcatInitFailException();
			}
		} catch (MalformedURLException e) {
			LOG.error(e.toString());
			assert false;	//dead "-ea" jvm option
		} catch (IOException e) {
			LOG.error(e.toString());
			assert false;	//dead "-ea" jvm option
		}
	}

	static class TomcatInitFailException extends Exception {

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
				uri = AppStartupConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
			return Paths.get(uri);
		} else {
			return Paths.get(getResourcePath(""));
		}
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

	private static File createBaseDirectory() throws IOException {
		final File base = File.createTempFile("tmp-", "");
		if (!base.delete()) {
			throw new IOException("Cannot (re)create base folder: " + base.getAbsolutePath());

		}
		if (!base.mkdir()) {
			throw new IOException("Cannot create base folder: " + base.getAbsolutePath());
		}
		return base;
	}

	/**
	 * jar files and Copy to destination. If a file exists, overwrite it.<br>
	 * relative reference - http://cs.dvc.edu/HowTo_ReadJars.html
	 * 
	 * @param jarPath
	 * @param resourceDirInJar
	 *            - "/config" or "/config/" or "config" or ""
	 * @param destinationRoot
	 * @throws URISyntaxException
	 * @throws IOException
	 * @author qwefgh90
	 */
	public static void copyDirectoryInJar(String jarPath, String resourceDirInJar, File destinationRoot)
			throws URISyntaxException, IOException {
		if (resourceDirInJar.startsWith(File.separator)) { // replace to jar
															// entry style which
															// is not start with
															// '/'
			resourceDirInJar = resourceDirInJar.substring(1);
		}
		if (resourceDirInJar.length() != 0
				&& resourceDirInJar.getBytes()[resourceDirInJar.length() - 1] != File.separator.getBytes()[0]) // add
																												// rightmost
																												// seperator
			resourceDirInJar = resourceDirInJar + File.separator;

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		// loop entry
		while (entry != null) {

			if (entry.getName().startsWith(resourceDirInJar) // Directory in jar
					&& entry.getName().getBytes()[entry.getName().length() - 1] == File.separator.getBytes()[0]) {
				Files.createDirectories(new File(destinationRoot, entry.getName()).toPath());
			} else if (entry.getName().startsWith(resourceDirInJar) // File in
																	// jar
					&& entry.getName().getBytes()[entry.getName().length() - 1] != File.separator.getBytes()[0]) {
				File tempFile = extractTempFile(getResourceInputstream(entry.getName()));
				FileUtils.copyFile(tempFile, new File(destinationRoot.getAbsolutePath(), entry.getName())); // copy
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

	public static void copyFileInJar(String jarPath, String resourcePathInJar, File destinationRootDir)
			throws URISyntaxException, IOException {
		if (resourcePathInJar.startsWith(File.separator)) { // replace to jar
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
					&& entry.getName().getBytes()[entry.getName().length() - 1] != File.separator.getBytes()[0]) {
				File tempFile = extractTempFile(getResourceInputstream(entry.getName()));
				FileUtils.copyFile(tempFile, new File(destinationRootDir.getAbsolutePath(), entry.getName())); // copy
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
		return AppStartupConfig.class.getClassLoader().getResourceAsStream(resourceName);
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

	@Override
	public void run() {
		launch();
	}

}
