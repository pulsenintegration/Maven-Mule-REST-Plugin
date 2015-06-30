package org.mule.tools.maven.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuleFileInfo {
	
	/**
	 * Version of Mule application if found (or null)
	 */
	public String appVersion;
	
	/**
	 * Name of the Mule application without extension and version if found
	 */
	public String appName;
	
	public static MuleFileInfo parseFromFile(String customMuleAppFilePath) {
		String fileName = getFileName(customMuleAppFilePath);
		String fileNameWithoutExt = MuleFileInfo.getFileNameWithoutExt(fileName);
		
		Pattern versionRegex = Pattern.compile("-\\s*\\d+"); //Something in the form of [ANY_CHAR-AT_LEAST_ONE_NUMBER]
		Matcher matcher = versionRegex.matcher(fileNameWithoutExt);
		MuleFileInfo  muleFileInfo = new MuleFileInfo();
		
		if (matcher.find()){
			int matchPos = matcher.start();
			muleFileInfo.appName = fileNameWithoutExt.substring(0,matchPos).trim();
			muleFileInfo.appVersion = fileNameWithoutExt.substring(matchPos + 1).trim();
		} else {
			muleFileInfo.appName = fileNameWithoutExt;
		}
		
		
		return muleFileInfo;
	}
	
	private static String getFileName(String filePath){
		String standardFilePath = filePath.replace("\\", "/");
		int lastSlashPos = standardFilePath.lastIndexOf("/");
		if(lastSlashPos >= 0){
			String fileName = filePath.substring(lastSlashPos + 1);
			return fileName;
		} else  {
			return filePath;
		}
	}
	
	private static String getFileNameWithoutExt(String fileName){
		int pos = fileName.lastIndexOf(".");
		if (pos >= 0) {
			String fileNameWithoutExt = fileName.substring(0, pos);
			return fileNameWithoutExt;
		} else {
			return fileName;
		}
	}
	

}
