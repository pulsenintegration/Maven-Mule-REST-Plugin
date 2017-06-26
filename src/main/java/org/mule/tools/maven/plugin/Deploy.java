package org.mule.tools.maven.plugin;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mule.tools.mmc.rest.DeploymentState;
import org.mule.tools.mmc.rest.DeploymentStatus;
import org.mule.tools.mmc.rest.MuleRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * @goal deploy
 * @execute phase="compile"
 * @requiresDirectInvocation true
 * @requiresDependencyResolution runtime
 * @requiresProject false
 * 
 * @author Nicholas A. Stuart
 * @author Mohamed EL HABIB
 */
public class Deploy extends AbstractMojo {

	public int DEPLOYMENT_TIMEOUT_MS = 30000;
	public int DEPLOYMENT_WAIT_SLEEP_MS = 500;

	private Logger _logger;

	/**
	 * The output directory coming from the pom.xml of the Mule app. This
	 * directory the location where the Mule app artifact is generated.
	 * 
	 * @parameter property="outputDirectory"
	 *            default-value="${project.build.directory}"
	 * @required
	 */
	protected String outputDirectory;

	/**
	 * The full name of the artifact (without extension) coming from the pom.xml
	 * of the Mule app
	 * 
	 * @parameter property="finalName"
	 *            default-value="${project.build.finalName}"
	 * @required
	 */
	protected String finalName;

	/**
	 * The artifactId coming from the pom.xml of the Mule app
	 * {@link Deploy#name}
	 * 
	 * @parameter property="artifactId" default-value="${project.artifactId}"
	 */
	protected String artifactId;

	/**
	 * The version coming from the pom.xml of the Mule app
	 * 
	 * @parameter property="version" default-value="${project.version}"
	 */
	protected String version;

	/**
	 * @parameter property="useTimestampVersion"
	 *            default-value="${useTimestampVersion}"
	 */
	protected Boolean useTimestampVersion = false;

	/**
	 * If true all parameters inherited from pom.xml values are ignored
	 * 
	 * @parameter property="noPomMode" default-value="${noPomMode}"
	 */
	protected boolean noPomMode = false;

	/**
	 * If specified, overrides the default location of the Mule app to deploy.
	 * The default Mule app location is deduced from the Maven Mule app project
	 * file (pom.xml). Overriding the Mule app file path can be useful when
	 * deploying an application outside of a project.
	 * 
	 * @parameter property="customMuleAppFilePath"
	 *            default-value="${customMuleAppFilePath}"
	 */
	protected String customMuleAppFilePath;

	/**
	 * The name that the application will have on the repository. If not
	 * specified, the name will be the artifactId
	 * 
	 * @parameter property="customRepositoryAppName"
	 *            default-value="${customRepositoryAppName}"
	 */
	protected String customRepositoryAppName;

	/**
	 * The version that the application will have on the repository. If not
	 * specified, the version will be taken from the pom.xml
	 * 
	 * @parameter property="customRepositoryAppVersion"
	 *            default-value="${customRepositoryAppVersion}"
	 */
	protected String customRepositoryAppVersion;

	/**
	 * The name that the application will be deployed as. If not specified, the
	 * name will be the artifactId
	 * 
	 * @parameter property="customDeploymentName"
	 *            default-value="${customDeploymentName}"
	 */
	protected String customDeploymentName;

	/**
	 * MMC (Mule Management Console) URL
	 * 
	 * @parameter property="mmcApiUrl" default-value="${mmcApiUrl}"
	 * @required
	 */
	protected String mmcApiUrl;

	/**
	 * MMC login username
	 * 
	 * @parameter property="mmcUsername" default-value="${mmcUsername}"
	 * @required
	 */
	protected String mmcUsername;

	/**
	 * MMC login password
	 * 
	 * @parameter property="mmcPassword" default-value="${mmcPassword}"
	 * @required
	 */
	protected String mmcPassword;

