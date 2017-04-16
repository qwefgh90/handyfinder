package io.github.qwefgh90.handyfinder.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class AppStartupUtil {
	private final static Logger LOG = LoggerFactory
			.getLogger(AppStartupUtil.class);

	public static String getFourDigitsNumber(){
		final Random random = new Random();
		return String.format("%d%d%d%d", Math.abs(random.nextInt() % 10)
				, Math.abs(random.nextInt() % 10)
				, Math.abs(random.nextInt() % 10)
				, Math.abs(random.nextInt() % 10));
	}
	
	/**
	 * jar files and Copy to destination. If a file exists, overwrite it.<br>
	 * relative reference - http://cs.dvc.edu/HowTo_ReadJars.html jar resource
	 * use forward slash (/)
	 * 
	 * @param jarPath
	 * @param resourceDirInJar
	 *            - "/config" or "/config/" or "config" or ""
	 * @param destinationRoot
	 * @throws URISyntaxException
	 * @throws IOException
	 * @author qwefgh90
	 */
	public static void copyDirectoryInJar(String jarPath,
			String resourceDirInJar, File destinationRoot, BiFunction<File, JarEntry, Boolean> destFileFilter)
			throws URISyntaxException, IOException {
		if(destFileFilter == null)
			destFileFilter = (file,entry) -> true;
		if (resourceDirInJar.startsWith("/")) { // jar url start with /
												// replace to jar
												// entry style which
												// is not start with
												// '/'
			resourceDirInJar = resourceDirInJar.substring(1);
		}
		if (resourceDirInJar.length() != 0
				&& resourceDirInJar.getBytes()[resourceDirInJar.length() - 1] != File.separator
						.getBytes()[0]) // add
										// rightmost
										// seperator
			resourceDirInJar = resourceDirInJar + "/";

		LOG.trace("package extract info : " + "\nFile.separator : "
				+ File.separator + "\nresourceDirInJar : " + resourceDirInJar
				+ "\njarPath : " + jarPath + "\ndestinationRoot"
				+ destinationRoot);

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		// loop entry
		while (entry != null) {
			LOG.trace("extract from java : " + entry.getName());
			if (entry.getName().startsWith(resourceDirInJar) // Directory in jar
					&& entry.getName().endsWith("/")) {
				LOG.trace("try to copy : " + entry.getName());
				Files.createDirectories(new File(destinationRoot, entry
						.getName()).toPath());
			} else if (entry.getName().startsWith(resourceDirInJar) // File in jar
					&& !entry.getName().endsWith("/")) {
				File destFile = new File(destinationRoot.getAbsolutePath(), entry
						.getName());
				
				if(!destFileFilter.apply(destFile, entry)){
					LOG.debug("skip copy : " + entry.getName());
					
				}else{
					LOG.debug("copy start : " + entry.getName());
					File tempFile = extractTempFile(getResourceInputstream(entry
							.getName()));
					FileUtils.copyFile(
							tempFile,
							new File(destinationRoot.getAbsolutePath(), entry
									.getName())); // copy
					tempFile.delete();
				}
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}
	

	/**
	 * Returns a free port number on localhost, or throw runtime exception if
	 * unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free
	 *         port
	 * @since 3.0
	 */
	public static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
		}
		throw new RuntimeException("no available ports.");
	}

	/**
	 * get exection jar path
	 * 
	 * @return String - path
	 */
	public static Path getCurrentBuildPath() {
		if (getResourcePath("") == null) {
			URI uri;
			try {
				uri = AppStartup.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
			return Paths.get(uri);
		} else {
			return Paths.get(getResourcePath(""));
		}
	}

	public static String[] allClassPath() {
		ArrayList<String> arr = new ArrayList<String>();
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			arr.add(url.getFile());
		}

		return arr.toArray(new String[arr.size()]);
	}

	/**
	 * Resource input stream in jar
	 * 
	 * @param resourceName
	 * @return
	 */
	public static InputStream getResourceInputstream(String resourceName) {
		return AppStartup.class.getClassLoader().getResourceAsStream(
				resourceName);
	}
	public static boolean isJarStart() {
		return getCurrentBuildPath().toString().endsWith(".jar");
	}

	/**
	 * Handle resourceName, whether File.separator exists or not
	 * 
	 * @param dirName
	 * @return
	 */
	public static String getResourcePath(String dirName) {
		try {
			return new ClassPathResource(dirName).getFile().getAbsolutePath();
		} catch (IOException e) {
			return null;
		}
	}
	
	public static void copyFileInJar(String jarPath, String resourcePathInJar,
			File destinationRootDir, BiFunction<File, JarEntry, Boolean> destFileFilter)
			throws URISyntaxException, IOException {
		if(destFileFilter == null)
			destFileFilter = (file, entry) -> true;
		if (resourcePathInJar.startsWith("/")) { // jar url start with /
													// replace to jar
													// entry getName() style which
													// is not start with
													// '/'
			resourcePathInJar = resourcePathInJar.substring(1);
		}

		FileInputStream fis = new FileInputStream(jarPath);
		JarInputStream jis = new JarInputStream(fis);
		JarEntry entry = jis.getNextJarEntry();
		// loop entry
		while (entry != null) {
			if (entry.getName().startsWith(resourcePathInJar) // File in jar
					&& entry.getName().getBytes()[entry.getName().length() - 1] != File.separator
							.getBytes()[0]) {
				int lastIndex = entry.getName().lastIndexOf("/");
				String entryName;
				if (lastIndex != -1)
					entryName = entry.getName().substring(lastIndex);
				else
					entryName = entry.getName();
				File destFile = new File(destinationRootDir.getAbsolutePath(),entryName);
				
				if(!destFileFilter.apply(destFile, entry)){
					LOG.debug("skip copy : " + entry.getName());
				}else{
					LOG.debug("copy start : " + entry.getName());
					File tempFile = extractTempFile(getResourceInputstream(entry
							.getName()));
					FileUtils.copyFile(
								tempFile,destFile); // copy from source file to destination file
					tempFile.delete();
				}
			}
			entry = jis.getNextJarEntry();
		}
		jis.close();
	}

	/**
	 * This method is responsible for extracting resource files from within the
	 * .jar to the temporary directory.
	 * 
	 * @param input
	 *            - returned value of
	 *            getClassLoader().getResourceAsStream("config/help.txt");
	 * @return Temp file created by stream
	 * @throws IOException
	 */
	public static File extractTempFile(InputStream input) throws IOException {
		File f = File.createTempFile("Thistempfile", "willdelete");
		FileOutputStream tempFileos = new FileOutputStream(f);
		byte[] byteArray = new byte[1024];
		int i;
		// While the input stream has bytes
		while ((i = input.read(byteArray)) > 0) {
			// Write the bytes to the output stream
			tempFileos.write(byteArray, 0, i);
		}
		// Close streams to prevent errors
		input.close();
		tempFileos.close();
		return f;
	}
}
