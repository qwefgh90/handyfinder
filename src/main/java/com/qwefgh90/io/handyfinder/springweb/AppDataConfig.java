package com.qwefgh90.io.handyfinder.springweb;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.gui.GlobalAppDataView;
import com.qwefgh90.io.handyfinder.lucene.LuceneHandler;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;
import com.qwefgh90.io.handyfinder.tikamime.TikaMimeXmlObject;
import com.qwefgh90.io.handyfinder.tikamime.TikaMimeXmlObject.TikaMimeXmlObjectFactory;

@Configuration
public class AppDataConfig {

	@Bean
	public LuceneHandler luceneHandler(CommandInvoker invoker) {
		return LuceneHandler
				.getInstance(AppStartupConfig.pathForIndex, invoker);
	}

	@Bean
	public GlobalAppDataView globalAppDataView() {
		GlobalAppDataView view =  GlobalAppDataView.getInstance();
		return view;
	}

	@Bean
	public TikaMimeXmlObject tikaMimeXmlObject() throws ParserConfigurationException, SAXException, IOException {
		return TikaMimeXmlObjectFactory.getInstanceFromXml(AppStartupConfig.tikaXmlFilePath.toAbsolutePath().toString());
	}
}
