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
					<version>1.4.2-SNAPSHOT</version>
				</plugin>
			</plugins>
		</build>
		...
	</project>

# Calling the plugin #

There is only one goal, deploy. To call the plugin, do the following

- With a pom.xml containing all required parameters:

		mvn mule-mmc-rest-plugin:deploy
			
- With a pom.xml missing MMC parameters:

		mvn com.github.nicholasastuart:mule-mmc-rest-plugin:[ARTIFACT_VERSION]:deploy -DmmcUsername=[USERNAME] -DmmcPassword=[PASSWORD] -DmmcApiUrl=[MMC_URL] -DtargetDeploymentServer=[SERVER_OR_GROUP]

- Without pom.xml

		mvn com.github.nicholasastuart:mule-mmc-rest-plugin:[ARTIFACT_VERSION]:deploy -DmmcUsername=[USERNAME] -DmmcPassword=[PASSWORD] -DmmcApiUrl=[MMC_URL] -DtargetDeploymentServer=[SERVER_OR_GROUP] -DcustomMuleAppFilePath=[PATH_TO_MULE_PACKAGE] 
	
	
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
    <th>Mandatory
    
	<tr>
		<td>mmcApiUrl
		<td>The URL of the Mule Management Console API (usually http://[IP_OR_HOST_NAME:8080]/mmc/api).
		<td>Empty
		<td>Yes
	<tr>
		<td>mmcUsername
		<td>The username to the Mule MMC API.
		<td>Empty
		<td>Yes
	<tr>
		<td>mmcPassword
		<td>The password to the Mule MMC API.
		<td>Empty
		<td>Yes
	<tr>
		<td>targetDeploymentServer
		<td>The name of the server or server group where to deploy the application.
		<td>Empty
		<td>Yes
	<tr>
		<td>artifactId
		<td>Id of the artifact from the pom.xml. Used as default app name and/or deployment name if not overridden by custom options.
		<td>Depends on the pom.xml
		<td>No
	<tr>
		<td>version
		<td>Version from the pom.xml. Used as Mule app version on the repository if not overridden by custom options.	
		<td>Depends on the pom.xml
		<td>No
	<tr>
		<td>useTimestampVersion
		<td>If set to true, the version of the app in the repository is a timestamp in "MM-dd-yyyy-HH:mm:ss" format.
		<td>false
		<td>No
	<tr>
		<td>noPomMode
		<td>If true, allows the plugin to be used without pom.xml. 
		<td>false
		<td>No
	<tr>
		<td>customMuleAppFilePath
		<td>Mandatory if noPomMode is true, allows the plugin to use a custom Mule application package. The name and the version will be taken from the file name if not overridden by custom options.	
		<td>Empty
		<td>Yes if noPomMode is True
	<tr>
		<td>customRepositoryAppName
		<td>Overrides the name of the Mule app on the repository.
		<td>Empty
		<td>No
	<tr>
		<td>customRepositoryAppVersion
		<td>Overrides the version of the Mule app on the repository.
		<td>Empty
		<td>No
	<tr>
		<td>customDeploymentName
		<td>Overrides the name of the deployment.
		<td>Empty
		<td>No
	<tr>
		<td>deploymentTimeoutMs
		<td>Specifies the time to wait for the uploaded application to reach the deployed state before a timeout error occurs.
		<td>30000
		<td>No
</table> 
