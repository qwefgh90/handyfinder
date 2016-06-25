package io.github.qwefgh90.handyfinder.springweb.websocket;

public interface ICommandReceiver {
	void sendToProgressChannel(ProgressCommand obj);
	void startProgressChannel(ProgressCommand obj);
	void terminateProgressChannel(ProgressCommand obj);
	void sendSelectedDirectoryChannel(String pathString);
	void sendToUpdateSummary(UpdateSummaryCommand obj);
	void sendToDocumentContent(DocumentContentCommand obj);
}
