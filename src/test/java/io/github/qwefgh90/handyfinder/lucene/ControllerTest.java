package io.github.qwefgh90.handyfinder.lucene;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.springweb.config.AppDataConfig;
import io.github.qwefgh90.handyfinder.springweb.config.RootContext;
import io.github.qwefgh90.handyfinder.springweb.config.ServletContextTest;
import io.github.qwefgh90.handyfinder.springweb.model.DocumentDto;
import io.github.qwefgh90.handyfinder.springweb.model.OptionDto;
import io.github.qwefgh90.handyfinder.springweb.model.SupportTypeDto;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { ServletContextTest.class, RootContext.class,
		AppDataConfig.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ControllerTest {
	private final static Logger LOG = LoggerFactory
			.getLogger(ControllerTest.class);
	@Autowired
	WebApplicationContext wac;

	@Autowired
	CommandInvoker invoker;

	@Autowired
	LuceneHandler handler;

	@Autowired
	BasicOption basicOption;
	
	@Autowired
	MimeOption mimeOption;

	MockMvc mvc;
	
	final ObjectMapper om = new ObjectMapper();
	final List<Directory> indexDirList = new ArrayList<>();
	
	@Before
	public void setup() throws IOException {
		basicOption.setLimitCountOfResult(100);
		basicOption.setMaximumDocumentMBSize(100);
		mimeOption.initGlobTrue();
		handler.deleteAllIndexesFromFileSystem();
		
		final Path indexPath = AppStartupConfig.deployedPath.resolve("index-test-files");
		
		Directory testFileiDir = new Directory();
		testFileiDir.setRecursively(true);
		testFileiDir.setUsed(true);
		testFileiDir.setPathString(indexPath.toAbsolutePath().toString());;
		
		indexDirList.add(testFileiDir);
		indexDirList.forEach(dir -> {
			basicOption.addDirectory(dir);
		});
		
		handler.indexDirectory(
				AppStartupConfig.deployedPath.resolve("index-test-files"), true);
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@After
	public void clean() throws IOException {
	}

	@Test
	public void optionTest() throws Exception {
		MvcResult mvcResult = mvc
				.perform(
						get("/options").contentType(
								MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andReturn();

		String responseString = mvcResult.getResponse().getContentAsString();
		OptionDto option1 = om.readValue(responseString, OptionDto.class);
		option1.setLimitCountOfResult(2);
		option1.setMaximumDocumentMBSize(1);

		String modifiedJsonString = om.writeValueAsString(option1);
		OptionDto option2 = om.readValue(modifiedJsonString, OptionDto.class);
		assertThat(option1.getLimitCountOfResult(), Matchers.is(option2
				.getLimitCountOfResult()));
		assertThat(option1.getMaximumDocumentMBSize(), Matchers.is(option2
				.getMaximumDocumentMBSize()));
	}

	@Test
	public void searchTest() throws Exception {
		MvcResult mvcResult = mvc.perform(
				get("/documents").contentType(MediaType.APPLICATION_JSON_UTF8)
						.param("keyword", "javageeks"))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andReturn();
		
		String responseString = mvcResult.getResponse().getContentAsString();
		List<DocumentDto> list = om.readValue(responseString, List.class);
		Assert.assertThat(list.size(), Matchers.is(5));
	}

	@Test
	public void searchAllDocumentTest() throws Exception {
		MvcResult mvcResult = mvc.perform(
				get("/documents").contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andReturn();
		
		String responseString = mvcResult.getResponse().getContentAsString();
		List<String> list = om.readValue(responseString, List.class);
		Assert.assertThat(list.size(), Matchers.is(12));
	}

	@Test
	public void supportTypeTest() throws Exception {
		MvcResult result = mvc
				.perform(
						get("/supportTypes").contentType(
								MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$", hasSize(greaterThan(1000))))
				.andExpect(jsonPath("$[0].type", startsWith("*")))
				.andExpect(jsonPath("$[1].type", startsWith("*"))).andReturn();

		String responseString = result.getResponse().getContentAsString();

		// response to json
		JSONArray arr = (JSONArray) new JSONParser().parse(responseString);
		SupportTypeDto dto = new SupportTypeDto();
		dto.setType(((JSONObject) arr.get(0)).get("type").toString());
		dto.setUsed(true);
		String json = om.writeValueAsString(dto);

		// update type -> true
		mvc.perform(post("/supportType").contentType(MediaType.APPLICATION_JSON_UTF8).content(json))
				.andExpect(status().isOk());

		// check -> true
		mvc.perform(get("/supportTypes").contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$[0].type", Matchers.is(dto.getType())))
				.andExpect(jsonPath("$[0].used", Matchers.is(Boolean.valueOf(dto.isUsed()))));

		dto.setUsed(false);
		json = om.writeValueAsString(dto);
		// update type -> false
		mvc.perform(
				post("/supportType").contentType(
						MediaType.APPLICATION_JSON_UTF8).content(json))
				.andExpect(status().isOk());

		// final check -> false
		mvc.perform(
				get("/supportTypes").contentType(
						MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$[0].type", Matchers.is(dto.getType())))
				.andExpect(jsonPath("$[0].used", Matchers.is(Boolean.valueOf(dto.isUsed()))));

	}
	
	@Test
	public void supportTypeTest2() throws Exception {
		MvcResult result = mvc
				.perform(get("/supportTypes").contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andReturn();

		String responseString = result.getResponse().getContentAsString();

		List<SupportTypeDto> list = new ArrayList<SupportTypeDto>();
		// response to json
		JSONArray arr = (JSONArray) new JSONParser().parse(responseString);
		for (int i = 0; i < arr.size(); i++) {
			SupportTypeDto dto = new SupportTypeDto();
			dto.setType(((JSONObject) arr.get(i)).get("type").toString());
			dto.setUsed(false);
			list.add(dto);
		}
		String json = om.writeValueAsString(list);

		mvc.perform(post("/supportTypes").contentType(MediaType.APPLICATION_JSON_UTF8).content(json))
				.andExpect(status().isOk());
		
		result = mvc
				.perform(get("/supportTypes").contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andReturn();

		responseString = result.getResponse().getContentAsString();
		arr = (JSONArray) new JSONParser().parse(responseString);
		for (int i = 0; i < arr.size(); i++) {
			assertFalse(Boolean.valueOf(((JSONObject) arr.get(i)).get("used").toString()));
		}
	}
}
