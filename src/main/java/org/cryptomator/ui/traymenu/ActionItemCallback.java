package org.cryptomator.ui.traymenu;

import org.cryptomator.integrations.tray.ActionItem;
import org.purejava.linux.GCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionItemCallback implements GCallback {
	private static final Logger LOG = LoggerFactory.getLogger(ActionItemCallback.class);
	private ActionItem actionItem;

	public ActionItemCallback(ActionItem actionItem) {
		this.actionItem = actionItem;
	}

	@Override
	public void apply() {
		LOG.info("Hit Action {}", actionItem.action().toString());
		actionItem.action().run();
	}
}
