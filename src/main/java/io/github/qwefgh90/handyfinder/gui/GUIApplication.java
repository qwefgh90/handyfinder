package io.github.qwefgh90.handyfinder.gui;

import static io.github.qwefgh90.handyfinder.gui.Java2JavascriptUtils.connectBackendObject;
import io.github.qwefgh90.handyfinder.exception.TomcatInitFailException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import javax.servlet.ServletContext;

import net.sf.ehcache.constructs.nonstop.store.ExecutorServiceStore;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GUIApplication extends Application {
	private final Logger LOG = LoggerFactory
			.getLogger(GUIApplication.class);

	private final static CompletableFuture<GUIApplication> self = new CompletableFuture<>();
	
	public static CompletableFuture<GUIApplication> getSingleton() {
		return self;
	}
	
	public static void start(String[] args){
		GUIApplication.launch(args);
	}
	
	private final double WINDOW_LOADING_WIDTH = 300;
	private final double WINDOW_LOADING_HEIGHT = 350;

	private WebView currentView = null;
	private Tomcat tomcat;
	private Stage primaryStage;

	private boolean stopped = false;
	public boolean isStop(){ return stopped; }

	public void healthCheck() throws TomcatInitFailException {
		String strUrl = "http://" + AppStartup.address + ":"
				+ AppStartup.port + "/health";

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
		} catch (IOException e) {
			LOG.error(e.toString());
		}
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		LOG.info("Handyfinder is Loading");
		
		//Register a callback when closing
		primaryStage.setOnCloseRequest(this::onCloseTask);
		
		showUI(this::initializeWebviewWhenLoading);
		CompletableFuture.runAsync(this::loadingTask);
	}
	
	public <T extends javafx.event.Event> void onCloseTask(T event){
		showUI(this::initializeWebviewWhenLoading);
		Platform.runLater(() -> setLoadingParagraphBeforeLoading("Handyfinder Stopping..."));
		stopped = true;
		LOG.info("javafx onCloseRequest()");
		final Preferences userPrefs = Preferences
				.userNodeForPackage(AppStartup.class);
		userPrefs.putDouble("stage.x", primaryStage.getX());
		userPrefs.putDouble("stage.y", primaryStage.getY());
		userPrefs.putDouble("stage.width", primaryStage.getWidth());
		userPrefs.putDouble("stage.height", primaryStage.getHeight());

		CompletableFuture.runAsync(() -> {
			LOG.info("tomcat is stopping");
			try {
				if(tomcat != null)
					tomcat.stop();
			} catch (Exception e) {
				LOG.error(ExceptionUtils.getStackTrace(e));
			} finally {
				Platform.exit(); //direct exit
				LOG.info("javafx Platform.exit() called");
			}
		});
		
		event.consume();
	}
	
	public Boolean loadingTask(){
		try {
			Platform.runLater(() -> setLoadingParagraphBeforeLoading("process checking..."));

			if(!AppStartup.alreadyProcessExists){
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server initialzing..."));

				tomcat = new Tomcat();
				tomcat.getConnector().setAttribute("address",
						AppStartup.address);
				tomcat.getConnector().setAttribute("port",
						AppStartup.port);

				Context context = tomcat.addWebapp("",
						AppStartup.pathForAppdata.toAbsolutePath()
						.toString());
				// https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html#addWebapp(org.apache.catalina.Host,%20java.lang.String,%20java.lang.String)

				context.setJarScanner(new FastJarScanner());
				context.addWelcomeFile(AppStartup.REDIRECT_PAGE);
				tomcat.init();
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server startup..."));
				tomcat.start();
				
				self.complete(this);
				
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server health checking..."));
				healthCheck();
				Platform.runLater(() -> showUI(GUIApplication.this::initializeWebviewWhenComplete));
			}else{
				Platform.runLater(() -> setLoadingParagraphBeforeLoading(
						"handyfinder is already running...\n" + AppStartup.alreadyHomeUrl.get()));
			}
		} catch (Exception e) {
			LOG.error(e.toString());
			return false;
		}
		return true;
	}


	@Override
	public void init() throws Exception {
		super.init();
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		LOG.info("javafx is stopped");
	}

	/**
	 * This api is that open dialog and select directory and consume it.
	 * if operation is failed, directory paramenter is "",  
	 * @param directoryConsumer directory consumer
	 */
	public void openAndSelectDirectory(Consumer<String> directoryConsumer){
		Platform.runLater(() -> {
			String result = "";
			try {
				final DirectoryChooser directoryChooser = new DirectoryChooser();
				final File selectedDirectory = directoryChooser
						.showDialog(GUIApplication.this.primaryStage);
				if (selectedDirectory != null) {
					result = selectedDirectory.getAbsolutePath();
				}
			} catch (Exception e) {
				LOG.error(e.toString());
			}
			directoryConsumer.accept(result);
		});
	}

	private void showUI(Supplier<WebView> run) {
		if (this.primaryStage == null){
			LOG.warn("Javafx startup is not nomally initialized");
			return;
		}
		currentView = run.get();
		if (!AppStartup.getServerOnlyMode())
			primaryStage.show();
	}

	/**
	 * 
	 * @param paragraph
	 * @throws IllegalStateException
	 *             if not called setWebviewBeforeLoading() or if
	 *             setWebviewAfterLoading() is already called
	 */
	private void setLoadingParagraphBeforeLoading(String paragraph) {
		WebEngine eg = currentView.getEngine();
		Element element = (Element) eg
				.executeScript("document.getElementById('loading')");
		if (element == null) {
			LOG.warn("can't set loading paragraph. setWebviewAfterLoading() is already called.");
			return;
		}
		element.setTextContent(paragraph);
	}

	private WebView initializeWebviewWhenLoading() {
		// create the JavaFX webview
		final WebView webView = new WebView();
		webView.getEngine().load(
				AppStartup.class.getResource(
						AppStartup.RESOURCE_LOADING_PAGE)
				.toExternalForm());
		final Scene scene = new Scene(webView);

		primaryStage.setScene(scene);
		primaryStage.setTitle(TITLE.BEFORE_LOADING.getTitle());
		primaryStage.setWidth(WINDOW_LOADING_WIDTH);
		primaryStage.setHeight(WINDOW_LOADING_HEIGHT);
		primaryStage.centerOnScreen();
		return webView;
	}

	private WebView initializeWebviewWhenComplete() {
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
		webView.getEngine().load(AppStartup.homeUrl);
		webView.getEngine().documentProperty()
		.addListener(new ChangeListener<Document>() {
			@Override
			public void changed(
					ObservableValue<? extends Document> prop,
					Document oldDoc, Document newDoc) {
				connectBackendObject(webView.getEngine(), "secretKey", AppStartup.secretKey, true);
			//	connectBackendObject(webView.getEngine(), "guiService",
			//			new GUIService(), true);
			}
		});

		primaryStage.setScene(new Scene(webView));
		primaryStage.setTitle(TITLE.AFTER_LOADING.getTitle());

		Preferences userPrefs = Preferences
				.userNodeForPackage(AppStartup.class);

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		final double WINDOW_DEFAULT_X;
		final double WINDOW_DEFAULT_Y;
		final double WINDOW_DEFAULT_WIDTH;
		final double WINDOW_DEFAULT_HEIGHT;

		WINDOW_DEFAULT_X =  primaryScreenBounds.getWidth() / 10;
		WINDOW_DEFAULT_WIDTH = primaryScreenBounds.getWidth()/2 < 520 ? primaryScreenBounds.getWidth() : 520;
		WINDOW_DEFAULT_Y = primaryScreenBounds.getHeight() / 4;
		WINDOW_DEFAULT_HEIGHT = WINDOW_DEFAULT_WIDTH;

		double x = userPrefs.getDouble("stage.x", WINDOW_DEFAULT_X);
		double y = userPrefs.getDouble("stage.y", WINDOW_DEFAULT_Y);
		double w = userPrefs.getDouble("stage.width", WINDOW_DEFAULT_WIDTH);
		double h = userPrefs.getDouble("stage.height", WINDOW_DEFAULT_HEIGHT);
		if(w <= WINDOW_LOADING_WIDTH)
			w = WINDOW_DEFAULT_WIDTH;
		if(h <= WINDOW_LOADING_HEIGHT)
			h = WINDOW_DEFAULT_HEIGHT;

		primaryStage.setX(x);
		primaryStage.setY(y);
		primaryStage.setWidth(w);
		primaryStage.setHeight(h);

		LOG.info("Handyfinder is ready : " + AppStartup.homeUrl);
		return webView;
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

	private static enum TITLE {
		BEFORE_LOADING("Loading..."), AFTER_LOADING("Your Assistant");
		String title;

		TITLE(String title) {
			this.title = title;
		};

		String getTitle() {
			return this.title;
		}
	}
}
