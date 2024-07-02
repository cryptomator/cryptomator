package org.cryptomator.common.settings;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NetworkSettings {

	public final ObjectProperty<ProxyMode> mode;
	public final StringProperty httpProxy;
	public final IntegerProperty httpPort;
	public final BooleanProperty samePortProxyForHttpHttps;
	public final StringProperty httpsProxy;
	public final IntegerProperty httpsPort;

	NetworkSettings(NetworkSettingsJson json){
		this.mode = new SimpleObjectProperty<>(this, "mode", json.mode);
		this.httpProxy = new SimpleStringProperty(this, "httpProxy", json.httpProxy);
		this.httpPort = new SimpleIntegerProperty(this, "httpPort", json.httpPort);
		this.samePortProxyForHttpHttps = new SimpleBooleanProperty(this, "samePortProxyForHttpHttps", json.samePortProxyForHttpHttps);
		this.httpsProxy = new SimpleStringProperty(this, "httpsProxy", json.httpsProxy);
		this.httpsPort = new SimpleIntegerProperty(this, "httpsPort", json.httpsPort);
	}

	NetworkSettingsJson serialized(){
		var json = new NetworkSettingsJson();
		json.mode = mode.get();
		json.httpProxy = httpProxy.get();
		json.httpPort = httpPort.get();
		json.samePortProxyForHttpHttps = samePortProxyForHttpHttps.get();
		json.httpsProxy = httpsProxy.get();
		json.httpsPort = httpsPort.get();
		return json;
	}

	Observable[] observables() {
		return new Observable[]{mode, httpProxy, httpPort,httpsProxy,httpsPort};
	}

	public enum ProxyMode {
		NO,
		SYSTEM,
		MANUAL
	}
}
