package org.mule.tools.maven.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import org.apache.commons.lang.NullArgumentException;
import org.apache.cxf.common.util.StringUtils;

public class FileFinder {

	public static File find(String filePathPattern) throws Exception {
		if (StringUtils.isEmpty(filePathPattern)) {
			throw new NullArgumentException("Can't find files from null or empty pattern");
		}

		File filePath = new File(filePathPattern);
		if (filePath.exists()) {
			return filePath;
		}

		String standarizedFilePathPattern = filePathPattern.replace("\\", "/");

		int lastSlashPos = standarizedFilePathPattern.lastIndexOf("/");

		String rootSearchDirPath;
		String fileNameSearchPattern;
		if (lastSlashPos >= 0) {
			rootSearchDirPath = filePathPattern.substring(0, lastSlashPos);
			fileNameSearchPattern = filePathPattern.substring(lastSlashPos + 1);
		} else {
			rootSearchDirPath = "./";
			fileNameSearchPattern = filePathPattern;
		}

		final String fileNameSearchRegex = fileNameSearchPattern.replace(".", "\\.").replace("*", ".*");

		File rootSearchDir = new File(rootSearchDirPath);
		File[] matchingFiles = rootSearchDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(fileNameSearchRegex);
			}
		});

		if (matchingFiles == null || matchingFiles.length <= 0) {
			throw new FileNotFoundException("No file matching pattern \"" + fileNameSearchPattern + "\" found in directory \"" + rootSearchDir.getAbsolutePath() + "\"");
		} else if (matchingFiles.length > 1) {
			throw new Exception("More than one file found matching pattern \"" + fileNameSearchPattern + "\" in directory \"" + rootSearchDir.getAbsolutePath() + "\"");
		} else {
			return matchingFiles[0];
		}
	}

}
