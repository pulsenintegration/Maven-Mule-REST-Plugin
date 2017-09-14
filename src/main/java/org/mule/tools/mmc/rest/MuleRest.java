package org.mule.tools.mmc.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleRest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger _logger = LoggerFactory.getLogger(MuleRest.class);
	private static final String SNAPSHOT = "SNAPSHOT";

	private URL mmcUrl;
	private String username;
	private String password;

	/**
	 * Constructor
	 * 
	 * @param mmcUrl
	 * @param username
	 * @param password
	 */
	public MuleRest(URL mmcUrl, String username, String password) {
		this.mmcUrl = mmcUrl;
		this.username = username;
		this.password = password;
		_logger.debug("MMC URL: {}, Username: {}", mmcUrl, username);
	}

	private WebClient _getWebClient(String... paths) {
		WebClient webClient = WebClient.create(mmcUrl.toString(), username, password, null);
		HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
		for (String path : paths) {
			webClient.path(path);
		}
		if (_logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (String path : paths) {
				sb.append('/');
				sb.append(path);
			}
			_logger.trace("WebClient path: "+sb.toString());
		}
		return webClient;
	}

	private String _processResponse(Response response) throws IOException {
		int statusCode = response.getStatus();
		String responseText = IOUtils.toString((InputStream) response.getEntity());

		if (statusCode == Status.OK.getStatusCode() || statusCode == Status.CREATED.getStatusCode()) {
			return responseText;
		}
		_logger.trace("MMC Response: {}", responseText);
		if (statusCode == Status.NOT_FOUND.getStatusCode()) {
			throw new HTTPException(statusCode, "The resource was not found.", mmcUrl);
		} else if (statusCode == Status.CONFLICT.getStatusCode()) {
			throw new HTTPException(statusCode, "The operation was unsuccessful because a resource with that name already exists.", mmcUrl);
		} else if (statusCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			throw new HTTPException(statusCode, "The operation was unsuccessful.", mmcUrl);
		} else {
			throw new HTTPException(statusCode, "Unexpected MMC returned status code \"" + statusCode + "\". Response was \"" + responseText + "\".", mmcUrl);
		}
	}

	/**
	 * Creates a new deployment without deploying the application referenced by
	 * the version id. To deploy the application, see method
	 * {@link #restfullyDeployDeploymentById(String)}
	 * 
	 * @param targetServerName
	 *            Name of the server or group where to deploy the application
	 * @param name
	 *            Name of the deployment
	 * @param versionId
	 *            Version id of an application on the repository
	 * @return Returns the id of the deployment
	 * @throws IOException
	 * @throws Exception
	 */
	public String restfullyCreateDeployment(String targetServerName, String name, String versionId) throws IOException {
		_logger.trace("START: restfullyCreateDeployment");
		String serverOrGroupId = restfullyGetServerGroupId(targetServerName);
		if (StringUtils.isEmpty(serverOrGroupId)) {
			serverOrGroupId = restfullyGetServerId(targetServerName);
		}
		String clusterId = null;
		if (StringUtils.isEmpty(serverOrGroupId)) {
			clusterId = restfullyGetClusterId(targetServerName);
		}

		if (StringUtils.isEmpty(serverOrGroupId) && StringUtils.isEmpty(clusterId)) {
			throw new IllegalArgumentException("No group, server or cluster named \"" + targetServerName + "\" found");
		}

		JsonNode deployment = restfullyGetDeploymentByName(name, serverOrGroupId, clusterId);

		WebClient webClient = _getWebClient("deployments");
		webClient.type(MediaType.APPLICATION_JSON_TYPE);
		try {
			if (deployment != null) {
				webClient.path(deployment.path("id").getTextValue());
				webClient.path("remove");
				
				String deploymentJson = createDeploymentJSON(name,deployment.path("lastModified").getTextValue(), deployment.path("applications").get(0).asText());
				
				deployment = doHttpRequest("PUT", deploymentJson, webClient);
				deploymentJson = createDeploymentJSON(name, deployment.path("lastModified").getTextValue(), versionId);
				
				webClient.back(false);
				webClient.back(false);
				webClient.path(deployment.path("id").getTextValue());
				webClient.path("add");
				
				deployment = doHttpRequest("PUT", deploymentJson, webClient);
				return deployment.path("id").getTextValue();
			}
			else {
				String deploymentJson = createDeploymentJSON(name, null, versionId, serverOrGroupId, clusterId);
	
				JsonNode jsonNode =  doHttpRequest("POST", deploymentJson, webClient);
				String deploymentId = jsonNode.path("id").getTextValue();
	
				_logger.info("Deployment successfully created with id \"" + deploymentId + "\"");
	
				return deploymentId;
			}
		} finally {
			webClient.close();
			_logger.trace("END: restfullyCreateDeployment");
		}
	}
	
	private JsonNode doHttpRequest(String method, String body, WebClient webClient) throws IOException {
		int retries = 0;
		String responseText;
		while (true) {
			_logger.trace(method+" \n"+body);
			Response response;
			if (method.equals("POST")) {
				response = webClient.post(body);
			}
			else if (method.equals("PUT")) {
				response = webClient.put(body);				
			}
			else {
				throw new UnsupportedOperationException(method);
			}
			
			try {
				responseText = _processResponse(response);
				break;
			} 
			catch (HTTPException he) {
				if (retries > 1) {
					throw he;
				}
				sleep(1000);
				_logger.info("Retrying...");
				retries++;
			}
		}
		return OBJECT_MAPPER.readTree(responseText);		
	}
	
	private String createDeploymentJSON(String name, String lastModified, String appVersionId) throws IOException {
		return createDeploymentJSON(name, lastModified, appVersionId, null, null);
	}
	
	private String createDeploymentJSON(String name, String lastModified, String appVersionId, String serverId, String clusterId) throws IOException {
		StringWriter stringWriter = new StringWriter();
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator jGenerator = jfactory.createJsonGenerator(stringWriter);
		jGenerator.writeStartObject(); // {
		jGenerator.writeFieldName("applications"); // "applications" :
		jGenerator.writeStartArray(); // [
		jGenerator.writeString(appVersionId); // "application version Id"
		jGenerator.writeEndArray(); // ]
		if (!StringUtils.isEmpty(clusterId)) { 
			jGenerator.writeFieldName("clusters"); // "clusters" :
			jGenerator.writeStartArray(); // [
			jGenerator.writeString(clusterId); // "clusterId"
			jGenerator.writeEndArray(); // ]
		}
		if (lastModified != null) {
			jGenerator.writeStringField("lastModified", lastModified); // "lastModified" : lastModified
		}
		jGenerator.writeStringField("name", name); // "name" : name
		if (!StringUtils.isEmpty(serverId)) { 
			jGenerator.writeFieldName("servers"); // "servers" :
			jGenerator.writeStartArray(); // [
			jGenerator.writeString(serverId); // "serverId"
			jGenerator.writeEndArray(); // ]
		}
		jGenerator.writeEndObject(); // }
		jGenerator.close();
		
		return stringWriter.toString();
	}

	public void restfullyDeleteDeploymentById(String deploymentId) throws IOException {
		_logger.trace("START: restfullyDeleteDeploymentById");
		WebClient webClient = _getWebClient("deployments", deploymentId);

		try {
			int retries = 0;
			while (true) {
				try {
					_logger.trace("DELETE");
					Response response = webClient.delete();
					_processResponse(response);
					break;
				} catch (HTTPException he) {
					if (retries > 1) {
						throw he;
					}
					sleep(1000);
					_logger.info("Retrying...");
					retries++;					
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyDeleteDeploymentById");
	}

	/**
	 * Deploy the deployment from the deployment id
	 * 
	 * @param deploymentId
	 *            Id of the deployment
	 * @throws IOException
	 */
	public void restfullyDeployDeploymentById(String deploymentId) throws IOException {
		_logger.trace("START: restfullyDeployDeploymentById");
		WebClient webClient = _getWebClient("deployments", deploymentId, "deploy");

		try {
			int retries = 0;
			while (true) {
				_logger.trace("POST");
				Response response = webClient.post(null);
				try {
					String responseText = _processResponse(response);
					_logger.info("Application deployed with answer \"" + responseText + "\"");
					break;
				} 
				catch (HTTPException he) {
					if (retries > 1) {
						throw he;
					}
					sleep(1000);
					_logger.info("Retrying...");
					retries++;					
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyDeployDeploymentById");
	}

	/**
	 * Returns the deployment id from the deployment name
	 * 
	 * @param deploymentName
	 * @return
	 * @throws IOException
	 */
	public String restfullyGetDeploymentIdByName(String deploymentName, String serverId, String clusterId) throws IOException {
		_logger.trace("START: restfullyGetDeploymentIdByName");
		WebClient webClient = _getWebClient("deployments");
		if (serverId != null) {
			webClient.query("serverId", serverId);
		}
		else if (clusterId != null) {
			webClient.query("clusterId", clusterId);			
		}

		String deploymentId = null;
		try {
			_logger.trace("GET");
			Response response = webClient.get();

			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode deploymentsNode = jsonNode.path("data");
			for (JsonNode deploymentNode : deploymentsNode) {
				if (deploymentName.equals(deploymentNode.path("name").getTextValue())) {
					deploymentId = deploymentNode.path("id").getTextValue();
					break;
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyGetDeploymentIdByName");
		return deploymentId;
	}

	/**
	 * Returns the deployment from the deployment name
	 * 
	 * @param deploymentName
	 * @return
	 * @throws IOException
	 */
	public JsonNode restfullyGetDeploymentByName(String deploymentName, String serverId, String clusterId) throws IOException {
		_logger.trace("START: restfullyGetDeploymentByName");
		WebClient webClient = _getWebClient("deployments");
//		if (serverId != null) {
//			webClient.query("serverId", serverId);
//		}
//		else if (clusterId != null) {
//			webClient.query("clusterId", clusterId);			
//		}

		JsonNode deployment = null;
		try {
			_logger.trace("GET");
			Response response = webClient.get();

			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode deploymentsNode = jsonNode.path("data");
			for (JsonNode deploymentNode : deploymentsNode) {
				if (deploymentName.equals(deploymentNode.path("name").getTextValue())) {
					deployment = deploymentNode;
					break;
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyGetDeploymentByName");
		return deployment;
	}
	
	
	/**
	 * Get deployment info from deployment id
	 * 
	 * @param deploymentId
	 * @return
	 * @throws IOException
	 */
	public DeploymentState restfullyGetDeploymentState(String deploymentId) throws IOException {
		_logger.trace("START: restfullyGetDeploymentState");
		WebClient webClient = _getWebClient("deployments", deploymentId);
		try {
			_logger.trace("GET");
			Response response = webClient.get();
			String responseText = _processResponse(response);

			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			DeploymentState deploymentState = new DeploymentState();
			deploymentState.reconciled = jsonNode.path("reconciled").getBooleanValue();
			deploymentState.status = DeploymentStatus.valueOf(jsonNode.path("status").getTextValue().toUpperCase());
			deploymentState.href = jsonNode.path("href").getTextValue();
			deploymentState.name = jsonNode.path("name").getTextValue();

			return deploymentState;

		} finally {
			webClient.close();
			_logger.trace("END: restfullyGetDeploymentState");
		}
	}

	public String restfullyGetApplicationId(String name, String version) throws IOException {
		_logger.trace("START: restfullyGetApplicationId");
		WebClient webClient = _getWebClient("repository");

		String applicationId = null;
		try {
			_logger.trace("GET");
			Response response = webClient.get();

			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode applicationsNode = jsonNode.path("data");
			for (JsonNode applicationNode : applicationsNode) {
				if (name.equals(applicationNode.path("name").getTextValue())) {
					JsonNode versionsNode = applicationNode.path("versions");
					for (JsonNode versionNode : versionsNode) {
						if (version.equals(versionNode.path("name").getTextValue())) {
							applicationId = versionNode.get("id").getTextValue();
							break;
						}
					}
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyGetApplicationId");
		return applicationId;
	}

	/**
	 * Returns id of given group name or null if not found
	 * 
	 * @param serverGroupName
	 * @return
	 * @throws IOException
	 */
	public final String restfullyGetServerGroupId(String serverGroupName) throws IOException {
		_logger.trace("START: restfullyGetServerGroupId");
		String serverGroupId = null;
		WebClient webClient = _getWebClient("serverGroups");
		try {
			_logger.trace("GET");
			Response response = webClient.get();

			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode groupsNode = jsonNode.path("data");
			for (JsonNode groupNode : groupsNode) {
				if (serverGroupName.equals(groupNode.path("name").getTextValue())) {
					serverGroupId = groupNode.path("id").getTextValue();
					break;
				}
			}
			return serverGroupId;
		} finally {
			webClient.close();
			_logger.trace("END: restfullyGetServerGroupId");
		}
	}

	/**
	 * Returns ids of all servers in given group name
	 * 
	 * @param serverGroupName
	 * @return
	 * @throws IOException
	 */
	public Set<String> restfullyGetServerIdsInGroup(String serverGroupName) throws IOException {
		_logger.trace("START: restfullyGetServerIdsInGroup");
		Set<String> serversId = new TreeSet<String>();
		WebClient webClient = _getWebClient("servers");

		try {
			_logger.trace("GET");
			Response response = webClient.get();

			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode serversNode = jsonNode.path("data");
			for (JsonNode serverNode : serversNode) {
				String serverId = serverNode.path("id").getTextValue();

				JsonNode groupsNode = serverNode.path("groups");
				for (JsonNode groupNode : groupsNode) {
					if (serverGroupName.equals(groupNode.path("name").getTextValue())) {
						serversId.add(serverId);
					}
				}
			}
		} finally {
			webClient.close();
		}
		_logger.trace("END: restfullyGetServerIdsInGroup");
		return serversId;
	}

	/**
	 * Returns id of given server name or null if not found
	 * 
	 * @param serverName
	 * @return
	 * @throws IOException
	 */
	public String restfullyGetServerId(String serverName) throws IOException {
		_logger.trace("START: restfullyGetServerId");
		String serverId = null;
		WebClient webClient = _getWebClient("servers");

		try {
			_logger.trace("GET");
			Response response = webClient.get();
			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode serversNode = jsonNode.path("data");
			for (JsonNode serverNode : serversNode) {
				if (serverName.equals(serverNode.path("name").getTextValue())) {
					serverId = serverNode.path("id").getTextValue();
					break;
				}
			}

			return serverId;
		} finally {
			webClient.close();
			_logger.trace("END: restfullyGetServerId");
		}
	}

	/**
	 * Returns id of given cluster name or null if not found
	 * 
	 * @param clusterName
	 * @return
	 * @throws IOException
	 */
	public String restfullyGetClusterId(String clusterName) throws IOException {
		_logger.trace("START: restfullyGetClusterId");
		String clusterId = null;
		WebClient webClient = _getWebClient("clusters");

		try {
			_logger.trace("GET");
			Response response = webClient.get();
			String responseText = _processResponse(response);
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseText);
			JsonNode clustersNode = jsonNode.path("data");
			for (JsonNode clusterNode : clustersNode) {
				if (clusterName.equals(clusterNode.path("name").getTextValue())) {
					clusterId = clusterNode.path("id").getTextValue();
					break;
				}
			}

			return clusterId;
		} finally {
			webClient.close();
			_logger.trace("END: restfullyGetClusterId");
		}
	}
	
	/**
	 * Uploads application to the repository and returns the version id
	 * 
	 * @param appName
	 *            Name of the application on the repository
	 * @param appVersion
	 *            version of the application on the repository
	 * @param packageFile
	 *            The application file
	 * @return The id of the uploaded application on the repository
	 * @throws IOException
	 */

	public String restfullyUploadRepository(String appName, String appVersion, File packageFile) throws IOException {
		_logger.trace("START: restfullyUploadRepository");
		WebClient webClient = _getWebClient("repository");
		webClient.type("multipart/form-data");

		try {
			// delete application first
			if (isSnapshotVersion(appVersion)) {
				restfullyDeleteApplication(appName, appVersion);
			}
			Attachment nameAttachment = new AttachmentBuilder().id("name").object(appName).contentDisposition(new ContentDisposition("form-data; name=\"name\"")).build();
			Attachment versionAttachment = new AttachmentBuilder().id("version").object(appVersion).contentDisposition(new ContentDisposition("form-data; name=\"version\"")).build();
			Attachment fileAttachment = new Attachment("file", new FileInputStream(packageFile), new ContentDisposition("form-data; name=\"file\"; filename=\"" + packageFile.getName() + "\""));

			MultipartBody multipartBody = new MultipartBody(Arrays.asList(fileAttachment, nameAttachment, versionAttachment), MediaType.MULTIPART_FORM_DATA_TYPE, true);

			_logger.trace("POST");
			Response response = webClient.post(multipartBody);

			String responseObject = _processResponse(response);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode result = mapper.readTree(responseObject);
			return result.path("versionId").getTextValue();
		} finally {
			webClient.close();
			_logger.trace("END: restfullyUploadRepository");
		}
	}

	public void restfullyDeleteApplicationById(String applicationVersionId) throws IOException {
		_logger.trace("START: restfullyDeleteApplicationById");
		WebClient webClient = _getWebClient("repository", applicationVersionId);

		try {
			_logger.trace("DELETE");
			Response response = webClient.delete();
			_processResponse(response);
		} finally {
			webClient.close();
			_logger.trace("END: restfullyDeleteApplicationById");
		}
	}

	public void restfullyDeleteApplication(String applicationName, String version) throws IOException {
		int retries = 0;
		while (true) {
			try {
				String applicationVersionId = restfullyGetApplicationId(applicationName, version);
				if (applicationVersionId != null) {
					restfullyDeleteApplicationById(applicationVersionId);
				}
				break;
			}
			catch (HTTPException he) {
				if (retries > 1) {
					throw he;
				}
				sleep(1000);
				_logger.info("Retrying...");
				retries++;					
			}
		}
	}

	protected boolean isSnapshotVersion(String version) {
		return version.contains(SNAPSHOT);
	}
	
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}