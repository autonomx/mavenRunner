package mavenRunner;




import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import ConfigReader.Config;
import ConfigReader.ProxyDetector;
import helper.utils.UtilityHelper;
import net.lingala.zip4j.ZipFile;

public class MavenCommandRunner {

	public static String MAVEN_PATH = StringUtils.EMPTY;
	public static String MAVEN_URL = "https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip";
	public static String MAVEN_DOWNLOAD_DESTINATION = UtilityHelper.getRootDir()+ ".maven" + File.separator;
    private static final int DOWNLOAD_BUFFER = 16 * 1024;

	final static String MAVEN_PROPERTY = "maven.home";
	final static String MAVEN_URL_PROPERTY = "maven.url";
	
	public static boolean MAVEN_AUTO_PROXY_SET = false;
	public static boolean IS_PROXY_ENABLED = false;
	
	public static String MAVEN_DIR = "apache-maven";



	/**
	 * process of setting maven: 1. set maven path from config, if exists 2. use mvn
	 * -version shell command to get maven path 3. if not available, download maven
	 * into runner/utils/maven folder 4. run maven through maven invoker 5. if maven
	 * invoker failed, run using shell command "mvn command"
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Root Path: " + UtilityHelper.getRootDir());

		
		// load config properties
		Config.loadConfig();

		// set maven path from config value: maven.home
		setMavenPathFromConfig();

		// set maven path using mvn -version command
		setMavenPath();
		
		// determine if proxy should be enabled
		setAutoProxy();

		// if no maven path found, download in utils folder
		downloadMavenIfNotExist();
		
		String[] command = setMavenCommandProxy(args);

		// run maven invoker. user maven home path
		boolean isSuccess = runMavenInvoker(command);

		// if not successful, run mvn command from shell
		if (!isSuccess)
			excuteCommand("mvn " + command);
	}
	
	/**
	 * set if proxy should be enabled
	 * once per suite
	 * @throws MalformedURLException
	 */
	private static void setAutoProxy() throws MalformedURLException {
		
		boolean isProxyAutoDetect = Config.getBooleanValue(ProxyDetector.PROXY_AUTO_DETECT);
		
		// use url from maven property if not set
		String urlProperty = Config.getValue(MAVEN_URL_PROPERTY);
		if (!urlProperty.isEmpty())
			MAVEN_URL = urlProperty;
		
		// set proxy enabled value based on proxy auto detection. if auto detect enabled,
		// attempt to connect to url with proxy info. if able to connect, enable proxy
		if(isProxyAutoDetect && !MAVEN_AUTO_PROXY_SET) {
			IS_PROXY_ENABLED = ProxyDetector.setProxyAutoDetection(new URL(MAVEN_URL));
			MAVEN_AUTO_PROXY_SET = true;
		}else if (!isProxyAutoDetect)
			IS_PROXY_ENABLED = Config.getBooleanValue(ProxyDetector.PROXY_ENABLED);
	}

	/**
	 * set maven path if set from config file
	 */
	public static void setMavenPathFromConfig() {
		String path = Config.getValue(MAVEN_PROPERTY);
		System.out.println("Maven config path: " + path);
		if (path.isEmpty())
			return;

		File mavenFolderPath = new File(path);
		if (UtilityHelper.isFileInFolderPath(mavenFolderPath, "bin")) {
			MAVEN_PATH = path;
		}
	}

