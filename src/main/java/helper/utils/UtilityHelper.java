package helper.utils;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

public class UtilityHelper {
	

	/**
	 * gets list of files including from sub folder based on type. eg. ".csv"
	 * 
	 * @return
	 */
	public static List<File> getFileListWithSubfolders(String directoryName, String type, List<File> files) {
		File directory = new File(directoryName);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isFile() && file.getName().endsWith(type)) {
					files.add(file);
				} else if (file.isDirectory()) {
					getFileListWithSubfolders(file.getAbsolutePath(), type, files);
				}
			}
		return files;
	}
	
	/**
	 * get current project root directory, where pom.xml is
	 * 
	 * @return
	 */
	public static String getRootDir() {
		File currentWorkingDir = new File(".");
		File root = null;

		if (isFileInFolderPath(currentWorkingDir, "pom.xml"))
			root = currentWorkingDir;
		else if (isFileInFolderPath(new File(".."), "pom.xml")) {
			root = new File("..");
		}
		return root.getAbsolutePath() + File.separator;
	}
	
	/**
	 * checks if maven is installed installed = if maven/bin folder exists
	 * 
	 * @param folderPath
	 * @return
	 */
	public static boolean isFileInFolderPath(File folderPath, String exepctedFile) {

		File[] fileList = folderPath.listFiles();
		if (fileList == null)
			return false;

		for (File file : fileList) {
			if (file.getName().toLowerCase().contains(exepctedFile))
				return true;
		}
		return false;
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
				// TestLog.ConsoleLog("All files: " + f.getPath() + " : " + f.getName());
				array.add(f.getPath());
			}
		}
		return array;
	}
	

	
	public static ArrayList<File> getFileListByType(String directoryPath, String type) {
		return getFileListByType(directoryPath, type, false);
	}

	/**
	 * gets the list of files tye: file type. eg. ".csv"
	 * 
	 * @return
	 */
	public static ArrayList<File> getFileListByType(String directoryPath, String type, boolean includeSubtype) {
		ArrayList<File> filteredFiles = new ArrayList<File>();
		ArrayList<File> testFiles = new ArrayList<File>();
		if (includeSubtype)
			testFiles = getFileList(directoryPath, testFiles);
		else
			testFiles = getFileList(directoryPath);

		for (File file : testFiles) {
			if (file.isFile() && file.getName().endsWith(type)) {
				filteredFiles.add(file);
			}
		}
		return filteredFiles;
	}
	
	/**
	 * get file by name
	 * 
	 * @param path
	 * @param filename
	 * @return
	 */
	public static File getFileByName(String path, String filename, boolean includeSubDir) {
		path = getFullPath(path);
		
		List<File> files = getFileList(path, includeSubDir);
		for (File file : files) {
			String simplename = file.getName().split("\\.")[0];
			if (simplename.equals(filename))
				return file;
		}
		return null;
	}
	
	
	/**
	 * gets full path from relative path
	 * relative path is from root directory ( where pom.xml file is located )
	 * @param path
	 * @return
	 */
	protected static String getFullPath(String path) {
		path = path.replace("\\", File.separator).replace("//", File.separator);
		
		if(!path.contains(getRootDir()))
			path = getRootDir() + path;
		
		return path;	
	}
	
	/**
	 * returns the list of files in directory
	 * 
	 * @param directoryPath
	 * @return
	 */
	protected static ArrayList<File> getFileList(String directoryPath, boolean includeSubDir) {
		ArrayList<File> testFiles = new ArrayList<File>();
		if (includeSubDir)
			testFiles = getFileList(directoryPath, testFiles);
		else
			testFiles = getFileList(directoryPath);
		
		return testFiles;
	}
	
	/**
	 * returns the list of files in directory
	 * 
	 * @param directoryPath
	 * @return
	 */
	protected static ArrayList<File> getFileList(String directoryPath) {
		File folder = new File(directoryPath);
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> testFiles = new ArrayList<File>();

		// fail test if no csv files found
		if (listOfFiles == null) {
			System.out.println("test files not found at path: " + directoryPath);
		}
		testFiles = new ArrayList<>(Arrays.asList(listOfFiles));
		return testFiles;
	}
	
	/**
	 * returns the list of files in directory
	 * 
	 * @param directoryPath
	 * @return
	 */
	protected static ArrayList<File> getFileList(String directoryPath, ArrayList<File> files) {
		File directory = new File(directoryPath);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isFile()) {
					files.add(file);
				} else if (file.isDirectory()) {
					getFileList(file.getAbsolutePath(), files);
				}
			}
		return files;
	}
	
	

	/**
	 * normalizes string removes space, new line chars
	 * 
	 * @param value
	 * @return
	 */
	public static String stringRemoveLines(String value) {
		value = value.trim().replace("\n", "").replace("\r", "");
		value = value.replaceAll("\\r|\\n", "");
		return value;
	}
	
	/**
	 * get numeric value from string
	 * 
	 * @param value
	 * @param isFailOnNoInt
	 * @return
	 */
	protected static int getIntFromString(String value) {
		double doubleVal = getDoubleFromString(value, false);
		return (int) doubleVal;
	}
	
	/**
	 * get numeric value from string
	 * 
	 * @param value
	 * @param isFailOnNoInt
	 * @return
	 */
	protected static double getDoubleFromString(String value, boolean isFailOnNoInt) {
		if (!isStringContainNumber(value)) {
			if (isFailOnNoInt)
				System.out.println("numeric value not found from String: " + value);
			else
				return -1;
		}
		// remove all non numeric characters
		value = value.replaceAll("[^\\d.]", "");

		// remove . if value starts with ".". eg. ".1"
		if (value.startsWith("."))
			value = value.substring(1);

		if (value.isEmpty())
			return -1;

		Scanner st = new Scanner(value);
		while (!st.hasNextDouble()) {
			st.next();
		}
		double valueDouble = st.nextDouble();
		st.close();

		return valueDouble;
	}
	
	/**
	 * does string have int value
	 * 
	 * @param value
	 * @return
	 */
	protected static boolean isStringContainNumber(String value) {
		value = value.replaceAll("[^\\d.]", "");
		value = value.replace(".", "");
		if (StringUtils.isBlank(value))
			return false;
		return true;
	}

}
