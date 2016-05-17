package com.qwefgh90.io.handyfinder.gui;

import java.io.File;

import javafx.stage.DirectoryChooser;

public class GUIService {

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
