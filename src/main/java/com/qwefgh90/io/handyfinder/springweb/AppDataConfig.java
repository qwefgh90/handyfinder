package com.qwefgh90.io.handyfinder.springweb;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.sax.TikaMimeXmlObject;
import com.qwefgh90.io.handyfinder.sax.TikaMimeXmlObject.TikaMimeXmlObjectFactory;
import com.qwefgh90.io.handyfinder.springweb.repository.GlobalAppData.GlobalAppDataView;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;

@Configuration
public class AppDataConfig {

	@Bean
	public LuceneHandler luceneHandler(CommandInvoker invoker) {
		return LuceneHandler
				.getInstance(AppStartupConfig.pathForIndex, invoker);
	}

	@Bean
	public GlobalAppDataView globalAppDataView() {
		GlobalAppDataView view = new GlobalAppDataView();
		return view;
	}

	@Bean
	public TikaMimeXmlObject tikaMimeXmlObject() throws ParserConfigurationException, SAXException, IOException {
		return TikaMimeXmlObjectFactory.createInstanceFromXml(AppStartupConfig.tikaXmlFilePath.toAbsolutePath().toString());
	}
}
