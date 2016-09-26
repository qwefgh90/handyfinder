package io.github.qwefgh90.handyfinder.springweb.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "receiver" })
public class DocumentContentCommand implements ICommand{
	private String pathString;
	private String highlightTag;
	private ICommandReceiver receiver;
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
	public DocumentContentCommand(ICommandReceiver receiver, String pathString, String highlightTag) {
		this.pathString = pathString;
		this.highlightTag = highlightTag;
		this.receiver = receiver;
	}
	@Override
	public void execute() {
		// TODO Auto-generated method stub
		receiver.sendToDocumentContent(this);
	}
	
	
}
