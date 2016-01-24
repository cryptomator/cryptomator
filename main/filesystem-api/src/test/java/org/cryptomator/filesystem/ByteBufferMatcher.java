package org.cryptomator.filesystem;

import java.nio.ByteBuffer;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class ByteBufferMatcher {

	public static Matcher<ByteBuffer> byteBufferFilledWith(int value) {
		if (((byte) value) != value) {
			throw new IllegalArgumentException("Invalid byte value");
		}
		return new TypeSafeDiagnosingMatcher<ByteBuffer>(ByteBuffer.class) {

			@Override
			public void describeTo(Description description) {
				description.appendText("a byte buffer filled with " + value);
			}

			@Override
			protected boolean matchesSafely(ByteBuffer item, Description mismatchDescription) {
				while (item.hasRemaining()) {
					byte currentValue = item.get();
					if (currentValue != value) {
						mismatchDescription.appendText("a byte buffer containing also " + currentValue);
						return false;
					}
				}
				return true;
			}
		};
	}

}
