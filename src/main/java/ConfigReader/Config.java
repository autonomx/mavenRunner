package ConfigReader;




import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import ConfigReader.MavenReader;
import ConfigReader.PropertiesReader;
import helper.utils.UtilityHelper;

public class Config {

	private static final String CONFIG_GROUP_PREFIX = "config.group.";
	private static final String CONFIG_PROFILE_PREFIX = "config.profile.";

	private static final String CONFIG_PREFIX = "config.";
	private static final String PROFILE_PREFIX = "profile.";
	private static final String GROUP_PREFIX = "profile.group.";
	
	public static String RESOURCE_PATH = PropertiesReader.getLocalResourcePath();
	public static Map<String, Object> CONFIG_READER = new ConcurrentHashMap<String, Object>();
	
	public static final String ENVIRONMENT_CONFIG = "environment";

	/**
	 * gets property value based on key from maven or properties file order: maven
	 * Then properties
	 * @param key key in properties file
	 * @param property
	 * @return string value of property file
	 */
	private static String getStringProperty(String key, Properties property) {
		if (!MavenReader.getStringProperty(key).isEmpty()) {
			return MavenReader.getStringProperty(key);
		}
		if (!PropertiesReader.getStringProperty(key, property).isEmpty()) {
			return PropertiesReader.getStringProperty(key, property);
		}

		return "";
	}

