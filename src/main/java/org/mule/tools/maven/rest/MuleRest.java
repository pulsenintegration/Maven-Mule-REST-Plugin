package org.mule.tools.maven.rest;

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
import org.apache.cxf.transport.http.HTTPException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleRest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(MuleRest.class);
	private static final String SNAPSHOT = "SNAPSHOT";

	private URL mmcUrl;
	private String username;
	private String password;

	public MuleRest(URL mmcUrl, String username, String password) {
		this.mmcUrl = mmcUrl;
		this.username = username;
		this.password = password;
		logger.debug("MMC URL: {}, Username: {}", mmcUrl, username);
	}

	private WebClient getWebClient(String... paths) {
		WebClient webClient = WebClient.create(mmcUrl.toString(), username, password, null);
		for (String path : paths) {
			webClient.path(path);
		}
		return webClient;
	}

	private String processResponse(Response response) throws IOException {
		int statusCode = response.getStatus();
		String responseObject = IOUtils.toString((InputStream) response.getEntity());

		if (statusCode == Status.OK.getStatusCode() || statusCode == Status.CREATED.getStatusCode()) {
			return responseObject;
		} else if (statusCode == Status.NOT_FOUND.getStatusCode()) {
			HTTPException he = new HTTPException(statusCode, "The resource was not found.", mmcUrl);
			throw he;
		} else if (statusCode == Status.CONFLICT.getStatusCode()) {
			HTTPException he = new HTTPException(statusCode, "The operation was unsuccessful because a resource with that name already exists.", mmcUrl);
			throw he;
		} else if (statusCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			HTTPException he = new HTTPException(statusCode, "The operation was unsuccessful.", mmcUrl);
			throw he;
		} else {
			HTTPException he = new HTTPException(statusCode, "Unexpected Status Code Return, Status Line: " + statusCode, mmcUrl);
			throw he;
		}
	}

	/**
	 * Creates a new deployment without deploying the application referenced by
	 * the version id. To deploy the application, see method
	 * {@link #restfullyDeployDeploymentById(String)}
	 * 
	 * @param serverOrGroup
	 *            Name of the server or group where to deploy the application
	 * @param name
	 *            Name of the deployment
	 * @param versionId
	 *            Version id of an application on the repository
	 * @return Returns the id of the deployment
	 * @throws IOException
	 * @throws Exception
	 */
	public String restfullyCreateDeployment(String serverOrGroup, String name, String versionId) throws IOException {
		String serverOrGroupId = restfullyGetServerGroupId(serverOrGroup);
		if (StringUtils.isEmpty(serverOrGroupId)) {
			serverOrGroupId = restfullyGetServerId(serverOrGroup);
		}

		if (StringUtils.isEmpty(serverOrGroupId)) {
			throw new IllegalArgumentException("No group or server named \"" + serverOrGroup + "\" found");
		}

		// delete existing deployment before creating new one
		restfullyDeleteDeployment(name);

		WebClient webClient = getWebClient("deployments");
		webClient.type(MediaType.APPLICATION_JSON_TYPE);

		try {
			StringWriter stringWriter = new StringWriter();
			JsonFactory jfactory = new JsonFactory();
			JsonGenerator jGenerator = jfactory.createJsonGenerator(stringWriter);
			jGenerator.writeStartObject(); // {
			jGenerator.writeStringField("name", name); // "name" : name
			jGenerator.writeFieldName("servers"); // "servers" :
			jGenerator.writeStartArray(); // [
			jGenerator.writeString(serverOrGroupId); // "serverId"
			jGenerator.writeEndArray(); // ]
			jGenerator.writeFieldName("applications"); // "applications" :
			jGenerator.writeStartArray(); // [
			jGenerator.writeString(versionId); // "application version Id"
			jGenerator.writeEndArray(); // ]
			jGenerator.writeEndObject(); // }
			jGenerator.close();

			Response response = webClient.post(stringWriter.toString());
			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);

			String deploymentId = jsonNode.path("id").getTextValue();

			logger.info("Deployment successfully created with id \"" + deploymentId + "\"");

			return deploymentId;
		} finally {
			webClient.close();
		}
	}

	public void restfullyDeleteDeployment(String name) throws IOException {
		String deploymentId = restfullyGetDeploymentIdByName(name);
		if (deploymentId != null) {
			restfullyDeleteDeploymentById(deploymentId);
		}
	}

	public void restfullyDeleteDeploymentById(String deploymentId) throws IOException {
		WebClient webClient = getWebClient("deployments", deploymentId);

		try {
			Response response = webClient.delete();
			processResponse(response);
		} finally {
			webClient.close();
		}
	}

	
	
	/**
	 * Deploy the deployment from the deployment id
	 * @param deploymentId Id of the deployment
	 * @throws IOException
	 */
	public void restfullyDeployDeploymentById(String deploymentId) throws IOException {
		WebClient webClient = getWebClient("deployments", deploymentId, "deploy");

		try {
			Response response = webClient.post(null);
			processResponse(response);
			String responseText = IOUtils.toString((InputStream) response.getEntity());
			logger.info("Application deployed with answer \"" + responseText + "\"");
		} finally {
			webClient.close();
		}
	}

	/**
	 * Returns the deployment id from the deployment name
	 * @param deploymentName
	 * @return
	 * @throws IOException
	 */
	public String restfullyGetDeploymentIdByName(String deploymentName) throws IOException {
		WebClient webClient = getWebClient("deployments");

		String deploymentId = null;
		try {
			Response response = webClient.get();

			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);
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
		return deploymentId;
	}

	public String restfullyGetApplicationId(String name, String version) throws IOException {
		WebClient webClient = getWebClient("repository");

		String applicationId = null;
		try {
			Response response = webClient.get();

			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);
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
		String serverGroupId = null;
		WebClient webClient = getWebClient("serverGroups");
		try {
			Response response = webClient.get();
			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);
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
		Set<String> serversId = new TreeSet<String>();
		WebClient webClient = getWebClient("servers");

		try {
			Response response = webClient.get();

			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);
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
		String serverId = null;
		WebClient webClient = getWebClient("servers");

		try {
			Response response = webClient.get();
			InputStream responseStream = (InputStream) response.getEntity();
			JsonNode jsonNode = OBJECT_MAPPER.readTree(responseStream);
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
		}
	}

	/**
	 * Updloads application to the repository and returns the version id
	 * 
	 * @param name
	 *            Name of the application on the repository
	 * @param version
	 *            version of the application on the repository
	 * @param packageFile
	 *            The application file
	 * @return The id of the uploaded application on the repository
	 * @throws IOException
	 */

	public String restfullyUploadRepository(String name, String version, File packageFile) throws IOException {
		WebClient webClient = getWebClient("repository");
		webClient.type("multipart/form-data");

		try {
			// delete application first
			if (isSnapshotVersion(version)) {
				restfullyDeleteApplication(name, version);
			}
			Attachment nameAttachment = new AttachmentBuilder().id("name").object(name).contentDisposition(new ContentDisposition("form-data; name=\"name\"")).build();
			Attachment versionAttachment = new AttachmentBuilder().id("version").object(version).contentDisposition(new ContentDisposition("form-data; name=\"version\"")).build();
			Attachment fileAttachment = new Attachment("file", new FileInputStream(packageFile), new ContentDisposition("form-data; name=\"file\"; filename=\"" + packageFile.getName() + "\""));

			MultipartBody multipartBody = new MultipartBody(Arrays.asList(fileAttachment, nameAttachment, versionAttachment), MediaType.MULTIPART_FORM_DATA_TYPE, true);

			Response response = webClient.post(multipartBody);

			String responseObject = processResponse(response);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode result = mapper.readTree(responseObject);
			return result.path("versionId").getTextValue();
		} finally {
			webClient.close();
		}
	}

	public void restfullyDeleteApplicationById(String applicationVersionId) throws IOException {
		WebClient webClient = getWebClient("repository", applicationVersionId);

		try {
			Response response = webClient.delete();
			processResponse(response);
		} finally {
			webClient.close();
		}

	}

	public void restfullyDeleteApplication(String applicationName, String version) throws IOException {
		String applicationVersionId = restfullyGetApplicationId(applicationName, version);
		if (applicationVersionId != null) {
			restfullyDeleteApplicationById(applicationVersionId);
		}
	}

	protected boolean isSnapshotVersion(String version) {
		return version.contains(SNAPSHOT);
	}
}