package com.qwefgh90.io.handyfinder.springweb.websocket;

import java.nio.file.Path;

public class GUICommand implements ICommand{
	public enum COMMAND{
		OPEN_DIRECTORY
	}
	
	private COMMAND com;
	private ICommandReceiver receiver;
	private String pathString;
	
	public GUICommand(ICommandReceiver receiver, COMMAND command, String pathString){
		this.com = command;
		this.receiver = receiver;
		this.pathString = pathString;
	}

	@Override
	public void execute() {
		receiver.sendSelectedDirectoryChannel(this);
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