	/**
	 * download maven if path is not found
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public static void downloadMavenIfNotExist() throws Exception {
		if (!MAVEN_PATH.isEmpty())
			return;

		File mavenRootDestinationPath = new File(MAVEN_DOWNLOAD_DESTINATION);
		File mavenDirDestinationPath = new File(MAVEN_DOWNLOAD_DESTINATION + File.separator + MAVEN_DIR );

		if (!isMavenDownloaded(mavenRootDestinationPath)) {

			// use url from maven property if not set
			String urlProperty = Config.getValue(MAVEN_URL_PROPERTY);
			if (!urlProperty.isEmpty())
				MAVEN_URL = urlProperty;

			System.out.println("<<Downloading maven... " + MAVEN_URL + ">>");
			// delete folder first
			FileUtils.deleteDirectory(mavenDirDestinationPath);

			// create directory
			mavenRootDestinationPath.mkdir();
			
			// download. if proxy enabled, download through proxy
			String zipPath = mavenRootDestinationPath.getAbsolutePath() + File.separator + "download.zip";
			downloadFromURL(new URL(MAVEN_URL), new File(zipPath));
			
			// unzip
			new ZipFile(zipPath).extractAll(MAVEN_DOWNLOAD_DESTINATION);
			FileUtils.forceDelete(new File(zipPath));
		}


		// rename file
		File updateMaven = renameDir(mavenRootDestinationPath);
		
		System.out.println("Setting maven path to: " + updateMaven.getAbsolutePath());
		MAVEN_PATH = updateMaven.getAbsolutePath();
	}
	
	private static File renameDir(File mavenDestinationPath) {
		String mavenPath = MAVEN_DOWNLOAD_DESTINATION + getMavenDownloadHome(mavenDestinationPath);
		String mavenPathUpdated = MAVEN_DOWNLOAD_DESTINATION + MAVEN_DIR;

		File maven = new File(mavenPath);
		File updateMaven = new File(mavenPathUpdated);
		maven.renameTo(updateMaven);
		return updateMaven;
	}
	
	/**
	 * copy to url
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void downloadFromURL(URL source, File destination) throws IOException {
		Proxy proxy = null;

		String host = Config.getValue(ProxyDetector.PROXY_HOST);
		int port = Config.getIntValue(ProxyDetector.PROXY_PORT);
		String username = Config.getValue("proxy.username");
		String password = Config.getValue("proxy.password");
		
		// set username/password for proxy authenticator
		if (!username.isEmpty() && !password.isEmpty()) {
			Authenticator.setDefault(new Authenticator() {
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password.toCharArray());
				}
			});
		}

		// set and download through proxy if enabled
		if (!host.isEmpty() && port != -1)
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

		System.out.println("downloading maven from: " + source );

		if(IS_PROXY_ENABLED && proxy !=null) {
			System.out.println("downloading maven through proxy: host: " + host + " port: " + port );
			downloadUsingProxy(source, destination, proxy);
		}
		else
			FileUtils.copyURLToFile(source, destination);
	}
	

    /**
     */
    private static void downloadUsingProxy(URL source, File destination, Proxy proxy) throws IOException {
    	
    	try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destination));
            InputStream inputStream = source.openConnection(proxy).getInputStream()) {

            byte[] buffer = new byte[DOWNLOAD_BUFFER];
            int len;
            while ((len = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, len);
            }
        }
    }

	/**
	 * gets maven downloaded folder name eg. apache-maven-3.6.2
	 * 
	 * @param mavenDestinationPath
	 * @return
	 */
	private static String getMavenDownloadHome(File mavenDestinationPath) {
		String mavenHomePath = StringUtils.EMPTY;
		File[] fileList = mavenDestinationPath.listFiles();
		if (fileList.length == 0)
			return mavenHomePath;

		for (File file : fileList) {
			if (file.getName().toLowerCase().contains("maven"))
				return file.getName();
		}
		return mavenHomePath;
	}

	/**
	 * returns if maven has downloaded properly
	 * 
	 * @param mavenDestinationPath
	 * @return
	 */
	private static boolean isMavenDownloaded(File mavenDestinationPath) {
		File[] fileList = mavenDestinationPath.listFiles();
		if (fileList == null || fileList.length == 0)
			return false;

		File mavenPath = new File(mavenDestinationPath.getAbsolutePath() + File.separator + MAVEN_DIR + File.separator
				+ "bin" + File.separator + "mvn");
		if (mavenPath.exists())
			return true;
		return false;
	}

	/**
	 * Maven home: /usr/local/Cellar/maven/3.6.2/libexec get path of maven from "mvn
	 * -version"
	 * 
	 * @param results
	 * @return
	 */
	public static void setMavenPath() {

		// if maven path is set using config, skip
		if (!MAVEN_PATH.isEmpty())
			return;
		
		// if maven is downloaded in utility, return
		File mavenDestinationPath = new File(MAVEN_DOWNLOAD_DESTINATION);
		if (isMavenDownloaded(mavenDestinationPath)) return;

		ArrayList<String> results = excuteCommand("mvn -version");
		System.out.println("maven -version results: " + results);

		String resultsString = Arrays.toString(results.toArray());

		if (results.isEmpty())
			return;

		String[] resultArray = resultsString.split(",");
		for (String result : resultArray) {
			if (result.contains("Maven home:")) {
				MAVEN_PATH = result.split(":")[1].trim();
			}
		}
		System.out.println("maven path: " + MAVEN_PATH);
	}

	/**
	 * run command based on windows or mac/linux environment
	 * 
	 * @param command
	 * @return
	 */
	protected static ArrayList<String> excuteCommand(String... command) {
		System.out.println("<<executing maven command through command line>>");
		
		String commandString = getString(command);


		ArrayList<String> results = new ArrayList<String>();

		if (isMac() || isUnix()) {
			results = runCommand(new String[] { "/bin/sh", "-c", commandString });
		} else if (isWindows()) {
			results = runCommand("cmd /c start " + commandString);
		}

		return results;
	}
	
	/**
	 * append proxy to maven command if enabled
	 * @param command
	 * @return
	 */
	public static String[] setMavenCommandProxy(String[] args) {
		
		// convert args to list
		ArrayList<String> commands = new ArrayList<>(Arrays.asList(args));
		
		String host = Config.getValue(ProxyDetector.PROXY_HOST);
		int port = Config.getIntValue(ProxyDetector.PROXY_PORT);
		String proxyProtocal = Config.getValue(ProxyDetector.PROXY_MAVEN_PROTOCAL);

		// add parallel maven build
		commands.add("-T 1C");
		
		// if proxy is disabled, set proxy protocal to none
		if(!IS_PROXY_ENABLED) {
			proxyProtocal = "none";
		}
		
		switch(proxyProtocal) {
		case "http":
			commands.add("-DproxySet=true");
			commands.add("-Dhttp.proxyHost=" + host);
			commands.add("-Dhttp.proxyPort=" + port);
			break;
		case "https":
			commands.add("-DproxySet=true");
			commands.add("-Dhttps.proxyHost=" + host);
			commands.add("-Dhttps.proxyPort=" + port);	
			break;
		case "default":
			commands.add("-DproxySet=true");
			commands.add("-DproxyHost=" + host);
			commands.add("-DproxyPort=" + port);	
			break;
		case "none":
			break;
		default:
			break;
		}
		
		String[] commandArr = commands.toArray(new String[commands.size()]);
		String commandString = getString(commandArr);
		System.out.println("maven command: " + commandString);
		return commandArr;
	}
	
	/**
	 * get string from array separted by space
	 * @param array
	 * @return
	 */
	public static String getString(String[] array) {
		String value = Arrays.toString(array).replaceAll("^.|.$", "").replaceAll(","," ");
		return value;
	}

	/**
	 * run command using command line
	 * 
	 * @param cmd
	 * @return
	 */	
	public static ArrayList<String> runCommand(String... command) {
		ArrayList<String> results = new ArrayList<String>();

	    try {
	        ProcessBuilder builder = new ProcessBuilder(command);
	        // Share standard input/output/error descriptors with Java process...
	        builder.inheritIO();
	        // ... except standard output, so we can read it with getInputStream().
	        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);

	        Process p = builder.start();

	        try (BufferedReader reader =
	            new BufferedReader(new InputStreamReader(p.getInputStream()))) {

	            String line = "";
	            while ((line = reader.readLine()) != null) {
	            	results.add(line);
	            }
	        }

	        p.waitFor();

	    } catch (IOException | InterruptedException e) {
			System.out.println("command:  '" + command + "' output: " + e.getMessage());
	    }
	    
	    if (results.isEmpty())
			System.out.println(
					"command:  '" + Arrays.toString(command) + "' did not return results. please check your path at resourced -> properties -> environment.property");

		return results;
	}

	/**
	 * returns true if OS is mac
	 * 
	 * @return
	 */
	protected static boolean isMac() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("mac");
	}

	/**
	 * returns true if OS is windows
	 * 
	 * @return
	 */
	protected static boolean isWindows() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("win");
	}

	/**
	 * returns true if OS is unix or linux
	 * 
	 * @return
	 */
	protected static boolean isUnix() {
		String osName = System.getProperty("os.name");
		return (osName.indexOf("nix") >= 0 || osName.indexOf("linux") >= 0 || osName.indexOf("nux") >= 0
				|| osName.indexOf("aix") > 0);
	}

	/**
	 * run maven command through maven invoker requires maven home path
	 * 
	 * @param args
	 * @return
	 */
	private static boolean runMavenInvoker(String[] args) {

		ArrayList<String> goals = new ArrayList<String>();
		
		for (int i = 0; i < args.length; i++) {
			goals.add(args[i]);
		}
		if (goals.isEmpty())
			goals.add("compile");

		InvocationRequest request = new DefaultInvocationRequest();
		String pomLocation = UtilityHelper.getRootDir() + "pom.xml";
		request.setPomFile(new File(pomLocation));
		request.setGoals(goals);

		Invoker invoker = new DefaultInvoker();

		// get maven home path (root path of maven)
		File mavenFile = GetAndVerifyMavenHomePath();

		System.out.println("executing maven command using maven Invoker");
		System.out.println("runMavenInvoker: " + MAVEN_PATH);
		System.out.println("maven command for maven invoker: " + Arrays.toString(args));
		
		invoker.setMavenHome(mavenFile);

		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			System.out.println("<<maven invoker has failed>>");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * verify maven bin path exists in maven path or parent folder set maven path to
	 * the correct value
	 * 
	 * @return
	 */
	public static File GetAndVerifyMavenHomePath() {

		File mavenFolderPath = new File(MAVEN_PATH.trim());
		if (UtilityHelper.isFileInFolderPath(mavenFolderPath, "bin")) {
			return mavenFolderPath;
		}

		// check parent folder for bin folder. mvn -version returns maven path with
		// inner folder
		mavenFolderPath = mavenFolderPath.getParentFile();
		if (UtilityHelper.isFileInFolderPath(mavenFolderPath, "bin")) {
			return mavenFolderPath;
		}

		MAVEN_PATH = mavenFolderPath.getAbsolutePath();
		return mavenFolderPath;
	}
	

	public static void executeMavenCommandEmbedded() {
//		ArrayList<String> goals = new ArrayList<String>();
//
//		String root = new File(".").getAbsolutePath();
//		MavenCli cli = new MavenCli(new ClassWorld("maven",Thread.currentThread().getContextClassLoader()));
//		System.setProperty("maven.multiModuleProjectDirectory", root);
//
//	
//		if(goals.isEmpty()) goals.add("compile");
//
//		String[] goalsArays = goals.toArray(new String[goals.size()]);
//		cli.doMain(goalsArays, ".", System.out, System.err);
	}

}
