package com.qwefgh90.io.handyfinder.springweb;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;

public class ContainerInitializer extends
		AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		//root-context.xml
		return new Class<?>[]{RootContext.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		//myservlet-context.xml
		return new Class<?>[]{ServletContext.class, RootWebSocketConfig.class};
	}

	@Override
	protected String[] getServletMappings() {
		//root url
		return new String[]{"/"}; 
	}

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		// TODO Auto-generated method stub
		WebApplicationContext context = super.createRootApplicationContext();
		AppStartupConfig.setRootAppContext(context);
		return context;
	}

	@Override
	protected WebApplicationContext createServletApplicationContext() {
		// TODO Auto-generated method stub
		WebApplicationContext context = super.createServletApplicationContext();
		AppStartupConfig.setServletAppContext(context);
		return context;
	}

}
