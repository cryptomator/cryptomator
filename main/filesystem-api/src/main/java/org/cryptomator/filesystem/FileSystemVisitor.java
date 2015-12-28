package org.cryptomator.filesystem;

import java.util.function.Consumer;

public class FileSystemVisitor {

	private final Consumer<Folder> beforeFolderVisitor;
	private final Consumer<Folder> afterFolderVisitor;
	private final Consumer<File> fileVisitor;

	private FileSystemVisitor(Consumer<Folder> beforeFolderVisitor, Consumer<Folder> afterFolderVisitor, Consumer<File> fileVisitor) {
		this.beforeFolderVisitor = beforeFolderVisitor;
		this.afterFolderVisitor = afterFolderVisitor;
		this.fileVisitor = fileVisitor;
	}

	public static FileSystemVisitorBuilder fileSystemVisitor() {
		return new FileSystemVisitorBuilder();
	}

	public FileSystemVisitor visit(Folder folder) {
		beforeFolderVisitor.accept(folder);
		folder.folders().forEach(this::visit);
		folder.files().forEach(this::visit);
		afterFolderVisitor.accept(folder);
		return this;
	}

	public FileSystemVisitor visit(File file) {
		fileVisitor.accept(file);
		return this;
	}

	public static class FileSystemVisitorBuilder {

		private Consumer<Folder> beforeFolderVisitor = noOp();
		private Consumer<Folder> afterFolderVisitor = noOp();
		private Consumer<File> fileVisitor = noOp();

		private FileSystemVisitorBuilder() {
		}

		public FileSystemVisitorBuilder beforeFolder(Consumer<Folder> beforeFolderVisitor) {
			if (beforeFolderVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.beforeFolderVisitor = beforeFolderVisitor;
			return this;
		}

		public FileSystemVisitorBuilder afterFolder(Consumer<Folder> afterFolderVisitor) {
			if (afterFolderVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.afterFolderVisitor = afterFolderVisitor;
			return this;
		}

		public FileSystemVisitorBuilder forEachFile(Consumer<File> fileVisitor) {
			if (fileVisitor == null) {
				throw new IllegalArgumentException("Vistior may not be null");
			}
			this.fileVisitor = fileVisitor;
			return this;
		}

		public FileSystemVisitor visit(Folder folder) {
			return build().visit(folder);
		}

		public FileSystemVisitor visit(File file) {
			return build().visit(file);
		}

		public FileSystemVisitor build() {
			return new FileSystemVisitor(beforeFolderVisitor, afterFolderVisitor, fileVisitor);
		}

		private static <T> Consumer<T> noOp() {
			return ignoredParameter -> {
			};
		}

	}

}
