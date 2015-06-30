package org.mule.tools.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.tools.maven.plugin.Deploy;
import org.mule.tools.mmc.rest.DeploymentState;
import org.mule.tools.mmc.rest.DeploymentStatus;
import org.mule.tools.mmc.rest.MuleRest;

import static org.mockito.Mockito.*;

public class DeployTest {

	private static final String ARTIFACT_ID = "my_mule_app";

	private static final String MOCKED_VERSION_ID = "7959";
	private static final String MOCKED_DEPLOYMENT_ID = "1234";

	private static final String MMC_USERNAME = "muleuser1";
	private static final String MMC_PASSWORD = "pwd1234";
	private static final String TARGET_DEPLOYMENT_SERVER = "Development";

	private static final String VERSION = "1.0-SNAPSHOT";

	private Deploy deploy;

	private MuleRest mockMuleRest;

	private File _muleAppFile;

	private File _tempDirectory;

	@Before
	public void setup() throws Exception {
		deploy = spy(new Deploy());

		_tempDirectory = File.createTempFile("DeployUT", "");
		if (_tempDirectory.exists()) {
			_tempDirectory.delete();
		}
		_tempDirectory.mkdir();

		String finalName = ARTIFACT_ID + "-" + VERSION;

		_muleAppFile = new File(_tempDirectory, finalName + ".zip");
		_muleAppFile.createNewFile();

		setupMocks();
		Log log = new SystemStreamLog();

		deploy.setLog(log);

		deploy.artifactId = ARTIFACT_ID;
		deploy.version = VERSION;
		deploy.finalName = finalName;
		deploy.outputDirectory = _tempDirectory.getAbsolutePath();
		deploy.mmcApiUrl = "http://localhost:8080/mmc/api";
		deploy.mmcUsername = MMC_USERNAME;
		deploy.mmcPassword = MMC_PASSWORD;
		deploy.targetDeploymentServer = TARGET_DEPLOYMENT_SERVER;
	}

	@After
	public void cleanup() throws Exception {
		_tempDirectory.delete();
	}

	private void setupMocks() throws Exception {
		mockMuleRest = mock(MuleRest.class);
		when(deploy._createMuleRest(anyString(), anyString(), any(URL.class))).thenReturn(mockMuleRest);
		when(mockMuleRest.restfullyUploadRepository(anyString(), anyString(), any(File.class))).thenReturn(MOCKED_VERSION_ID);
		when(mockMuleRest.restfullyCreateDeployment(anyString(), anyString(), anyString())).thenReturn(MOCKED_DEPLOYMENT_ID);

		DeploymentState deploymentState = new DeploymentState();

		deploymentState.status = DeploymentStatus.DEPLOYED;

		when(mockMuleRest.restfullyGetDeploymentState(anyString())).thenReturn(deploymentState);
	}

	@Test
	public void testNominal() throws Exception {
		deploy.execute();
		verify(mockMuleRest).restfullyUploadRepository(ARTIFACT_ID, VERSION, _muleAppFile);
		verify(mockMuleRest).restfullyCreateDeployment(TARGET_DEPLOYMENT_SERVER, ARTIFACT_ID, MOCKED_VERSION_ID);
		verify(mockMuleRest).restfullyDeployDeploymentById(MOCKED_DEPLOYMENT_ID);
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
		deploy.finalName = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test(expected = MojoFailureException.class)
	public void testServerGroupNull() throws MojoExecutionException, MojoFailureException {
		deploy.targetDeploymentServer = null;
		deploy.execute();
		Assert.fail("Exception should have been thrown before this is called");
	}

	@Test
	public void testCustomRepositoryAppName() throws MojoExecutionException, MojoFailureException, IOException {
		String expectedRepositoryAppName = "MyCustomRepoAppName";
		deploy.customRepositoryAppName = expectedRepositoryAppName;
		Assert.assertNotSame(expectedRepositoryAppName, deploy.artifactId);

		deploy.execute();
		verify(mockMuleRest).restfullyUploadRepository(expectedRepositoryAppName, VERSION, _muleAppFile);
	}

	@Test
	public void testCustomRepositoryVersion() throws MojoExecutionException, MojoFailureException, IOException {
		String expectedRepositoryVersion = "MyCustomVersion";
		deploy.customRepositoryAppVersion = expectedRepositoryVersion;
		Assert.assertNotSame(expectedRepositoryVersion, deploy.version);

		deploy.execute();
		verify(mockMuleRest).restfullyUploadRepository(ARTIFACT_ID, expectedRepositoryVersion, _muleAppFile);
	}

	@Test
	public void testCustomDeploymentName() throws MojoExecutionException, MojoFailureException, IOException {
		String expectedDeploymentName = "MyCustomDeploymentName";
		deploy.customDeploymentName = expectedDeploymentName;
		Assert.assertNotSame(expectedDeploymentName, deploy.artifactId);

		deploy.execute();

		verify(mockMuleRest).restfullyCreateDeployment(TARGET_DEPLOYMENT_SERVER, expectedDeploymentName, MOCKED_VERSION_ID);
	}

}