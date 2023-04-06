package org.pointerless.vdmj.remote;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

@Slf4j
public final class GlobalProperties {

	private static final String propertiesFilename = "vdmj-remote.properties";

	/**
	 * Hostname to bind Spark instances to
	 * <p>
	 * Default is 127.0.0.1, 0.0.0.0 opens to non-local requests
 	 */
	public static String hostname = "127.0.0.1";

	public static String mainHostResourceFolder = "cli";

	public static String hostErrorResourceFolder = "no-folder-html";

	public static void loadPropertiesFromFile(String filename){
		Properties defaultProps = new Properties();
		URL propFileURL = GlobalProperties.class.getResource(filename);
		if(propFileURL == null){
			log.info("Couldn't find '"+propertiesFilename+"', leaving default properties");
		}else {
			try (FileInputStream propFile = new FileInputStream(propFileURL.getFile())){
				defaultProps.load(propFile);
				log.info("Loaded properties '"+propFileURL.getFile()+"'");
				hostname = defaultProps.getProperty("hostname", hostname);
				mainHostResourceFolder = defaultProps.getProperty("mainHostResourceFolder", mainHostResourceFolder);
				hostErrorResourceFolder = defaultProps.getProperty("hostErrorResourceFolder", hostErrorResourceFolder);
			} catch (IOException e) {
				log.info("Couldn't load '" + propFileURL.getFile() + "', leaving default properties");
			}
		}
	}

	static {
		loadPropertiesFromFile(propertiesFilename);
	}

}
