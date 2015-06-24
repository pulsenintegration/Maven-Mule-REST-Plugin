package org.mule.tools.maven.rest;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * @goal deploy
 * @execute phase="compile"
 * @requiresDirectInvocation true
 * @requiresDependencyResolution runtime
 * 
 * @author Nicholas A. Stuart
 * @author Mohamed EL HABIB
 */
public class Deploy extends AbstractMojo {

	
	private Logger _logger;

	/**
	 * Directory containing the generated Mule App.
	 * 
	 * @parameter property="outputDirectory"
	 *            default-value="${project.build.directory}"
	 * @required
	 */
	protected File outputDirectory;

	/**
	 * Name of the generated Mule App without extension. (This field is
	 * initialized with internal Maven variable ${project.build.finalName} which
	 * is corresponding to the Mule application artifact name)
	 * 
	 * @parameter property="muleAppFileNameWithoutExt"
	 *            default-value="${project.build.finalName}"
	 * @required
	 */
	protected String muleAppFileNameWithoutExt;

	/**
	 * The name of the application in the repository. Default is
	 * "MuleApplication"
	 * 
	 * @parameter property="repositoryAppName"
	 *            default-value="${repositoryAppName}"
	 */
	protected String repositoryAppName;

	/**
	 * The name that the application will be deployed as. Default is same as
	 * {@link Deploy#name}
	 * 
	 * @parameter property="deploymentName" default-value="${deploymentName}"
	 */
	protected String deploymentName;

	/**
	 * The version that the application will be have in the repository. Default
	 * is the project version.
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
	 * MMC (Mule Management Console) URL
	 * 
	 * @parameter property="mmcApiUrl" default-value="${mmcApiUrl}"
	 * @required
	 */
	protected URL mmcApiUrl;

	/**
	 * Name of the server or server group where to deploy the Mule application
	 * 
	 * @parameter property="serverOrGroup" default-value="${serverOrGroup}"
	 * @required
	 */
	protected String serverOrGroup;

	private MuleRest _muleRest;

	public Deploy(){
		StaticLoggerBinder.getSingleton().setLog(getLog());
		this._logger = LoggerFactory.getLogger(getClass());
	}
	
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {


		if (StringUtils.isEmpty(repositoryAppName)) {
			repositoryAppName = this.muleAppFileNameWithoutExt;
		}

		if (StringUtils.isEmpty(deploymentName)) {
			deploymentName = this.muleAppFileNameWithoutExt;
		}

		if (this.useTimestampVersion) {
			version = new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss").format(Calendar.getInstance().getTime());
		}

		if (mmcUsername == null || mmcPassword == null) {
			throw new MojoFailureException("mmcUsername and/or mmcPassword not set.");
		}

		if (outputDirectory == null) {
			throw new MojoFailureException("outputDirectory not set.");
		}
		if (muleAppFileNameWithoutExt == null) {
			throw new MojoFailureException("muleAppFileName not set.");
		}
		if (serverOrGroup == null) {
			throw new MojoFailureException("serverOrGroup not set.");
		}

		File muleZipFile = getMuleZipFile(outputDirectory, muleAppFileNameWithoutExt);
		
		_logDeploymentSummary(muleZipFile.toString());

		
		try {
			_muleRest = buildMuleRest();

			String versionId = _muleRest.restfullyUploadRepository(this.repositoryAppName, version, muleZipFile);
			String deploymentId = _muleRest.restfullyCreateDeployment(serverOrGroup, deploymentName, versionId);
			_muleRest.restfullyDeployDeploymentById(deploymentId);
		} catch (Exception e) {
			throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
		}
	}

	private void _logDeploymentSummary(String muleZipFile) {
		this._logger.info("___MULE APPLICATION DEPLOYMENT SUMMARY___");
		this._logger.info("> Artifact to be deployed : " + (muleZipFile == null ? "null" : "\"" + muleZipFile + "\""));
		
		this._logger.info("> MMC URL : " + (this.mmcApiUrl == null ? "null" : "\"" + this.mmcApiUrl + "\""));
		this._logger.info("> Username : " + (this.mmcUsername == null ? "null" : "\"" + this.mmcUsername + "\""));
		this._logger.info("> Password : " + (this.mmcPassword == null ? "null" : "\"" + this.mmcPassword + "\""));

		this._logger.info("> App name on the repository : " + (this.repositoryAppName == null ? "null" : "\"" + this.repositoryAppName + "\""));
		this._logger.info("> App version on the repository : " + (this.version == null ? "null" : "\"" + this.version + "\""));
		
		this._logger.info("> Deployed application name : " + (this.deploymentName == null ? "null" : "\"" + this.deploymentName + "\""));
		this._logger.info("> Target server or group : " + (this.serverOrGroup == null ? "null" : "\"" + this.serverOrGroup + "\""));
	}

	protected File getMuleZipFile(File outputDirectory, String filename) throws MojoFailureException {
		File file = new File(outputDirectory, filename + ".zip");
		if (!file.exists()) {
			throw new MojoFailureException("There no application ZIP file generated : check that you have configured the maven-mule-plugin to generated the this file");
		}
		return file;
	}

	protected MuleRest buildMuleRest() {
		return new MuleRest(mmcApiUrl, mmcUsername, mmcPassword);
	}

}