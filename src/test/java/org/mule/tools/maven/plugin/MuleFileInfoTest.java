package org.mule.tools.maven.plugin;

import org.junit.Assert;
import org.junit.Test;

public class MuleFileInfoTest {

	@Test()
	public void testAppNameAndVersionWindowsPath() {
		
		MuleFileInfo mfi = MuleFileInfo.parseFromFile("c:\\anydir\\Mule-App-1.0.0-SNAPSHOT.zip");
		Assert.assertEquals("Mule-App", mfi.appName);
		Assert.assertEquals("1.0.0-SNAPSHOT", mfi.appVersion);
	}
	
	@Test()
	public void testAppNameAndVersionLinuxPath() {
		
		MuleFileInfo mfi = MuleFileInfo.parseFromFile("c:/anydir/Mule-App-1.0.0-SNAPSHOT.zip");
		Assert.assertEquals("Mule-App", mfi.appName);
		Assert.assertEquals("1.0.0-SNAPSHOT", mfi.appVersion);
	}
	
	@Test()
	public void testAppNameAndVersionNoPath() {
		
		MuleFileInfo mfi = MuleFileInfo.parseFromFile("Mule-App-1.0.0-SNAPSHOT.zip");
		Assert.assertEquals("Mule-App", mfi.appName);
		Assert.assertEquals("1.0.0-SNAPSHOT", mfi.appVersion);
	}
	
	@Test()
	public void testAppNameAndVersionNoVersion() {
		MuleFileInfo mfi1 = MuleFileInfo.parseFromFile("Mule-App.zip");
		Assert.assertEquals("Mule-App", mfi1.appName);
		Assert.assertEquals(null, mfi1.appVersion);

		
		MuleFileInfo mfi2 = MuleFileInfo.parseFromFile("Mule-App-.zip");
		Assert.assertEquals("Mule-App-", mfi2.appName);
		Assert.assertEquals(null, mfi2.appVersion);
	}
	
	@Test()
	public void testAppNameAndVersionVersionOnly() {
		MuleFileInfo mfi1 = MuleFileInfo.parseFromFile("-5.zip");
		Assert.assertEquals("", mfi1.appName);
		Assert.assertEquals("5", mfi1.appVersion);
	}
}
