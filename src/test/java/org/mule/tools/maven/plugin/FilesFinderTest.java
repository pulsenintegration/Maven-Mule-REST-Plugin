package org.mule.tools.maven.plugin;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FilesFinderTest {

	private File _tempDir;
	private File _testingFileInCurrentDir;

	@Before
	public void setup() throws Exception {
		_tempDir = File.createTempFile("TmpDir", "");
		if (_tempDir.exists()) {
			_tempDir.delete();
		}

		_testingFileInCurrentDir = new File("./MyUnitTestFileToDelete.txt");
		_testingFileInCurrentDir.createNewFile();

	}

	@After
	public void cleanup() throws Exception {
		_tempDir.delete();
		_testingFileInCurrentDir.delete();
	}

	@Test
	public void FindFileIfNoPattern() throws Exception {
		_tempDir.mkdir();

		new File(_tempDir, "MyFile1.txt").createNewFile();
		File expectedFile = new File(_tempDir, "MyFile2.txt");
		expectedFile.createNewFile();

		String pattern = _tempDir.getAbsolutePath() + File.separatorChar + "MyFile2.txt";

		Assert.assertEquals(expectedFile, FileFinder.find(pattern));
	}

	@Test
	public void FindTheOnlyFileFromTheGivenPattern() throws Exception {
		_tempDir.mkdir();

		File expectedFile = new File(_tempDir, "MyFile1.txt");
		expectedFile.createNewFile();
		new File(_tempDir, "AnotherFile.txt").createNewFile();

		String pattern = _tempDir.getAbsolutePath() + File.separatorChar + "MyFile*.txt";

		Assert.assertEquals(expectedFile, FileFinder.find(pattern));
	}

	@Test(expected = Exception.class)
	public void ThrowsIfMoreThanOneFileMatchingPattern() throws Exception {
		_tempDir.mkdir();

		new File(_tempDir, "MyFile1.txt").createNewFile();
		new File(_tempDir, "MyFile2.txt").createNewFile();

		String pattern = _tempDir.getAbsolutePath() + File.separatorChar + "MyFile*.txt";

		FileFinder.find(pattern);
	}

	@Test(expected = FileNotFoundException.class)
	public void ThrowsIfNoFileMatchPattern() throws Exception {
		_tempDir.mkdir();

		new File(_tempDir, "MyFile1.txt").createNewFile();
		new File(_tempDir, "MyFile2.txt").createNewFile();

		String pattern = _tempDir.getAbsolutePath() + File.separatorChar + "MySpecialFile*.txt";

		FileFinder.find(pattern);
	}

	@Test(expected = FileNotFoundException.class)
	public void ThrowsIfNoFileFound() throws Exception {
		_tempDir.mkdir();

		new File(_tempDir, "MyFile1.txt").createNewFile();
		new File(_tempDir, "MyFile2.txt").createNewFile();

		String pattern = _tempDir.getAbsolutePath() + File.separatorChar + "MySpecialFile.txt";

		FileFinder.find(pattern);
	}

	@Test
	public void FindSearchCurrentDirectoryIfNoPathSpecified() throws Exception {
		Assert.assertEquals(_testingFileInCurrentDir, FileFinder.find("MyUnitTestFile*.txt"));
	}
}
