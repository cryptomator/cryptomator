package org.cryptomator.filesystem.invariants.matchers;

import static org.hamcrest.CoreMatchers.is;

import org.cryptomator.common.test.matcher.PropertyMatcher;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.hamcrest.Matcher;

public class NodeMatchers {

	public static Matcher<Folder> folderWithName(String name) {
		return new PropertyMatcher<>(Folder.class, Folder::name, "name", is(name));
	}

	public static Matcher<File> fileWithName(String name) {
		return new PropertyMatcher<>(File.class, File::name, "name", is(name));
	}

}
