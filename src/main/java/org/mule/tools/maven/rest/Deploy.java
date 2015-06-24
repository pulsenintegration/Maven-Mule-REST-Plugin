package org.mule.tools.maven.rest;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

	public static final String DEFAULT_NAME = "MuleApplication";

	/**
	 * Directory containing the generated Mule App.
	 * 
	 * @parameter property="outputDirectory"
	 *            default-value="${project.build.directory}"
	 * @required
	 */
	protected File outputDirectory;

	/**
	 * Directory containing the app resources.
	 * 
	 * @parameter property="appDirectory"
	 *            default-value="${basedir}/src/main/app"
	 * @required
	 */
	protected File appDirectory;

	/**
	 * Name of the generated Mule App. (This field is initialized with internal
	 * Maven variable ${project.build.finalName} which is corresponding to the
	 * Mule application artifact name)
	 * 
	 * @parameter property="muleAppFileName"
	 *            default-value="${project.build.finalName}"
	 * @required
	 */
	protected String muleAppFileName;

	/**
	 * The name of the application in the repository. Default is
	 * "MuleApplication"
	 * 
	 * @parameter property="name" default-value="${name}"
	 */
	protected String name;

	/**
	 * The name that the application will be deployed as. Default is same as
	 * {@link Deploy#name}
	 * 
	 * @parameter property="deploymentName" default-value="${deploymentName}"
	 */
	protected String deploymentName;

	/**
	 * The version that the application will be deployed as. Default is the
	 * current time in milliseconds.
	 * 
	 * @parameter property="version" default-value="${version}"
	 */
	protected String version;

	/**
	 * MMC login username
	 * 
	 * @parameter property="username" default-value="${username}"
	 * @required
	 */
	protected String username;

	/**
	 * MMC login password
	 * 
	 * @parameter property="password" default-value="${password}"
	 * @required
	 */
	protected String password;

	/**
	 * MMC (Mule Management Console) URL
	 * 
	 * @parameter property="muleApiUrl" default-value="${muleApiUrl}"
	 * @required
	 */
	protected URL muleApiUrl;

	/**
	 * Name of the server or server group where to deploy the Mule application
	 * 
	 * @parameter property="serverOrGroup" default-value="${serverOrGroup}"
	 * @required
	 */
	protected String serverOrGroup;

	private MuleRest _muleRest;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		StaticLoggerBinder.getSingleton().setLog(getLog());
		Logger logger = LoggerFactory.getLogger(getClass());

		_logDebug(logger);

		if (name == null) {
			logger.info("Name is not set, using default \"{}\"", DEFAULT_NAME);
			name = DEFAULT_NAME;
		}
		if (deploymentName == null) {
			logger.info("DeploymentName is not set, using application name \"{}\"", name);
			deploymentName = name;
		}
		if (version == null) {
			version = new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss").format(Calendar.getInstance().getTime());
			logger.info("Version is not set, using a default of the timestamp: {}", version);
		}
		if (username == null || password == null) {
			throw new MojoFailureException((username == null ? "Username" : "Password") + " not set.");
		}
		if (outputDirectory == null) {
			throw new MojoFailureException("outputDirectory not set.");
		}
		if (muleAppFileName == null) {
			throw new MojoFailureException("muleAppFileName not set.");
		}
		if (serverOrGroup == null) {
			throw new MojoFailureException("serverOrGroup not set.");
		}
		try {
			validateProject(appDirectory);
			_muleRest = buildMuleRest();
			String versionId = _muleRest.restfullyUploadRepository(name, version, getMuleZipFile(outputDirectory, muleAppFileName));
			String deploymentId = _muleRest.restfullyCreateDeployment(serverOrGroup, deploymentName, versionId);
			_muleRest.restfullyDeployDeploymentById(deploymentId);
		} catch (Exception e) {
			throw new MojoFailureException("Error in attempting to deploy archive: " + e.toString(), e);
		}
	}

	private void _logDebug(Logger logger) {
		logger.debug(this.getClass().getName() + " fields :");
		logger.debug("deploymentName=" + (this.deploymentName == null ? "null" : "\"" + this.deploymentName + "\""));
		logger.debug("name=" + (this.name == null ? "null" : "\"" + this.name + "\""));
		logger.debug("muleAppFileName=" + (this.muleAppFileName == null ? "null" : "\"" + this.muleAppFileName + "\""));
		logger.debug("muleApiUrl=" + (this.muleApiUrl == null ? "null" : "\"" + this.muleApiUrl + "\""));
		logger.debug("username=" + (this.username == null ? "null" : "\"" + this.username + "\""));
		logger.debug("password=" + (this.password == null ? "null" : "\"" + this.password + "\""));
		logger.debug("serverOrGroup=" + (this.serverOrGroup == null ? "null" : "\"" + this.serverOrGroup + "\""));
		logger.debug("version=" + (this.version == null ? "null" : "\"" + this.version + "\""));
		logger.debug("appDirectory=" + (this.appDirectory == null ? "null" : "\"" + this.appDirectory + "\""));
		logger.debug("outputDirectory=" + (this.outputDirectory == null ? "null" : "\"" + this.outputDirectory + "\""));
	}

	protected File getMuleZipFile(File outputDirectory, String filename) throws MojoFailureException {
		File file = new File(outputDirectory, filename + ".zip");
		if (!file.exists()) {
			throw new MojoFailureException("There no application ZIP file generated : check that you have configured the maven-mule-plugin to generated the this file");
		}
		return file;
	}

	protected void validateProject(File appDirectory) throws MojoExecutionException {
		File muleConfig = new File(appDirectory, "mule-config.xml");
		File deploymentDescriptor = new File(appDirectory, "mule-deploy.properties");

		if ((muleConfig.exists() == false) && (deploymentDescriptor.exists() == false)) {
			throw new MojoExecutionException("No mule-config.xml or mule-deploy.properties");
		}
	}

	protected MuleRest buildMuleRest() {
		return new MuleRest(muleApiUrl, username, password);
	}

}