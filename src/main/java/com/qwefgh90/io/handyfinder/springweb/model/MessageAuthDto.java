package com.qwefgh90.io.handyfinder.springweb.model;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

@Deprecated
public class MessageAuthDto {

	private String sessionId;
	private MessageHeaders header;
	
	public static MessageAuthDto createInstance(String sessionId) {
		MessageAuthDto dto = new MessageAuthDto();
		dto.sessionId = sessionId;
		dto.header = createHeaders(dto.sessionId);
		return dto;
	}

	public MessageHeaders getHeader() {
		return header;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * stomp header for routing
	 * @param sessionId
	 * @return
	 */
	private static MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
