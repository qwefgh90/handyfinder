package io.github.qwefgh90.handyfinder.springweb.websocket;

public interface ICommandReceiver {
	void sendToProgressChannel(ProgressCommand obj);
	void startProgressChannel(ProgressCommand obj);
	void terminateProgressChannel(ProgressCommand obj);
	void sendSelectedDirectoryChannel(GUICommand obj);
	void sendToUpdateSummary(UpdateSummaryCommand obj);
}
