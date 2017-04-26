package io.github.qwefgh90.handyfinder.springweb.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "receiver" })
public class DocumentContentMessage implements IMessage{
	private String pathString;
	private String highlightTag;
	private IMessageSender receiver;
	public String getPathString() {
		return pathString;
	}
	public void setPathString(String pathString) {
		this.pathString = pathString;
	}
	public String getHighlightTag() {
		return highlightTag;
	}
	public void setHighlightTag(String highlightTag) {
		this.highlightTag = highlightTag;
	}
	public DocumentContentMessage(IMessageSender receiver, String pathString, String highlightTag) {
		this.pathString = pathString;
		this.highlightTag = highlightTag;
		this.receiver = receiver;
	}
	@Override
	public void send() {
		// TODO Auto-generated method stub
		receiver.sendToDocumentContent(this);
	}
	
	
}
