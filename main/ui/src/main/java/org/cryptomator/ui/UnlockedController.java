/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import org.cryptomator.ui.model.Directory;

public class UnlockedController implements Initializable {

	private ResourceBundle rb;
	private LockListener listener;
	private Directory directory;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;

	}

	@FXML
	protected void closeVault(ActionEvent event) {
		directory.unmount();
		directory.stopServer();
		if (listener != null) {
			listener.didLock(this);
		}
	}

	/* Getter/Setter */

	public Directory getDirectory() {
		return directory;
	}

	public void setDirectory(Directory directory) {
		this.directory = directory;
		final String msg = String.format(rb.getString("unlocked.messageLabel.runningOnPort"), directory.getServer().getPort());
		messageLabel.setText(msg);
	}

	public LockListener getListener() {
		return listener;
	}

	public void setListener(LockListener listener) {
		this.listener = listener;
	}

	/* callback */

	interface LockListener {
		void didLock(UnlockedController ctrl);
	}

}
