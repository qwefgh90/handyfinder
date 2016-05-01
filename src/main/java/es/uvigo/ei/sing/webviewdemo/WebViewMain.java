package es.uvigo.ei.sing.webviewdemo;

import static es.uvigo.ei.sing.javafx.webview.Java2JavascriptUtils.connectBackendObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.core.io.ClassPathResource;

import es.uvigo.ei.sing.webviewdemo.backend.CalculatorService;
import es.uvigo.ei.sing.webviewdemo.backend.FruitsService;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
	
public class WebViewMain extends Application {

	private final static String APP_DIRECTORY_NAME = "app";
	private final static String PAGE = "/" + APP_DIRECTORY_NAME + "/index.html";
	private final static String REDIRECT_PAGE = "/" + APP_DIRECTORY_NAME + "/redirect.html";

	@Override
	public void start(Stage primaryStage) {
		createWebView(primaryStage, PAGE);
	}

	private void createWebView(Stage primaryStage, String page) {

		// create the JavaFX webview
		final WebView webView = new WebView();

		// connect the FruitsService instance as "fruitsService"
		// javascript variable
		connectBackendObject(webView.getEngine(), "fruitsService",
				new FruitsService());

		// connect the CalculatorService instance as "calculatorService"
		// javascript variable
		connectBackendObject(webView.getEngine(), "calculatorService",
				new CalculatorService());

		// show "alert" Javascript messages in stdout (useful to debug)
		webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
			@Override
			public void handle(WebEvent<String> arg0) {
				System.err.println("alertwb1: " + arg0.getData());
			}
		});

		// load index.html
		webView.getEngine().load(getClass().getResource(page).toExternalForm());
		System.out.println(getClass().getResource(page).toExternalForm());

		primaryStage.setScene(new Scene(webView));
		primaryStage.setTitle("WebView with Java backend");
		primaryStage.show();
	}


	public static void main(String[] args) throws LifecycleException,
	ServletException, IOException, URISyntaxException {

		System.setProperty("prism.lcdtext", "false"); // enhance fonts
		// local server
		Tomcat tomcat = new Tomcat();
		tomcat.getConnector().setAttribute("address", "127.0.0.1");;
		tomcat.getConnector().setAttribute("port", "9292");

		Path deployedPath = getCurrentBuildPath() ;

		Path parentOfClassPath = deployedPath.getParent();
		Path pathForAppdata = parentOfClassPath.resolve("appdata");

		if(!Files.isWritable(parentOfClassPath)){
			System.out.println("can't write resource");
			return;
		}else if(Files.exists(pathForAppdata)){
			System.out.println("already exists");
		}else{
			Files.createDirectory(pathForAppdata);
		}

		File pathForAppdataFile = pathForAppdata.toFile();

		if(isJarStart())
			copyDirectoryInJar(deployedPath.toString(), APP_DIRECTORY_NAME, pathForAppdataFile);
		else
			FileUtils.copyDirectory(new File(deployedPath.toString()), pathForAppdataFile);

		System.out.println("classpath_root"+getCurrentBuildPath()); // 현재 클래스 위치
		System.out.println("appdata:"+pathForAppdataFile); // 현재 클래스 위치

		Context ctx = tomcat.addWebapp("", pathForAppdataFile.getAbsolutePath());
		// https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html#addWebapp(org.apache.catalina.Host,%20java.lang.String,%20java.lang.String)

		ServletContext context = ctx.getServletContext();
		ctx.addWelcomeFile(REDIRECT_PAGE);
		tomcat.start();
		//tomcat.getServer().await();
		
		// gui
		launch(args);
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
				uri = WebViewMain.class.getProtectionDomain()
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
			throw new IOException("Cannot (re)create base folder: "
					+ base.getAbsolutePath());

		}
		if (!base.mkdir()) {
			throw new IOException("Cannot create base folder: "
					+ base.getAbsolutePath());
		}
		return base;
	}


	/**
	 * jar files and Copy to destination.
	 * If a file exists, overwrite it.<br>
	 * relative reference - http://cs.dvc.edu/HowTo_ReadJars.html
	 * @param jarPath
	 * @param resourceDirInJar - "/config" or "/config/" or "config" or ""
	 * @param destinationRoot
	 * @throws URISyntaxException
	 * @throws IOException
	 * @author qwefgh90
	 */
	public static void copyDirectoryInJar(String jarPath, String resourceDirInJar, File destinationRoot) throws URISyntaxException, IOException
	{
		if(resourceDirInJar.startsWith(File.separator)){ //replace to jar entry style which is not start with '/'
			resourceDirInJar = resourceDirInJar.substring(1);
		}
		if(resourceDirInJar.length() != 0 && resourceDirInJar.getBytes()[resourceDirInJar.length()-1] != File.separator.getBytes()[0])	// add rightmost seperator 
			resourceDirInJar = resourceDirInJar + File.separator;

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		//loop entry
		while ( entry != null ) {

			if(entry.getName().startsWith(resourceDirInJar) //Directory in jar
					&& entry.getName().getBytes()[entry.getName().length()-1] == File.separator.getBytes()[0]){
				Files.createDirectories(new File(destinationRoot, entry.getName()).toPath());
			}else if(entry.getName().startsWith(resourceDirInJar)  //File in jar
					&& entry.getName().getBytes()[entry.getName().length()-1] != File.separator.getBytes()[0]){
				File tempFile = extractTempFile(getResourceInputstream(entry.getName()));
				FileUtils.copyFile(tempFile, new File(destinationRoot.getAbsolutePath(), entry.getName())); //copy from source file to destination file
				tempFile.delete();
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}

	public static void copyFileInJar(String jarPath, String resourcePathInJar, File destinationRootDir) throws URISyntaxException, IOException
	{
		if(resourcePathInJar.startsWith(File.separator)){ //replace to jar entry style which is not start with '/'
			resourcePathInJar = resourcePathInJar.substring(1);
		}

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		//loop entry
		while ( entry != null ) {
			if(entry.getName().startsWith(resourcePathInJar)  //File in jar
					&& entry.getName().getBytes()[entry.getName().length()-1] != File.separator.getBytes()[0]){
				File tempFile = extractTempFile(getResourceInputstream(entry.getName()));
				FileUtils.copyFile(tempFile, new File(destinationRootDir.getAbsolutePath(), entry.getName())); //copy from source file to destination file
				tempFile.delete();
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}


	/**
	 * Resource input stream in jar
	 * @param resourceName
	 * @return
	 */
	public static InputStream getResourceInputstream(String resourceName){
		return WebViewMain.class.getClassLoader().getResourceAsStream(resourceName);
	}

	/**
	 * This method is responsible for extracting resource files from within the .jar to the temporary directory.
	 * @param input - returned value of getClassLoader().getResourceAsStream("config/help.txt");
	 * @return Temp file created by stream
	 * @throws IOException
	 */
	public static File extractTempFile(InputStream input) throws IOException
	{
		File f = File.createTempFile("Thistempfile","willdelete");
		FileOutputStream tempFileos = new FileOutputStream(f);
		byte[] byteArray = new byte[1024];
		int i;
		//While the input stream has bytes
		while ((i = input.read(byteArray)) > 0) 
		{
			//Write the bytes to the output stream
			tempFileos.write(byteArray, 0, i);
		}
		//Close streams to prevent errors
		input.close();
		tempFileos.close();
		return f;

	}

}