	/**
	 * Name of the server or server group where to deploy the Mule application
	 * 
	 * @parameter property="targetDeploymentServer"
	 *            default-value="${targetDeploymentServer}"
	 */
	protected String targetDeploymentServer;

	/**
	 * Time to wait for application to be deployed before throwing exception
	 * 
	 * @parameter property="deploymentTimeoutMs"
	 *            default-value="${deploymentTimeoutMs}"
	 */
	protected int deploymentTimeoutMs = DEPLOYMENT_TIMEOUT_MS;

	/**
	 * If true an exception fail be thrown if the deployment fails  
	 * 
	 * @parameter property="throwIfDeployFails" default-value="${throwIfDeployFails}"
	 */
	protected boolean throwIfDeployFails = false;

	/**
	 * If true skip execution
	 * 
	 * @parameter property="skip" default-value="false"
	 */
	protected boolean skip = false;
	
	/**
	 * Constructor
	 */
	public Deploy() {
		StaticLoggerBinder.getSingleton().setLog(getLog());
		this._logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (!skip) {			
			// Mule zip file to use
			File muleAppFile = this.getMuleAppFile();
	
			// Extract app name and version from Mule app file name
			MuleFileInfo muleFileInfo = MuleFileInfo.parseFromFile(muleAppFile.getName());
			String artifactIdToUse = this.noPomMode ? muleFileInfo.appName : this.artifactId;
			String artifactVersionToUse = this.noPomMode ? muleFileInfo.appVersion : this.version;
	
			// Mule app version on the repository
			String repositoryAppVersion = (this.useTimestampVersion) ? new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss").format(Calendar.getInstance().getTime()) : (!StringUtils.isEmpty(this.customRepositoryAppVersion) ? this.customRepositoryAppVersion : artifactVersionToUse);
	
			// Name of the Mule app on the repository
			String repositoryAppName = StringUtils.isEmpty(this.customRepositoryAppName) ? artifactIdToUse : this.customRepositoryAppName;
	
			// Name of the deployment
			String deploymentName = StringUtils.isEmpty(this.customDeploymentName) ? artifactIdToUse : this.customDeploymentName;
	
			// MMC username and password
			String mmcUsername, mmcPassword;
			if (this.mmcUsername == null || this.mmcPassword == null) {
				throw new MojoFailureException("mmcUsername and/or mmcPassword not set.");
			}
			mmcUsername = this.mmcUsername;
			mmcPassword = this.mmcPassword;
	
			// URL of the MMC
			URL mmcApiUrl = getMmcApiUrl();
	
			// Target deployment server
			String targetDeploymentServer = this.targetDeploymentServer;
	
			// Deployment timeout
			int deploymentTimeoutMs = this.deploymentTimeoutMs;
	
			_logDeploymentSummary(muleAppFile.getAbsolutePath(), mmcApiUrl.getPath(), mmcUsername, mmcPassword, repositoryAppName, repositoryAppVersion, deploymentName, targetDeploymentServer, deploymentTimeoutMs);
	
			try {
				MuleRest muleRest = _createMuleRest(mmcUsername, mmcPassword, mmcApiUrl);
	
				String versionId = muleRest.restfullyUploadRepository(repositoryAppName, repositoryAppVersion, muleAppFile);
				if (targetDeploymentServer != null) {
					String deploymentId = muleRest.restfullyCreateDeployment(targetDeploymentServer, deploymentName, versionId);
					muleRest.restfullyDeployDeploymentById(deploymentId);
		
					DeploymentState deploymentState = null;
		
					long startTime = System.currentTimeMillis();
		
					// Wait for application to be deployed
					while (true) {
						deploymentState = muleRest.restfullyGetDeploymentState(deploymentId);
						if (deploymentState.status == DeploymentStatus.IN_PROGRESS) {
							long elaspedTime = System.currentTimeMillis() - startTime;
		
							if (elaspedTime > this.deploymentTimeoutMs) {
								throw new TimeoutException("Timeout of \"" + deploymentTimeoutMs + "ms\" occurred while waiting for Mule application \"" + versionId + "\" to be deployed");
							}
		
							Thread.sleep(DEPLOYMENT_WAIT_SLEEP_MS);
							continue;
						} else if (deploymentState.status == DeploymentStatus.DEPLOYED) {
							_logger.info("Application \"" + muleAppFile.getAbsolutePath() + "\" successfully deployed in deployment \"" + deploymentName + "\".");
							break;
						} else {
							String msg = "Failed to deploy application with deployment id \"" + deploymentId + "\", unexpected deployment state \"" + deploymentState.status + "\"";
							if (throwIfDeployFails) {
								throw new Exception(msg);							
							}
							_logger.warn(msg);
							break;
						}
					}
				}
			} catch (Exception e) {
				throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
			}
		} else {
			getLog().info("Skipping execution.");
		}
	}

