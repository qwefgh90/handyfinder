package intergration.websocket.test;

import static org.junit.Assert.fail;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.repository.MetaRespository;
import com.qwefgh90.io.handyfinder.springweb.service.RootService;
import com.qwefgh90.io.handyfinder.springweb.websocket.ProgressCommand;
import com.qwefgh90.io.handyfinder.springweb.websocket.ProgressCommand.STATE;

public class WebsockTest {
	private final static Logger LOG = LoggerFactory.getLogger(WebsockTest.class);
	
	RootService rootService;
	MetaRespository metaRepo;
	List<Directory> list = new ArrayList<>();

	@Before
	public void setup() throws LifecycleException, ServletException, IOException, URISyntaxException, SQLException, ParseException {
		AppStartupConfig.main(new String[] { "--no-gui"});

		rootService = AppStartupConfig.getBean(RootService.class);
		metaRepo = AppStartupConfig.getBean(MetaRespository.class);

		Directory dir = new Directory();
		dir.setRecursively(true);
		dir.setUsed(true);
		dir.setPathString(Paths.get(new ClassPathResource("").getFile().getAbsolutePath()).resolve("index-test-files")
				.toAbsolutePath().toString());
		list.add(dir);
		rootService.updateDirectories(list);
		LOG.trace("test directory to be updated: "+dir.getPathString());
	}

	@After
	public void clean() throws Exception {
		rootService.closeAppLucene();
		metaRepo.deleteDirectories();
		AppStartupConfig.terminateApp();
	}

	@Test
	public void connect() throws InterruptedException, SQLException {
		List<Directory> list = rootService.getDirectories();
		LOG.info("list size : " + String.valueOf(list.size()));

		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		StompSessionHandler handler = new Handler(failure, latch);

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		List<Transport> transports = new ArrayList<>();
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		RestTemplateXhrTransport xhrTransport = new RestTemplateXhrTransport(new RestTemplate());
		xhrTransport.setRequestHeaders(headers);
		transports.add(xhrTransport);

		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();

		SockJsClient sockJsClient = new SockJsClient(transports);
		WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		stompClient.setTaskScheduler(taskScheduler);
		stompClient.setDefaultHeartbeat(new long[] { 0, 0 });
		stompClient.connect("ws://" + AppStartupConfig.address + ":" + AppStartupConfig.port + "/endpoint", headers,
				handler, AppStartupConfig.port);

		if (failure.get() != null) {
			throw new AssertionError("", failure.get());
		}

		if (!latch.await(120, TimeUnit.SECONDS)) {
			fail("not received");
		}
	}

	class Handler extends AbstractTestSessionHandler {
		AtomicReference<Throwable> failure;
		CountDownLatch latch;
		StompSession session;

		public Handler(AtomicReference<Throwable> failure, CountDownLatch latch) {
			super(failure);
			this.failure = failure;
			this.latch = latch;
		}

		public void sendMsg() {
			try {
				session.send("/handyfinder/hello", "hello");
				session.send("/handyfinder/command/index/start","");
			} catch (Exception e) {
				LOG.info(ExceptionUtils.getStackTrace(e));
			}
		}

		@Override
		public void afterConnected(final StompSession session, StompHeaders connectedHeaders) {
			this.session = session;

			session.subscribe("/progress/single", new StompFrameHandler() {
				@Override
				public Type getPayloadType(StompHeaders headers) {
					return ProgressCommand.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					ProgressCommand json = (ProgressCommand) payload;
					LOG.info("Got " + ToStringBuilder.reflectionToString(json));
					if (json.getState() == STATE.PROGRESS.TERMINATE) {

						 session.disconnect();
						 try {
						 Thread.sleep(100);
						 } catch (InterruptedException e) {
						 LOG.info(ExceptionUtils.getStackTrace(e));
						 }
						 latch.countDown();
					}
					try {
					} catch (Throwable t) {
						failure.set(t);
					} finally {

					}
				}
			});
			session.subscribe("/test/hi", new StompFrameHandler() {
				@Override
				public Type getPayloadType(StompHeaders headers) {
					return String.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					String json = (String) payload;
					LOG.info("Got " + json);
					assertTrue("hi".equals(json));
					try {
					} catch (Throwable t) {
						failure.set(t);
					} finally {
					}
				}
			});
			
			sendMsg();//send test msg
		}
	};

	private static abstract class AbstractTestSessionHandler extends StompSessionHandlerAdapter {

		private final AtomicReference<Throwable> failure;

		public AbstractTestSessionHandler(AtomicReference<Throwable> failure) {
			this.failure = failure;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			LOG.error("STOMP ERROR frame: " + headers.toString());
			this.failure.set(new Exception(headers.toString()));
		}

		@Override
		public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
			LOG.error("Handler exception", ex);
			this.failure.set(ex);
		}

		@Override
		public void handleTransportError(StompSession session, Throwable ex) {
			LOG.error("Transport failure", ex);
			this.failure.set(ex);
		}
	}
}