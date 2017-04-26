package io.github.qwefgh90.handyfinder.gui;

import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.copyDirectoryInJar;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.copyFileInJar;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.findFreePort;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.getCurrentBuildPath;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.getFourDigitsNumber;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.getResourceInputstream;
import static io.github.qwefgh90.handyfinder.gui.AppStartupUtil.isJarStart;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import javafx.application.Platform;

/**
 * local file contents search engine with javafx webview and spring restful api
 * 
 * @author choechangwon
 */
public class AppStartup{
	public final static String APP_DATA_DIR_NAME = "appdata";
	public final static String DB_NAME = "handyfinderdb";
	public final static String INDEX_DIR_NAME = "index";
	public final static String WEB_APP_DIRECTORY_NAME = "app";
	public final static Path deployedPath;
	public final static Path parentOfClassPath;
	public final static Path pathForLog4j;
	public final static Path resetFilePath;
	public final static Path versionFilePath;
	public final static Path pathForAppdata;
	public final static Path pathForDatabase;
	public final static Path pathForIndex;
	public final static Path tomatLoggingFilePath;
	public final static Path appLoggingFilePath;
	public final static Path tikaXmlFilePath;
	public final static Path customTikaGlobPropertiesPath;
	public final static Path propertiesPath;
	public final static Path appDataJsonPath;
	public final static Path processLockPath;
	public final static String address;
	public final static int port;
	public final static String homeUrl;
	public final static Optional<String> versionOpt;
	public final static String secretKey;
	
	public final static boolean alreadyProcessExists;
	public final static Optional<String> alreadyHomeUrl;
	public final static Optional<Integer> alreadyPid;

	public final static String RESOURCE_LOADING_PAGE = "/" + APP_DATA_DIR_NAME
			+ "/" + WEB_APP_DIRECTORY_NAME + "/loading.html";
	public final static String PAGE = "/" + WEB_APP_DIRECTORY_NAME
			+ "/index.html";
	public final static String REDIRECT_PAGE = "/" + WEB_APP_DIRECTORY_NAME
			+ "/redirect.html";
	
	private final static Logger LOG = LoggerFactory
			.getLogger(AppStartup.class);

