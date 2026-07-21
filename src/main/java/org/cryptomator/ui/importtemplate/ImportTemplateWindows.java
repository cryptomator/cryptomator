package org.cryptomator.ui.importtemplate;

import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.cryptomator.ui.fxapp.PrimaryStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

/**
 * Entry point for showing the vault template import flow.
 * <p>
 * Everything the flow needs to decide - unpacking the archive, and choosing between the import dialog and the
 * "malformed template" dialog.
 */
@FxApplicationScoped
public class ImportTemplateWindows {

	private static final Logger LOG = LoggerFactory.getLogger(ImportTemplateWindows.class);

	private final ImportTemplateComponent.Factory importTemplateWindow;
	private final Dialogs dialogs;
	private final Stage primaryStage;
	private final ExecutorService executor;

	@Inject
	ImportTemplateWindows(ImportTemplateComponent.Factory importTemplateWindow, //
						  Dialogs dialogs, //
						  @PrimaryStage Stage primaryStage, //
						  ExecutorService executor) {
		this.importTemplateWindow = importTemplateWindow;
		this.dialogs = dialogs;
		this.primaryStage = primaryStage;
		this.executor = executor;
	}

	/**
	 * Unpacks the given archive off the FX thread and shows the import dialog for it.
	 * <p>
	 * A template that could never be imported is rejected before the user is asked to choose a storage location: in that
	 * case {@link #showMalformedTemplateDialog()} is shown instead and the returned stage is the primary stage. Failures
	 * that are <em>not</em> the template's fault (the file system refusing to unpack it) complete the stage
	 * exceptionally, leaving the generic error display to the caller.
	 *
	 * @param name    the fixed vault name
	 * @param archive the ZIP archive bytes
	 * @return the import window, or the primary stage if the template was rejected
	 */
	public CompletionStage<Stage> extractAndShowImportTemplateWindow(String name, byte[] archive) {
		return extractAsync(archive)
			.handleAsync((template, throwable) -> {
				if (throwable != null) {
					var cause = throwable.getCause() != null ? throwable.getCause() : throwable;
					if (cause instanceof MalformedTemplateException) {
						// template itself is invalid
						LOG.error("Vault template is malformed.", cause);
						return showMalformedTemplateDialog();
					} else {
						LOG.error("Failed to unpack vault template.", cause);
						throw new CompletionException(cause);
					}
				}
				try {
					var component = importTemplateWindow.create(name, template);
					component.showImportTemplateWindow();
					return component.window();
				} catch (RuntimeException e) {
					template.close(); //nothing took ownership, so the temporary directory is ours to discard
					throw e;
				}
			}, Platform::runLater);
	}

	/**
	 * Async Wrapper for {@link VaultTemplate#extract(byte[])}
	 * <p>
	 * Failures arrive as a {@link CompletionException} wrapping the cause: a {@link MalformedTemplateException}
	 * means the template itself is a dead end, any other {@link IOException} means unpacking failed for
	 * an environmental reason.
	 *
	 * @param archive the ZIP archive bytes
	 * @return the unpacked template, which the caller must close
	 * @see VaultTemplate#extract(byte[])
	 */
	CompletionStage<VaultTemplate> extractAsync(byte[] archive) {
		return CompletableFuture.supplyAsync(()-> {
			try {
				return VaultTemplate.extract(archive);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Tells the user that the template cannot be imported. Must be called on the FX thread.
	 *
	 * @return the primary stage, which is what remains visible once the user dismisses the dialog
	 */
	public Stage showMalformedTemplateDialog() {
		dialogs.prepareMalformedTemplateDialog(primaryStage).build().showAndWait();
		return primaryStage;
	}

}
