package ConfigReader;


import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import helper.utils.UtilityHelper;




public class PropertiesReader {

	private static String LOCAL_ROOT_PATH = UtilityHelper.getRootDir();
	public static String LOCAL_RESOURCE_PATH = LOCAL_ROOT_PATH + "resources" + File.separator;
	private static String PROPERTIES_TYPE_PROPERTIES = ".property";
	private static String PROPERTIES_TYPE_CONF = ".conf";

   /**
    * @param path path to properties file
    * @return properties list of properties from properties file
    * @throws Exception exception from getting properties file
    */
	public static List<Properties> Property(String path) throws Exception {

		List<Properties> properties = new ArrayList<Properties>();
		
		if(new File(path).isFile()) {
			properties.addAll(getPropertiesByFileType(path, StringUtils.EMPTY));
		}else {	
			properties.addAll(getPropertiesByFileType(path, PROPERTIES_TYPE_PROPERTIES));
			properties.addAll(getPropertiesByFileType(path, PROPERTIES_TYPE_CONF));
			
			if(getFileList(path).isEmpty()) {
				System.out.println("path: '" + path + "' does not have any property files, please verify resources/properties.property for correct path");
				System.exit(0);
			}
		}
		
		return properties;
	}

	/**
	 * gets all properties file by file type in a directory
	 * 
	 * @param path:
	 *            directory path
	 * @param fileType:
	 *            eg. ".conf"
	 * @return list of all properties
	 * @throws Exception exception from getting properties file
	 */
	public static List<Properties> getPropertiesByFileType(String path, String fileType) throws Exception {
		List<Properties> properties = new ArrayList<Properties>();
		
		List<File> files = new ArrayList<File>();
		
		if(fileType.isEmpty()) {
			File file = getFile(path);
			files.add(file);
		}else
			files = getFileListByType(path, fileType);


		for (File file : files) {
			// get property files
			FileInputStream fileInput = new FileInputStream(file);
			Properties prop = new Properties();
			prop.load(fileInput);

			// add to propery list
			properties.add(prop);
		}
		return properties;
	}

	/**
	 * @return path to the project root directory
	 */
	public static String getLocalRootPath() {
			return LOCAL_ROOT_PATH;
	}

	/**
	 * @return root path
	 */
	public static String getLocalResourcePath() {
			return LOCAL_RESOURCE_PATH;
	}

	/**
	 * gets the value of the properties file based on key value, And sets default
	 * value if value is missing
	 * @param key key in properties file
	 * @param Property target properties from property file
	 * @return string value of the property
	 */
	public static String getStringProperty(String key, Properties Property) {
		try {
			return Property.getProperty(key, "").replace("\"", "").trim();
		} catch (Exception e) {
			e.getMessage();
		}
		return "";
	}

	public static ArrayList<String> getAllFiles(File curDir) {
		ArrayList<String> array = new ArrayList<String>();

		array = getFileList(curDir, array);
		return array;
	}
	/**
	 * gets all files in a directory to get all files: File curDir = new File(".");
	 * getAllFiles(curDir);
	 * 
	 * @param curDir target directory
	 * @return the list of all files in given directory
	 */
	public static ArrayList<String> getFileList(File curDir, ArrayList<String> array) {
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isDirectory())
				getFileList(f, array);
			if (f.isFile()) {
				//TestLog.ConsoleLog("All files: " + f.getPath() + " : " + f.getName());
				array.add(f.getPath());
			}
		}
		return array;
	}
	
	/**
	 * get file by name
	 * @param path
	 * @param filename
	 * @return
	 */
	public static File getFileByName(String path, String filename) {
		List<File> files = getFileList(path);
		for(File file : files) {
			String simplename = file.getName().split("\\.")[0];
			if(simplename.equals(filename))
				return file;
		}
		System.out.println("file: <" + filename + "> not found at path: " + path);
		return null;
	}
	
	/**
	 * gets the list of files tye: file type. eg. ".csv"
	 * 
	 * @return
	 */
	protected static ArrayList<File> getFileListByType(String directoryPath, String type) {
		ArrayList<File> testFiles = getFileList(directoryPath);
		ArrayList<File> filteredFiles = new ArrayList<File>();
		
		// filter files by suffix And add to testFiles list
		for (int i = 0; i < testFiles.size(); i++) {
			if (testFiles.get(i).isFile() && testFiles.get(i).getName().endsWith(type)) {
				filteredFiles.add(testFiles.get(i));
				// System.out.println("File " + listOfFiles[i].getName());
			}
		}
		return filteredFiles;
	}
	
	/**
	 * get file from file path
	 * @param directoryPath
	 * @return
	 */
	protected static File getFile(String directoryPath) {
		File file = new File(directoryPath);
		if(!file.exists())
			System.out.println("test files not found at path: " + directoryPath);
		return file;
	}
	
	/**
	 * returns the list of files in directory
	 * @param directoryPath
	 * @return
	 */
	protected static ArrayList<File> getFileList(String directoryPath) {
		File folder = new File(directoryPath);
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> testFiles = new ArrayList<File>();

		// fail test if no csv files found
		if (listOfFiles == null) {
			 try {
				throw new Exception("test files not found at path: " + directoryPath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//	TestLog.logDirectConsoleMessage(Priority.WARN, "test files not found at path: " + directoryPath );
		//	return testFiles;
		}
		testFiles = new ArrayList<>(Arrays.asList(listOfFiles));
		return testFiles;
	}
}