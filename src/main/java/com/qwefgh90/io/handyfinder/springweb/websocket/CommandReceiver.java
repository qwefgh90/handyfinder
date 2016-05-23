package com.qwefgh90.io.handyfinder.springweb.websocket;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.websocket.GUICommand.COMMAND;

import javafx.stage.DirectoryChooser;

@Service
public class CommandReceiver implements ICommandReceiver {

	private final static Logger LOG = LoggerFactory.getLogger(CommandReceiver.class);
	@Autowired
	SimpMessagingTemplate messaging;

	@Override
	public void sendToProgressChannel(ProgressCommand obj) {
		messaging.convertAndSend("/progress/single", obj);
	}

	@Override
	public void startProgressChannel(ProgressCommand obj) {
		messaging.convertAndSend("/progress/single", obj);

	}

	@Override
	public void terminateProgressChannel(ProgressCommand obj) {
		messaging.convertAndSend("/progress/single", obj);

	}

	@Override
	public void sendSelectedDirectoryChannel(GUICommand obj) {
		if (obj.getCom() == COMMAND.OPEN_DIRECTORY) {
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
			messaging.convertAndSend("/gui/directory", result);
		}
	}
}
