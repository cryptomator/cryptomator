/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem;

import static java.lang.String.format;

import java.util.function.Consumer;

public class FolderVisitor {

	private final Consumer<Folder> beforeFolderVisitor;
	private final Consumer<Folder> afterFolderVisitor;
	private final Consumer<File> fileVisitor;
	private final Consumer<Node> nodeVisitor;
	private final int maxDepth;

	public FolderVisitor(FolderVisitorBuilder builder) {
		this.beforeFolderVisitor = builder.beforeFolderVisitor;
		this.afterFolderVisitor = builder.afterFolderVisitor;
		this.fileVisitor = builder.fileVisitor;
		this.nodeVisitor = builder.nodeVisitor;
		this.maxDepth = builder.maxDepth;
	}

	public static FolderVisitorBuilder folderVisitor() {
		return new FolderVisitorBuilder();
	}

	public FolderVisitor visit(Folder folder) {
		return visit(folder, 0);
	}

	public FolderVisitor visit(File file) {
		return visit(file, 0);
	}

	private FolderVisitor visit(Folder folder, int depth) {
		beforeFolderVisitor.accept(folder);
		nodeVisitor.accept(folder);
		final int childDepth = depth + 1;
		if (childDepth <= maxDepth) {
			folder.folders().forEach(childFolder -> visit(childFolder, childDepth));
			folder.files().forEach(childFile -> visit(childFile, childDepth));
		}
		afterFolderVisitor.accept(folder);
		return this;
	}

	private FolderVisitor visit(File file, int depth) {
		nodeVisitor.accept(file);
		fileVisitor.accept(file);
		return this;
	}

	public static class FolderVisitorBuilder {

		private Consumer<Folder> beforeFolderVisitor = noOp();
		private Consumer<Folder> afterFolderVisitor = noOp();
		private Consumer<File> fileVisitor = noOp();
		private Consumer<Node> nodeVisitor = noOp();
		private int maxDepth = Integer.MAX_VALUE;

		private FolderVisitorBuilder() {
		}

		public FolderVisitorBuilder beforeFolder(Consumer<Folder> beforeFolderVisitor) {
			if (beforeFolderVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.beforeFolderVisitor = beforeFolderVisitor;
			return this;
		}

		public FolderVisitorBuilder afterFolder(Consumer<Folder> afterFolderVisitor) {
			if (afterFolderVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.afterFolderVisitor = afterFolderVisitor;
			return this;
		}

		public FolderVisitorBuilder forEachFile(Consumer<File> fileVisitor) {
			if (fileVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.fileVisitor = fileVisitor;
			return this;
		}

		public FolderVisitorBuilder forEachNode(Consumer<Node> nodeVisitor) {
			if (nodeVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.nodeVisitor = nodeVisitor;
			return this;
		}

		public FolderVisitorBuilder withMaxDepth(int maxDepth) {
			if (maxDepth < 0) {
				throw new IllegalArgumentException(format("maxDepth must not be smaller 0 but was %d", maxDepth));
			}
			this.maxDepth = maxDepth;
			return this;
		}

		public FolderVisitor visit(Folder folder) {
			return build().visit(folder);
		}

		public FolderVisitor visit(File file) {
			return build().visit(file);
		}

		public FolderVisitor build() {
			return new FolderVisitor(this);
		}

		private static <T> Consumer<T> noOp() {
			return ignoredParameter -> {
			};
		}

	}

}
