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
			final DirectoryChooser directoryChooser = new DirectoryChooser();
			final File selectedDirectory = directoryChooser.showDialog(AppStartupConfig.primaryStage);
			if (selectedDirectory != null) {
				return selectedDirectory.getAbsolutePath();
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
}
