package io.github.qwefgh90.handyfinder.springweb.websocket;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.springweb.websocket.GUICommand.COMMAND;
import io.github.qwefgh90.handyfinder.springweb.websocket.UpdateSummaryCommand.STATE;
import javafx.stage.DirectoryChooser;
import javafx.application.*;
/**
 * UI Command API
 * 
 * @author choechangwon
 *
 */
@Service
public class CommandInvoker {

	private final static Logger LOG = LoggerFactory.getLogger(CommandInvoker.class);
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

	public void openAndSendSelectedDirectory() {
		Platform.runLater(() -> {
			String result = "";
			try {
				final DirectoryChooser directoryChooser = new DirectoryChooser();
				final File selectedDirectory = directoryChooser.showDialog(AppStartupConfig.primaryStage);
				if (selectedDirectory != null) {
					result = selectedDirectory.getAbsolutePath();
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
			GUICommand comm = GUICommand.createObjectForDirectory(receiver, result);
			comm.execute();
		});
	}

	public void startUpdateSummary(){
		UpdateSummaryCommand comm = UpdateSummaryCommand.getInstance(receiver);
		comm.setState(STATE.START);
		comm.setCountOfDeleted(-1);
		comm.setCountOfExcluded(-1);
		comm.setCountOfModified(-1);
		comm.execute();
	}
	
	public void terminateUpdateSummary(int countOfDeleted, int countOfExcluded, int countOfModified){
		UpdateSummaryCommand comm = UpdateSummaryCommand.getInstance(receiver);
		comm.setState(STATE.TERMINATE);
		comm.setCountOfDeleted(countOfDeleted);
		comm.setCountOfExcluded(countOfExcluded);
		comm.setCountOfModified(countOfModified);
		comm.execute();
	}
	
}
