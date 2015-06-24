#Maven Mule REST Plugin#

This is a project to utilize the RESTful interface that is provided for the Management console on Mule ESB's Enterprise Edition. 

This plugin assumes that you have configured the [maven-mule-plugin](https://github.com/mulesoft/maven-mule-plugin) to generated the mule application archive

This is a personal project and is not affiliated with MuleSoft or the maven mule plugin in any way.

Example:

	<project>
		...
		<build>
			<plugins>
				<plugin>
					<groupId>org.mule.tools</groupId>
					<artifactId>mule-mmc-rest-plugin</artifactId>
					<version>1.2.6-SNAPSHOT</version>
				</plugin>
			</plugins>
		</build>
		...
	</project>

# Calling the plugin #

There is only one goal, deploy. To call the plugin, do the following

	mvn mule-mmc-rest-plugin:deploy
	
	or 
	
	mvn com.github.nicholasastuart:mule-mmc-rest-plugin:[ARTIFACT_VERSION]:deploy -Dopt1=val1 -Dopt2=val2 ...
	
This goal will
*   delete an existing mule application archive from the MMC Repository if version contains "SNAPSHOT"
*	upload the mule application archive to the MMC Repository
*	delete an existing deployment having the same application name
*	upload application to the MMC repository
*	perform a deploy request to make MMC deploy into target server or server group

## Security ##
In order to post to the Mule Repository, you need only these permissions:

*	Repository Read 
*	Repository Modify

## Configuration Options ##
<table>
	<tr>
		<th>Property
		<th>Description
		<th>Default
<tr>
	<td>
		mmcApiUrl
	<td>
		The URL of the Mule Management Console API (usually .../api)
	<td>
		
<tr>
	<td>
		repositoryAppName
	<td>
		Name of the application when it is uploaded to the repository
	<td>
		Artifact name
<tr>
	<td>
		deploymentName
	<td>
		Name of the deployment when it is deployed on the server or group
	<td>
		Artifact name
<tr>
	<td>
		version
	<td>
		The version given to the uploaded application on the repository
	<td>
		Project version (from the pom.xml) unless <code>useTimestampVersion</code> is set to true
<tr>
	<td>
		useTimestampVersion
	<td>
		If set to true, the version is a timestamp in "MM-dd-yyyy-HH:mm:ss" format
	<td>
		false
<tr>
	<td>
		serverOrGroup
	<td>
		The name of the server or server group where to deploy the application
	<td>
<tr>
	<td>
		mmcUsername
	<td>
		The username to the Mule MMC API.
	<td>
<tr>
	<td>
		mmcPassword
	<td>
		The password to the Mule MMC API.
	<td>
</table> 
