package io.github.qwefgh90.handyfinder.springweb.websocket;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUICommand implements ICommand {
	private final static Logger LOG = LoggerFactory.getLogger(GUICommand.class);

	public enum COMMAND {
		OPEN_DIRECTORY
	}

	private COMMAND com;
	private ICommandReceiver receiver;
	private String pathString;

	private GUICommand() {}
	
	public static GUICommand createObjectForDirectory(ICommandReceiver receiver, String pathString){
		GUICommand gui = new GUICommand();
		gui.setCom(COMMAND.OPEN_DIRECTORY);
		gui.setReceiver(receiver);
		gui.setPathString(pathString);
		return gui;
	}

	@Override
	public void execute() {
		switch (this.com) {
		case OPEN_DIRECTORY: {
			receiver.sendSelectedDirectoryChannel(this.getPathString());
			break;
		}
		}
	}

	public COMMAND getCom() {
		return com;
	}

	public void setCom(COMMAND com) {
		this.com = com;
	}

	public ICommandReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(ICommandReceiver receiver) {
		this.receiver = receiver;
	}

	public String getPathString() {
		return pathString;
	}

	public void setPathString(String pathString) {
		this.pathString = pathString;
	}

}
