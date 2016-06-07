package com.qwefgh90.io.handyfinder.lucene;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.lucene.LuceneHandler;
import com.qwefgh90.io.handyfinder.springweb.RootContext;
import com.qwefgh90.io.handyfinder.springweb.ServletContextTest;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class })
public class SearchTest {
	private final static Logger LOG = LoggerFactory.getLogger(SearchTest.class);
	@Autowired
	WebApplicationContext wac;
	MockMvc mvc;
	
	@Autowired
	CommandInvoker invoker;
	
	LuceneHandler handler;
	static {
		try {
			AppStartupConfig.parseArguments(new String[]{"--no-gui"});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Before
	public void setup() throws IOException{
		handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invoker);
		handler.indexDirectory(AppStartupConfig.deployedPath.resolve("index-test-files"), true);
		
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}
	
	@After
	public void clean() throws IOException{
		handler.closeResources();
	}
	
	@Test
	public void searchTest() throws Exception{
		mvc.perform(get("/search").contentType(MediaType.APPLICATION_JSON_UTF8).param("keyword", "자바 고언어 파이썬"))
		.andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());
	}
}
