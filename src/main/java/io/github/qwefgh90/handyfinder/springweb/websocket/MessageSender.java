package io.github.qwefgh90.handyfinder.springweb.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageSender implements IMessageSender {

	private final static Logger LOG = LoggerFactory.getLogger(MessageSender.class);
	@Autowired
	SimpMessagingTemplate messaging;

	@Override
	public void sendToProgressChannel(IMessage obj) {
		messaging.convertAndSend("/index/progress", obj);
	}

	@Override
	public void sendSelectedDirectoryChannel(String pathString) {
		messaging.convertAndSend("/gui/directory", pathString);
	}

	@Override
	public void sendToUpdateSummary(IMessage obj) {
		messaging.convertAndSend("/index/update", obj);
	}

	@Override
	public void sendToDocumentContent(IMessage obj) {
		messaging.convertAndSend("/search/document", obj);
	}
}
