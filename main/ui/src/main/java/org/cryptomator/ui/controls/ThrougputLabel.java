package org.cryptomator.ui.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;

public class ThrougputLabel extends Label {

	private static final long kibsThreshold = 1l << 7; // 0.128 kiB/s
	private static final long mibsThreshold = 1l << 19; // 0.512 MiB/s

	private final StringProperty idleFormat = new SimpleStringProperty("-");
	private final StringProperty kibsFormat = new SimpleStringProperty("%.3f");
	private final StringProperty mibsFormat = new SimpleStringProperty("%.3f");
	private final LongProperty bytesPerSecond = new SimpleLongProperty();

	public ThrougputLabel() {
		textProperty().bind(createStringBinding());
	}


	protected StringBinding createStringBinding() {
		return Bindings.createStringBinding(this::updateText, kibsFormat, mibsFormat, bytesPerSecond);
	}

	private String updateText() {
		long bps = bytesPerSecond.get();
		if (bps > mibsThreshold) {
			double mibs = ((double) bytesPerSecond.get()) / 1024.0 / 1024.0;
			return String.format(mibsFormat.get(), mibs);
		} else if (bps > kibsThreshold) {
			double kibs = ((double) bytesPerSecond.get()) / 1024.0;
			return String.format(kibsFormat.get(), kibs);
		} else {
			return String.format(idleFormat.get(), bps);
		}
	}

	/* Observables */

	public StringProperty idleFormatProperty() {
		return idleFormat;
	}

	public String getIdleFormat() {
		return idleFormat.get();
	}

	public void setIdleFormat(String idleFormat) {
		this.idleFormat.set(idleFormat);
	}

	public StringProperty kibsFormatProperty() {
		return kibsFormat;
	}

	public String getKibsFormat() {
		return kibsFormat.get();
	}

	public void setKibsFormat(String kibsFormat) {
		this.kibsFormat.set(kibsFormat);
	}

	public StringProperty mibsFormatProperty() {
		return mibsFormat;
	}

	public String getMibsFormat() {
		return mibsFormat.get();
	}

	public void setMibsFormat(String mibsFormat) {
		this.mibsFormat.set(mibsFormat);
	}

	public LongProperty bytesPerSecondProperty() {
		return bytesPerSecond;
	}

	public long getBytesPerSecond() {
		return bytesPerSecond.get();
	}

	public void setBytesPerSecond(long bytesPerSecond) {
		this.bytesPerSecond.set(bytesPerSecond);
	}
}
