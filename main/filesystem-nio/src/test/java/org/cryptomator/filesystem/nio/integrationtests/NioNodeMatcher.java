package org.cryptomator.filesystem.nio.integrationtests;

import static org.hamcrest.CoreMatchers.equalTo;

import org.cryptomator.common.test.matcher.PropertyMatcher;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.hamcrest.Matcher;

class NioNodeMatcher {

	public static Matcher<Folder> folderWithName(String name) {
		return new PropertyMatcher<>(Folder.class, Folder::name, "name", equalTo(name));
	}

	public static Matcher<File> fileWithName(String name) {
		return new PropertyMatcher<>(File.class, File::name, "name", equalTo(name));
	}

}
