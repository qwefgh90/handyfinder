package com.qwefgh90.io.handyfinder.springweb.websocket;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.websocket.GUICommand.COMMAND;

import javafx.stage.DirectoryChooser;

/**
 * UI Command API
 * 
 * @author choechangwon
 *
 */
@Service
public class CommandInvoker {

	@Autowired
	ICommandReceiver receiver;

	ProgressCommand progress;

	synchronized public void startProgress(int totalProcessCount) {
		progress = ProgressCommand.getInstance(receiver);
		progress.setState(ProgressCommand.STATE.START);
		progress.setProcessIndex(0);
		progress.setProcessPath(Paths.get("Ready to Run"));
		progress.setTotalProcessCount(totalProcessCount);
		progress.execute();
	}

	synchronized public void terminateProgress(int totalProcessCount) {
		progress = ProgressCommand.getInstance(receiver);
		progress.setState(ProgressCommand.STATE.TERMINATE);
		progress.setProcessIndex(-1);
		progress.setProcessPath(null);
		progress.setTotalProcessCount(totalProcessCount);
		progress.execute();
	}

	synchronized public void updateProgress(int processIndex, Path processPath, int totalProcessCount) {
		progress = ProgressCommand.getInstance(receiver);
		progress.setState(ProgressCommand.STATE.PROGRESS);
		progress.setProcessIndex(processIndex);
		progress.setProcessPath(processPath);
		progress.setTotalProcessCount(totalProcessCount);
		progress.execute();
	}

	public void sendDirectory(String pathString) {
		GUICommand comm = new GUICommand(receiver, COMMAND.OPEN_DIRECTORY, pathString);
		comm.execute();
	}

}
