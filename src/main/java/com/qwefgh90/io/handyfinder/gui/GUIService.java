package com.qwefgh90.io.handyfinder.gui;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.stage.DirectoryChooser;

public class GUIService {

	private final static Logger LOG = LoggerFactory.getLogger(GUIService.class);
	public GUIService() {
	}

	// sync function
	public int sum(int a, int b) {
		return a + b;
	}

	public String openDialogAndSelectDirectory() {
		try {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			File selectedDirectory = directoryChooser.showDialog(AppStartupConfig.primaryStage);
			AppStartupConfig.primaryStage.show();
			if (selectedDirectory != null) {
				return selectedDirectory.getAbsolutePath();
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
}