	private static String _getStrRepr(String str) {
		return str == null ? "null" : "\"" + str + "\"";
	}

	private void _logDeploymentSummary(String muleZipFile, String mmcApiUrl, String mmcUsername, String mmcPassword, String repositoryAppName, String uploadedRepositoryVersion, String deploymentName, String targetDeploymentServer, int deploymentTimeoutMs) {
		this._logger.info("___MULE APPLICATION DEPLOYMENT SUMMARY___");
		this._logger.info("> Artifact to be deployed : " + (muleZipFile == null ? "null" : "\"" + muleZipFile + "\""));

		this._logger.debug("> MMC URL : " + _getStrRepr(mmcApiUrl));
		this._logger.debug("> Username : " + _getStrRepr(mmcUsername));
		this._logger.trace("> Password : " + _getStrRepr(mmcPassword));

		this._logger.info("> App name on the repository : " + _getStrRepr(repositoryAppName));
		this._logger.info("> App version on the repository : " + _getStrRepr(uploadedRepositoryVersion));

		this._logger.info("> Name of the deployment : " + _getStrRepr(deploymentName));
		if (targetDeploymentServer != null) {
			this._logger.info("> Target server or group : " + _getStrRepr(targetDeploymentServer));
		}
		this._logger.debug("> Deployment timeout (ms) : " + deploymentTimeoutMs);
	}

	private URL getMmcApiUrl() throws MojoFailureException {
		if (StringUtils.isEmpty(this.mmcApiUrl)) {
			throw new MojoFailureException("mmcApiUrl is not defined");
		}

		URL mmcUrl;
		try {
			mmcUrl = new URL(this.mmcApiUrl);
		} catch (Exception ex) {
			throw new MojoFailureException("Invalid mmcApiUrl \"" + this.mmcApiUrl + "\" : " + ex.getMessage());
		}

		return mmcUrl;
	}

	protected MuleRest _createMuleRest(String mmcUsername, String mmcPassword, URL mmcApiUrl) {
		return new MuleRest(mmcApiUrl, mmcUsername, mmcPassword);
	}

	protected File getMuleAppFile() throws MojoFailureException {

		if (this.noPomMode) {
			if (StringUtils.isEmpty(this.customMuleAppFilePath)) {
				throw new MojoFailureException("In noPomMode, customMuleAppFilePath should be specified");
			}

			try {
				return FileFinder.find(this.customMuleAppFilePath);
			} catch (Exception e) {
				throw new MojoFailureException(e.getMessage());
			}

		} else {
			if (StringUtils.isEmpty(this.outputDirectory)) {
				throw new MojoFailureException("Output directory undefined, make sure the pom.xml contains a valid path for project.build.directory");
			}
			if (StringUtils.isEmpty(this.finalName)) {
				throw new MojoFailureException("Final name undefined, make sure the pom.xml contains a valid file name for project.build.finalName");
			}

			return new File(this.outputDirectory, this.finalName + ".zip");

		}
	}

}