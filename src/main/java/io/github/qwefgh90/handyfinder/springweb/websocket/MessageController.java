package io.github.qwefgh90.handyfinder.springweb.websocket;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.qwefgh90.handyfinder.gui.AppStartup;
import io.github.qwefgh90.handyfinder.gui.GUIApplication;
import io.github.qwefgh90.handyfinder.springweb.websocket.GUIMessage.COMMAND;
import io.github.qwefgh90.handyfinder.springweb.websocket.UpdateSummaryMessage.STATE;
import javafx.stage.DirectoryChooser;
import javafx.application.*;
/**
 * UI Command API
 * 
 * @author choechangwon
 *
 */
@Service
public class MessageController {
	private final static Logger LOG = LoggerFactory.getLogger(MessageController.class);
	@Autowired
	IMessageSender receiver;

	ProgressMessage progress;

	public void startProgress(int totalProcessCount) {
		progress = ProgressMessage.createMessage(receiver
				, ProgressMessage.STATE.START
				, 0
				, Paths.get("Ready to Run")
				, totalProcessCount);
		progress.send();
	}

	public void terminateProgress(int totalProcessCount) {
		progress = ProgressMessage.createMessage(receiver
				, ProgressMessage.STATE.TERMINATE
				, -1
				, null
				, totalProcessCount);
		progress.send();
	}

	public void updateProgress(int processIndex, Path processPath, int totalProcessCount) {

		progress = ProgressMessage.createMessage(receiver
				, ProgressMessage.STATE.PROGRESS
				, processIndex
				, processPath
				, totalProcessCount);
		progress.send();
	}

	public void openAndSendSelectedDirectory() {
		try {
			GUIApplication.getSingleton().get().openAndSelectDirectory((dir) -> {
				GUIMessage comm = GUIMessage.createObjectForDirectory(receiver, dir);
				comm.send();
			});
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
		}
	}

	public void sendDocumentContent(String pathString, String content){
		DocumentContentMessage comm = new DocumentContentMessage(receiver, pathString, content);
		comm.send();
	}
	
	public void startUpdateSummary(){
		UpdateSummaryMessage comm = UpdateSummaryMessage.createCommand(receiver
				,STATE.START
				,-1
				,-1
				,-1);
		comm.send();
	}
	
	public void terminateUpdateSummary(int countOfDeleted, int countOfExcluded, int countOfModified){
		UpdateSummaryMessage comm = UpdateSummaryMessage.createCommand(receiver
				,STATE.TERMINATE
				,countOfDeleted
				,countOfExcluded
				,countOfModified);
		comm.send();
	}
	
}
