package io.github.qwefgh90.handyfinder.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ILuceneHandlerMimeOptionView {
	boolean isAllowMime(String mime);
	void setGlob(String glob, boolean b);
	void initGlobTrue();
	public Map<String, Boolean> getImmutableGlobMap();
	Set<String> getGlobSet(String mime) ;
	void updateGlobPropertiesFile() throws FileNotFoundException, IOException;
}
