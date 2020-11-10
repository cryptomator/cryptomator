package org.cryptomator.ui.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;

public class DataLabel extends Label {

	private static final long KIB_THRESHOLD = 1l << 7; // 0.128 kiB
	private static final long MIB_THRESHOLD = 1l << 19; // 0.512 MiB
	private static final long GIB_THRESHOLD = 1l << 29; // 0.512 GiB

	private final StringProperty byteFormat = new SimpleStringProperty("-");
	private final StringProperty kibFormat = new SimpleStringProperty("%.3f");
	private final StringProperty mibFormat = new SimpleStringProperty("%.3f");
	private final StringProperty gibFormat = new SimpleStringProperty("%.3f");
	private final LongProperty dataInBytes = new SimpleLongProperty();

	public DataLabel() {
		textProperty().bind(createStringBinding());
	}

	protected StringBinding createStringBinding() {
		return Bindings.createStringBinding(this::updateText, kibFormat, mibFormat, gibFormat, dataInBytes);
	}

	private String updateText() {
		long data = dataInBytes.get();
		if (data > GIB_THRESHOLD) {
			double giB = ((double) data) / 1024.0 / 1024.0 / 1024.0;
			return String.format(gibFormat.get(), giB);
		} else if (data > MIB_THRESHOLD) {
			double miB = ((double) data) / 1024.0 / 1024.0;
			return String.format(mibFormat.get(), miB);
		} else if (data > KIB_THRESHOLD) {
			double kiB = ((double) data) / 1024.0;
			return String.format(kibFormat.get(), kiB);
		} else {
			return String.format(byteFormat.get(), data);
		}
	}

	public StringProperty byteFormatProperty() { return byteFormat; }

	public String getByteFormat() { return byteFormat.get(); }

	public void setByteFormat(String byteFormat) {
		this.byteFormat.set(byteFormat);
	}

	public StringProperty kibFormatProperty() { return kibFormat; }

	public String getKibFormat() { return kibFormat.get(); }

	public void setKibFormat(String kibFormat) {
		this.kibFormat.set(kibFormat);
	}

	public StringProperty mibFormatProperty() { return mibFormat; }

	public String getMibFormat() { return mibFormat.get(); }

	public void setMibFormat(String mibFormat) {
		this.mibFormat.set(mibFormat);
	}

	public StringProperty gibFormatProperty() { return gibFormat; }

	public String getGibFormat() { return gibFormat.get(); }

	public void setGibFormat(String gibFormat) {
		this.gibFormat.set(gibFormat);
	}

	public LongProperty dataInBytesProperty() { return dataInBytes; }

	public long getDataInBytes() {
		return dataInBytes.get();
	}

	public void setDataInBytes(long dataInBytes) { this.dataInBytes.set(dataInBytes); }


}
