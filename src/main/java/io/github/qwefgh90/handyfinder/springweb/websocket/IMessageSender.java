package io.github.qwefgh90.handyfinder.springweb.websocket;

public interface IMessageSender {
	void sendToProgressChannel(IMessage obj);
	void sendSelectedDirectoryChannel(String pathString);
	void sendToUpdateSummary(IMessage obj);
	void sendToDocumentContent(IMessage obj);
}