	/**
	 * application system variable is initialized. deploy resources.
	 */
	static {
		final boolean isProduct = isJarStart();
		// Application Path
		deployedPath = getCurrentBuildPath(); //jar file or classes dir
		parentOfClassPath = deployedPath.getParent();
		
		if (isProduct) {
			versionFilePath = parentOfClassPath.resolve("version");
			resetFilePath = parentOfClassPath.resolve("INVALIDATE_INDEX");
			pathForLog4j = parentOfClassPath.resolve("log4j.xml");
			pathForAppdata = parentOfClassPath.resolve(APP_DATA_DIR_NAME);
		}
		else{
			versionFilePath = deployedPath.resolve("version");
			resetFilePath = deployedPath.resolve("INVALIDATE_INDEX");
			pathForLog4j = deployedPath.resolve("log4j.xml");			//possible to redeploy on dev mode
			pathForAppdata = deployedPath.resolve(APP_DATA_DIR_NAME);	//possible to redeploy on dev mode
		}
		
		pathForDatabase = pathForAppdata.resolve(DB_NAME);
		pathForIndex = pathForAppdata.resolve(INDEX_DIR_NAME);
		tomatLoggingFilePath = pathForAppdata.resolve("catalina.out");
		appLoggingFilePath = pathForAppdata.resolve("handyfinder.log");
		tikaXmlFilePath = pathForAppdata.resolve("tika-mimetypes.xml");
		propertiesPath = pathForAppdata.resolve("glob-used.properties");
		appDataJsonPath = pathForAppdata.resolve("appdata.json");
		processLockPath = pathForAppdata.resolve("pid.lock");
		customTikaGlobPropertiesPath = pathForAppdata
				.resolve("custom-tika-mimetypes.properties");

		address = "127.0.0.1";
		port = findFreePort();
		homeUrl = "http://" + address + ":" + port;
		secretKey = getFourDigitsNumber();
		
		// create AppData directory
		if (!Files.isWritable(parentOfClassPath)) {
			throw new RuntimeException("can't write resource classpath");
		} else {
			if(!Files.exists(pathForAppdata)){
				try {
					Files.createDirectory(pathForAppdata);
				} catch (IOException e) {
					throw new RuntimeException(ExceptionUtils.getStackTrace(e));
				}
			}
		}

		//Is a process already running?
		if(ProcessLockUtil.getProcessInfo(ProcessLockUtil.getPidFromLock(processLockPath).orElse(-1))
				.isPresent()){
			alreadyProcessExists = true;
			alreadyHomeUrl = ProcessLockUtil.getHomeUrlFromLock(processLockPath);
			alreadyPid = ProcessLockUtil.getPidFromLock(processLockPath);
		}else{
			alreadyProcessExists = false;
			alreadyHomeUrl = Optional.empty();
			alreadyPid = Optional.empty();
			ProcessLockUtil.deleteAndWriteLockFile(processLockPath, ProcessLockUtil.getCurrentPid(), homeUrl);
		}

		// deploy basic files
		try {
			if (isProduct) { // startup in jar
				copyFileInJar(deployedPath.toString(), pathForLog4j.getFileName().toString(),
						parentOfClassPath.toFile(), (File file, JarEntry entry) -> {return (!file.exists() || file.lastModified() < entry.getLastModifiedTime().toMillis());});
				
				System.out.println("Initializing log4j with: " + pathForLog4j);
				DOMConfigurator.configureAndWatch(pathForLog4j.toAbsolutePath().toString());
				
				// resources which is in jar copy to appdata deployed.
				copyDirectoryInJar(deployedPath.toString(), APP_DATA_DIR_NAME,
						parentOfClassPath.toFile(), (File file, JarEntry entry) -> file.lastModified() < entry.getLastModifiedTime().toMillis());
				
				//extract version string from jar
				try(InputStream is = getResourceInputstream("version.properties")){
					final Properties prop = new Properties();
					prop.load(is);
					versionOpt = Optional.of(prop.getProperty("version").trim());
					LOG.info("version : " + versionOpt.get());
				}
				
				//compare new version to old version, then create or not file 
				if(Files.exists(versionFilePath)){
					//read old file
					try(final BufferedReader reader = Files.newBufferedReader(versionFilePath)){
						final String oldVersion = reader.readLine();
						final boolean newVersionFound = versionOpt.get().compareToIgnoreCase((oldVersion == null ? "0.001" : oldVersion.trim())) > 0 ? true : false;
						if(newVersionFound == true){
							if(!Files.exists(resetFilePath))
								Files.createFile(resetFilePath);
						}
					}
					//remove old
					Files.delete(versionFilePath);
				}else{
					if(!Files.exists(resetFilePath))
						Files.createFile(resetFilePath);
				}

				//create new version file
				try(PrintWriter writer = new PrintWriter(versionFilePath.toFile())){
					writer.print(versionOpt.get());
				}
			} else { // no jar start
				DOMConfigurator.configureAndWatch(pathForLog4j.toAbsolutePath().toString());
				versionOpt = Optional.empty();
				LOG.info("version : not found");
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
				.append("[Handyfinder information]").append("\n")
				.append("* classpath: ").append(getCurrentBuildPath()).append("\n")
				.append("* appdata path: ").append(pathForAppdata.toString()).append("\n")
				.append("* log4j path: ").append(pathForLog4j.toString()).append("\n");
		LOG.info(logBuilder.toString());

		logBuilder.setLength(0);
	}

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
			InterruptedException, ExecutionException {
		if (!parseArguments(args))
			return; // failed
		else
			parameterInit = true;

		//disable same origin policy 
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		GUIApplication.start(args); // sync function // can't bean in spring container.
	}

	/**
	 * Handy finder App API
	 * terminate applicationf
	 */
	public static void terminateProgram() {
		try {
			if (!GUIApplication.getSingleton().get().isStop()) {
				Platform.exit();
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
				copyFileInJar(jarPath, resourceName,
						tikaXmlFilePath.getParent().toFile(), (file, entry) -> !file.exists());
			}
		}
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
		AppStartup.rootAppContext = rootAppContext;
	}

	public static void setServletAppContext(
			WebApplicationContext servletAppContext) {
		AppStartup.servletAppContext = servletAppContext;
	}


}
