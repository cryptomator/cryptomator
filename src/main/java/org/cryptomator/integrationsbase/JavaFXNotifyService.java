package org.cryptomator.integrationsbase;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.notify.NotifyService;
import org.cryptomator.integrations.notify.NotifyService2;
import org.cryptomator.integrations.notify.NotifyServiceException;

import java.util.ArrayList;

@Priority(Priority.FALLBACK)
//@LocalizedDisplayName(bundle = "strings", key ="")
@DisplayName("Cryptomator App Window")
public class JavaFXNotifyService implements NotifyService2 {

	//TODO: ipcMessageFile!

	@Override
	public void sendNotification(String header, String description, NotifyService2.Action... actions) throws NotifyServiceException {
		var argList = new ArrayList<String>();
		argList.add(header);
		argList.add(description);
		for(var action: actions) {
			argList.add(action.label());
			argList.add(action.returnMessage());
		}
		NotificationApp.main(argList.toArray(new String[] {}));
	}

}
