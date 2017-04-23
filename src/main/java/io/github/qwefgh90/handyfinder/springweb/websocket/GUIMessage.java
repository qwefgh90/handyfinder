package io.github.qwefgh90.handyfinder.springweb.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIMessage implements IMessage {
	private final static Logger LOG = LoggerFactory.getLogger(GUIMessage.class);

	public enum COMMAND {
		OPEN_DIRECTORY
	}

	private COMMAND com;
	private IMessageSender receiver;
	private String pathString;

	private GUIMessage() {}
	
	public static GUIMessage createObjectForDirectory(IMessageSender sender, String pathString){
		GUIMessage gui = new GUIMessage();
		gui.setCom(COMMAND.OPEN_DIRECTORY);
		gui.setReceiver(sender);
		gui.setPathString(pathString);
		return gui;
	}

	@Override
	public void send() {
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

	public IMessageSender getReceiver() {
		return receiver;
	}

	public void setReceiver(IMessageSender receiver) {
		this.receiver = receiver;
	}

	public String getPathString() {
		return pathString;
	}

	public void setPathString(String pathString) {
		this.pathString = pathString;
	}

}
