package org.mule.tools.maven.plugin;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.tools.maven.plugin.Deploy;
import org.mule.tools.mmc.rest.DeploymentState;
import org.mule.tools.mmc.rest.DeploymentStatus;
import org.mule.tools.mmc.rest.MuleRest;

import static org.mockito.Mockito.*;

public class DeployTest {
	private static final String VERSION_ID = "7959";
	private static final String DEPLOYMENT_ID = "1234";
	private static final String USER_NAME = "muleuser1";
	private static final String PASSWORD = "pwd1234";
	private static final String SERVER_GROUP = "Development";
	private static final String REPOSITORY_APP_NAME = "MyMuleApp";
	private static final String DEPLOYED_APP_NAME = "MyDeployedMuleApp";
	private static final String ARTIFACT_APP_NAME = "MyArtifactMuleApp";
	
	private static final String VERSION = "1.0-SNAPSHOT";

	private Deploy deploy;

	private MuleRest mockMuleRest;

	File _artifactPath; 
	
	@Before
	public void setup() throws Exception {
		deploy = spy(new Deploy());
		
		_artifactPath = new File("./mule-app.1.2.6-SNAPSHOT.zip");
		
		setupMocks();
		Log log = new SystemStreamLog();

		deploy.setLog(log);
		deploy.outputDirectory = File.createTempFile("456", null);

		deploy.muleAppFileNameWithoutExt = ARTIFACT_APP_NAME;
		deploy.mmcApiUrl = new URL("http", "localhost", 8080, "");
		deploy.mmcUsername = USER_NAME;
		deploy.mmcPassword = PASSWORD;
		deploy.serverOrGroup = SERVER_GROUP;
		deploy.repositoryAppName = REPOSITORY_APP_NAME;
		deploy.deploymentName = DEPLOYED_APP_NAME;
		deploy.version = VERSION;
		
	}

	private void setupMocks() throws Exception {
		doReturn(_artifactPath).when(deploy).getMuleZipFile(any(File.class), anyString());
		mockMuleRest = mock(MuleRest.class);
		when(deploy.buildMuleRest()).thenReturn(mockMuleRest);
		when(mockMuleRest.restfullyUploadRepository(anyString(), anyString(), any(File.class))).thenReturn(VERSION_ID);
		when(mockMuleRest.restfullyCreateDeployment(anyString(), anyString(), anyString())).thenReturn(DEPLOYMENT_ID);
		
		DeploymentState deploymentState = new DeploymentState();
		
		deploymentState.status = DeploymentStatus.DEPLOYED;
		
		when(mockMuleRest.restfullyGetDeploymentState(anyString())).thenReturn(deploymentState);
	}

	@Test(expected = MojoFailureException.class)
	public void testUsernameNull() throws MojoExecutionException, MojoFailureException {
		deploy.mmcUsername = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test(expected = MojoFailureException.class)
	public void testPasswordNull() throws MojoExecutionException, MojoFailureException {
		deploy.mmcPassword = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test(expected = MojoFailureException.class)
	public void testOutputDirectoryNull() throws MojoExecutionException, MojoFailureException {
		deploy.outputDirectory = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test(expected = MojoFailureException.class)
	public void testFinalNameNull() throws MojoExecutionException, MojoFailureException {
		deploy.muleAppFileNameWithoutExt = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test(expected = MojoFailureException.class)
	public void testServerGroupNull() throws MojoExecutionException, MojoFailureException {
		deploy.serverOrGroup = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test
	public void testDeploymentNameNull() throws MojoExecutionException, MojoFailureException {
		deploy.deploymentName = null;
		deploy.execute();
		Assert.assertEquals("When null, deploymentName should be the name of the artifact", ARTIFACT_APP_NAME, deploy.deploymentName);
	}

	@Test
	public void testHappyPath() throws Exception {
		deploy.execute();
		verify(mockMuleRest).restfullyUploadRepository(REPOSITORY_APP_NAME, VERSION, _artifactPath);
		verify(mockMuleRest).restfullyCreateDeployment(SERVER_GROUP, DEPLOYED_APP_NAME, VERSION_ID);
		verify(mockMuleRest).restfullyDeployDeploymentById(DEPLOYMENT_ID);
	}
}