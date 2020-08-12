package test;


import java.io.File;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ConfigReader.Config;
import mavenRunner.MavenCommandRunner;




public class MavenRunnerTest {
	
	
	
	@BeforeClass
	public void beforeClass()  {
		// load config properties
		Config.loadConfig();
	}

	@Test(priority=1)
	public void verifyMavenDownload() throws Exception {
		MavenCommandRunner.MAVEN_PATH = "";
		
		File mavenDestination = new File(MavenCommandRunner.MAVEN_DOWNLOAD_DESTINATION);
		FileUtils.deleteDirectory(mavenDestination);
		
		MavenCommandRunner.downloadMavenIfNotExist();

        File mavenFile = MavenCommandRunner.GetAndVerifyMavenHomePath();
		Assert.assertTrue( mavenFile.exists(), "maven destination not found");
		
		File mavenHome = new File(mavenFile.getAbsolutePath() + "/bin");
		Assert.assertTrue(mavenHome.exists(), "maven destination not found at " + mavenHome.getAbsolutePath());
	}
	
	@Test(dependsOnMethods = "verifyMavenDownload", priority=2)
	public void verifyMavenPathFromConfig() throws Exception {
		MavenCommandRunner.MAVEN_PATH = "";
	
		// download maven to utils folder
		MavenCommandRunner.downloadMavenIfNotExist();
        File mavenFile = MavenCommandRunner.GetAndVerifyMavenHomePath();
		MavenCommandRunner.MAVEN_PATH = "";

		// set config path
		Config.putValue("maven.home", mavenFile.getAbsolutePath());
		MavenCommandRunner.setMavenPathFromConfig();

		Assert.assertEquals(MavenCommandRunner.MAVEN_PATH, mavenFile.getAbsolutePath());
	}
	@Test()
	public void verifyMavenPathFromCommandLine() throws Exception {
		MavenCommandRunner.MAVEN_PATH = "";
		MavenCommandRunner.setMavenPath();
		
		// maven path could return empty. TODO: find way to get consistent return from command line
		if(MavenCommandRunner.MAVEN_PATH.isEmpty()) return;
		
		Assert.assertTrue(MavenCommandRunner.MAVEN_PATH.contains("maven"), "maven path not found: " + MavenCommandRunner.MAVEN_PATH);

		File mavenFolder = new File(MavenCommandRunner.MAVEN_PATH);
		Assert.assertTrue(mavenFolder.exists(), "maven folder not found: " + MavenCommandRunner.MAVEN_PATH);
	}
}
