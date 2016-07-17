package io.github.qwefgh90.handyfinder.gui;

import static io.github.qwefgh90.handyfinder.gui.Java2JavascriptUtils.connectBackendObject;
import io.github.qwefgh90.handyfinder.exception.TomcatInitFailException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
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
	private final static Logger LOG = LoggerFactory
			.getLogger(GUIApplication.class);
	
	private final static GUIApplication app = new GUIApplication();
	
	public void start(String[] args){
		launch(args);
	}
	public static GUIApplication getSingleton() {
		return app;
	}
	
	private final double WINDOW_LOADING_WIDTH = 300;
	private final double WINDOW_LOADING_HEIGHT = 330;
	

	private WebView currentView = null;
	private Tomcat tomcat;
	private Stage primaryStage;
	
	private boolean stopped = false;
	public boolean isStop(){ return stopped; }

	private ExecutorService webAppThread = Executors.newSingleThreadExecutor();
	public ExecutorService getWebAppThread() { return webAppThread; }

	private Task<Boolean> webApp = new Task<Boolean>() {
		@Override
		protected Boolean call() throws Exception {
			try {
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server initialzing..."));

				Tomcat tomcat = new Tomcat();
				GUIApplication.this.tomcat = tomcat;
				tomcat.getConnector().setAttribute("address",
						AppStartupConfig.address);
				tomcat.getConnector().setAttribute("port",
						AppStartupConfig.port);

				Context context = tomcat.addWebapp("",
						AppStartupConfig.pathForAppdata.toAbsolutePath()
								.toString());
				// https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html#addWebapp(org.apache.catalina.Host,%20java.lang.String,%20java.lang.String)

				context.setJarScanner(new FastJarScanner());
				context.addWelcomeFile(AppStartupConfig.REDIRECT_PAGE);
				tomcat.init();
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server startup..."));
				tomcat.start();
				Platform.runLater(() -> setLoadingParagraphBeforeLoading("server health checking..."));
				healthCheck();
				Platform.runLater(() -> showUI(GUIApplication.this::setWebviewAfterLoading));
				LOG.info("tomcat started completely : "
						+ AppStartupConfig.homeUrl);
			} catch (Exception e) {
				LOG.error(e.toString());
				return false;
			}
			return true;
		}
	};

	public void healthCheck() throws TomcatInitFailException {
		String strUrl = "http://" + AppStartupConfig.address + ":"
				+ AppStartupConfig.port + "/health";

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
		primaryStage.setOnCloseRequest(event -> {
			LOG.info("javafx onCloseRequest()");
			Preferences userPrefs = Preferences
					.userNodeForPackage(AppStartupConfig.class);
			userPrefs.putDouble("stage.x", primaryStage.getX());
			userPrefs.putDouble("stage.y", primaryStage.getY());
			userPrefs.putDouble("stage.width", primaryStage.getWidth());
			userPrefs.putDouble("stage.height", primaryStage.getHeight());
			Platform.runLater(() -> setLoadingParagraphBeforeLoading("Handyfinder Stopping..."));

			//wait for web application startup
			if (!webAppThread.isTerminated()){
				ExecutorService es = Executors.newSingleThreadExecutor();
				es.submit(() -> {
					LOG.info("javafx wait for tomcat startup thread");
					try {
						boolean result = webAppThread.awaitTermination(20, TimeUnit.SECONDS);
						if(result == false){
							LOG.warn("Web application startup spends more 20 seconds. force terminate all threads.");
							webAppThread.shutdownNow();
						}
					} catch (Exception e) {
						LOG.error(ExceptionUtils.getStackTrace(e));
					} finally {
						Platform.exit(); //direct exit
						LOG.info("javafx Platform.exit()");
					}
				});
				es.shutdown();
				event.consume();
			}
		});
		
		showUI(this::setWebviewBeforeLoading);
		Platform.runLater(() -> setLoadingParagraphBeforeLoading("Graphic initialzing..."));
		LOG.info("javafx ui is initialized ");
		
		webAppThread.submit(webApp);
		webAppThread.shutdown();
	}
	
	@Override
	public void init() throws Exception {
		super.init();
		LOG.info("javafx init()");
	}
	
	@Override
	public void stop() throws Exception {
		final Callable<Boolean> doServerStop = () -> {
			if(stopped == true)
				return true;
			try {
				stopped = true;
				tomcat.stop();
				LOG.info("tomcat stop()");
			} catch (Exception e) {
				LOG.error(ExceptionUtils.getStackTrace(e));
				return false;
			}
			return true;
		};
		
		doServerStop.call();
		super.stop();
		LOG.info("javafx stop()");
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
				System.out.println(e.toString());
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
		if (!AppStartupConfig.getServerOnlyMode())
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

	private WebView setWebviewBeforeLoading() {
		// create the JavaFX webview
		final WebView webView = new WebView();
		webView.getEngine().load(
				AppStartupConfig.class.getResource(
						AppStartupConfig.RESOURCE_LOADING_PAGE)
						.toExternalForm());
		Scene scene = new Scene(webView);
		
		primaryStage.setScene(scene);
		primaryStage.setTitle(TITLE.BEFORE_LOADING.getTitle());
		primaryStage.setWidth(WINDOW_LOADING_WIDTH);
		primaryStage.setHeight(WINDOW_LOADING_HEIGHT);
		primaryStage.centerOnScreen();
		return webView;
	}

	private WebView setWebviewAfterLoading() {
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
		webView.getEngine().load(AppStartupConfig.homeUrl);
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
		primaryStage.setTitle(TITLE.AFTER_LOADING.getTitle());

		Preferences userPrefs = Preferences
				.userNodeForPackage(AppStartupConfig.class);

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

		LOG.info("\nhandyfinder started completely " + "\n"
				+ "Webengine load: " + AppStartupConfig.homeUrl); // handyfinder
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
