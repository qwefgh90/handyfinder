package com.qwefgh90.io.handyfinder.springweb.websocket;

public interface ICommandReceiver {
	void sendToProgressChannel(ProgressCommand obj);
	void startProgressChannel(ProgressCommand obj);
	void terminateProgressChannel(ProgressCommand obj);
	public void sendSelectedDirectoryChannel(GUICommand obj);
}
