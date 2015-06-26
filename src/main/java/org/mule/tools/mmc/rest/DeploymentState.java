package org.mule.tools.mmc.rest;


public class DeploymentState {

	/**
	 * Boolean value indicating the reconciled status of the deployment
	 */
	public boolean reconciled;
	
	/**
	 * Deployment state (DEPLOYED, UNDEPLOYED, IN_PROGRESS, SUCCESSFUL, FAILED, and DELETING)
	 */
	public DeploymentStatus status;
	
	/**
	 * URL of the deployment 
	 */
	public String href;
	
	/**
	 * Name of the deployment
	 */
	public String name;
	
}