package com.qwefgh90.io.handyfinder.springweb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "auth" })
public class CommandDto {
	public enum COMMAND{
		START_INDEXING
		
	}
	
//	private MessageAuthDto auth;
	private int command;

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		this.command = command;
	}
//
//	public MessageAuthDto getAuth() {
//		return auth;
//	}
//
//	public void setAuth(MessageAuthDto auth) {
//		this.auth = auth;
//	}
//	
	
}