	/**
	 * git all files in given directory
	 * @param curDir target directory
	 */
	public static void getAllFiles(File curDir) {

		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isDirectory())
				getAllFiles(f);
			if (f.isFile()) {
				System.out.println("All files: " + f.getPath() + " : " + f.getName());
			}
		}
	}
	

	/**
	 * get all key values from property files in directory at path
	 * Fails if duplicate key exists. All keys need to be unique
	 * @param path path to proeprties file
	 * @return map of all key and values in all property files in given path
	 */
	public static Map<String, String> getAllKeys(String path) {
		Map<String, String> config = new ConcurrentHashMap<String, String>();

		try {
			List<Properties> properties = PropertiesReader.Property(path);

			for (Properties property : properties) {

				for (String key : property.stringPropertyNames()) {
					String value = getStringProperty(key, property);
					config.put(key, value);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return config;
	}

	/**
	 * loads config And properties files to TestObject config map
	 * @param testId id of the test
	 */
	public static void loadConfig() {

		Map<String, Object> config = loadConfigProperties();
		CONFIG_READER.putAll(config);	
	}
	
	/**
	 * loads config And properties files to TestObject config map
	 * @param testId id of the test
	 * @return 
	 */
	public static Map<String, Object> loadConfigProperties() {

		Map<String, Object> config = new ConcurrentHashMap<String, Object>();
		
		// get all keys from resource path
		Map<String, String> propertiesMap = getAllKeys(RESOURCE_PATH);

		File environmentConfig  = UtilityHelper.getFileByName(PropertiesReader.LOCAL_RESOURCE_PATH, ENVIRONMENT_CONFIG, true);
		propertiesMap = getAllKeys(environmentConfig.getAbsolutePath());
		config.putAll(propertiesMap);
		
		return config;
	}

	/**
	 * get a list of config path from properties.property file prefix: "config.",
	 * not including profiles: config.profile key
	 * 
	 * @param propertiesMap
	 * @return
	 */
	public static List<String> getConfigs(Map<String, String> propertiesMap) {

		List<String> configPath = new ArrayList<String>();

		// get list of profiles from key: config.profile
		for (Entry<String, String> entry : propertiesMap.entrySet()) {
			String key = entry.getKey().toString();
			boolean isConfig = key.startsWith(CONFIG_PREFIX) 
					&& !key.startsWith(CONFIG_PROFILE_PREFIX) 
					&& !key.startsWith(CONFIG_GROUP_PREFIX);
			if (isConfig) {
				configPath.add(entry.getValue());
			}
		}
		return configPath;
	}

	/**
	 * get the list of profile path specified by profile. in
	 * properties.property file multiple profiles can be separated by ","
	 * 
	 * @param propertiesMap
	 * @return
	 */
	public static List<String> getConfigProfiles(Map<String, String> propertiesMap) {
		List<String> profiles = new ArrayList<String>();
		List<String> profilePath = new ArrayList<String>();

		// get list of profiles from key: profile.
		for (Entry<String, String> entry : propertiesMap.entrySet()) {
			boolean isProfile = entry.getKey().toString().startsWith(PROFILE_PREFIX);
			boolean isCorrectLength =  entry.getKey().toString().split("\\.").length == 2;
			if (isProfile && isCorrectLength) {
				String profile = entry.getKey().split("\\.")[1];
				// add profile name to value. eg. environment.dev 
				List<String> values = new ArrayList<String>(Arrays.asList(entry.getValue().split(",")));
				profiles.addAll(values.stream().map(c -> profile + "." + c).collect(Collectors.toList()));
			}
		}
		// property value: profile.environment = dev
		// add profile path to list. eg. 'environment.dev'. profile is environment, 
		// dev is the property file name: dev.property
		for (String profile : profiles) {
			String profileValue = profile.split("\\.")[0];
			String propertyFile = profile.split("\\.")[1];
			
			// continue to next profile if value set to none
			if(propertyFile.equals("none"))
				continue;
						
			if (propertiesMap.get(CONFIG_PROFILE_PREFIX + profileValue) == null)
				System.out.println("profile not found: " + profile
						+ ". Please add profile to properties.property file as profile." + profile);
			String path =  propertiesMap.get(CONFIG_PROFILE_PREFIX + profileValue);
			File file = PropertiesReader.getFileByName(PropertiesReader.getLocalRootPath() + path, propertyFile);
			profilePath.add(path + file.getName());
		}

		return profilePath;
	}
	
	/**
	 * get the list of group path specified by profile.group.groupName. in
	 * properties.property file multiple profiles can be separated by ","
	 * 
	 * @param propertiesMap
	 * @return
	 */
	public static List<String> getConfigGroup(Map<String, String> propertiesMap) {
		List<String> profiles = new ArrayList<String>();
		List<String> groupPath = new ArrayList<String>();

		// get list of groups from key: profile.
		for (Entry<String, String> entry : propertiesMap.entrySet()) {
			boolean isProfile = entry.getKey().toString().startsWith(GROUP_PREFIX);
			boolean isCorrectLength =  entry.getKey().toString().split("\\.").length == 3;
			if (isProfile && isCorrectLength) {
				String group = entry.getKey().split("\\.")[2];
				
				// add group name to value. eg. repot.value
				List<String> values = new ArrayList<String>(Arrays.asList(entry.getValue().split(",")));
				profiles.addAll(values.stream().map(c -> group + "." + c).collect(Collectors.toList()));
			}
		}

		// add group path to list
		for (String profile : profiles) {
			String value = profile.split("\\.")[1];
			
			// continue to next profile if value set to none
			if(value.equals("none"))
				continue;
			
			if (propertiesMap.get(CONFIG_GROUP_PREFIX + profile) == null)
				System.out.println("profile not found: " + profile
						+ ". Please add groups to properties.property file as " + CONFIG_GROUP_PREFIX + profile);
			String path = propertiesMap.get(CONFIG_GROUP_PREFIX + profile);
			groupPath.add(path);
		}

		return groupPath;
	}

	
	/**
	 * returns config value
	 * 
	 * @param key get string value of key from properties
	 * @return string value of key
	 */
	public static String getValue(String key) {
		return getValue(key, false);
	}

	/**
	 * returns config value
	 * 
	 * @param key get string value of key from properties
	 * @return string value of key
	 */
	public static String getValue(String key, boolean isFailable) {

		if(CONFIG_READER.get(key) == null)
			return StringUtils.EMPTY;
		
		String value = CONFIG_READER.get(key).toString();
		if (value == null) {
		   	System.out.println("value not found, default empty: " + key);
			value = "";
		}
		List<String> items = Arrays.asList(value.split("\\s*,\\s*"));
		return items.get(0);
	}
	
	/**
	 * gets boolean value from properties key
	 * 
	 * @param key target key from properties file
	 * @return the boolean value of key from properties
	 */
	public static Boolean getBooleanValue(String key) {
		return getBooleanValue(key, false);
	}

	/**
	 * gets boolean value from properties key
	 * 
	 * @param key target key from properties file
	 * @return the boolean value of key from properties
	 */
	public static Boolean getBooleanValue(String key, boolean isFailable) {
		String value = getValue(key,isFailable);
		if (value.isEmpty()) {
			 System.out.println("value not found, default false: " + key);
			return false;
		}
		return Boolean.parseBoolean(value);
	}
	
	/**
	 * gets the object value from property key
	 * @param key key in properties file
	 * @return returns the object value of key from properties
	 */
	public static Object getObjectValue(String key) {
		Object value = CONFIG_READER.get(key);
		return value;
	}
	
	/**
	 * gets int value from properties key
	 * 
	 * @param key key in properties file
	 * @return returns the integer value of key from properties
	 */
	public static int getIntValue(String key) {
		return getIntValue(key, false);
	}

	/**
	 * gets int value from properties key
	 * 
	 * @param key key in properties file
	 * @return returns the integer value of key from properties
	 */
	public static int getIntValue(String key, boolean isFailable) {
		String value = getValue(key, isFailable);
		if (value.isEmpty()) {
			 System.out.println("value not found, default -1: " + key);
			return -1;
		}
		return Integer.valueOf(value);
	}
	
	/**
	 * gets double value from properties key
	 * 
	 * @param key key in properties file
	 * @return the double value of key from properties
	 */
	public static double getDoubleValue(String key) {
		return getDoubleValue(key, false);
	}
	
	/**
	 * gets double value from properties key
	 * 
	 * @param key key in properties file
	 * @return the double value of key from properties
	 */
	public static double getDoubleValue(String key, boolean isFailable) {
		String value = getValue(key, isFailable);
		if (value.isEmpty()) {
			 System.out.println("value not found, default -1: " + key);
			return -1;
		}
		return Double.valueOf(value);
	}
	
	/**
	 * returns a list from config value values separated by ","
	 * 
	 * @param key key in properties file
	 * @return the list of values from key separated by ","
	 */
	public static List<String> getValueList(String key) {
		return getValueList(key, true);
	}

	/**
	 * returns a list from config value values separated by ","
	 * 
	 * @param key key in properties file
	 * @return the list of values from key separated by ","
	 */
	public static List<String> getValueList(String key, boolean isFailable) {
		String value = (String) CONFIG_READER.get(key);
		List<String> items = new ArrayList<String>();
		if (value == null) {
			System.out.println("value not found in config files: " + key);
		}
		if(!value.isEmpty()) 
			items = Arrays.asList(value.split("\\s*,\\s*"));
		return items;
	}

	/**
	 * puts key value pair in config
	 * 
	 * @param key key in properties file
	 * @param value value associated with key
	 */
	public static void putValue(String key, Object value) {
		System.out.println("storing in key: " + key + " value: " + value);
		CONFIG_READER.put(key, value);
	}
	
	public static void putValue(String key, Object value, String info) {
		System.out.println("storing in key: " + key + " value: " + info);
		CONFIG_READER.put(key, value);
	}
}