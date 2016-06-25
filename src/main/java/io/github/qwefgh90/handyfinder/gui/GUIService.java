package io.github.qwefgh90.handyfinder.gui;

import java.io.File;

import javafx.stage.DirectoryChooser;
import javafx.application.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIService {

	private final static Logger LOG = LoggerFactory.getLogger(GUIService.class);
	public GUIService() {
	}

	// sync function
	public int sum(int a, int b) {
		return a + b;
	}

	public String openDialogAndSelectDirectory() {
		Platform.runLater(() -> {
			try {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				File selectedDirectory = directoryChooser.showDialog(AppStartupConfig.primaryStage);
				AppStartupConfig.primaryStage.show();
				if (selectedDirectory != null) {
					LOG.info("path : " + selectedDirectory.getAbsolutePath());
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
			//return "";
		});
		return "";
	}
}
