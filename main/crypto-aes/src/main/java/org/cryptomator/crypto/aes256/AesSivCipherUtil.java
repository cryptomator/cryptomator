/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.cryptomator.crypto.exceptions.DecryptFailedException;

/**
 * Implements the RFC 5297 SIV mode.
 */
final class AesSivCipherUtil {

	private static final byte[] BYTES_ZERO = new byte[16];
	private static final byte DOUBLING_CONST = (byte) 0x87;

	static byte[] sivEncrypt(byte[] key, byte[] plaintext, byte[]... additionalData) throws InvalidKeyException {
		if (key.length != 32 && key.length != 48 && key.length != 64) {
			throw new InvalidKeyException("Invalid key length " + key.length);
		}

		final byte[] k1 = Arrays.copyOf(key, key.length / 2);
		final byte[] k2 = Arrays.copyOfRange(key, k1.length, key.length);

		final byte[] iv = s2v(k1, plaintext, additionalData);

		final int numBlocks = (plaintext.length + 15) / 16;

		// clear out the 31st and 63rd (rightmost) bit:
		final byte[] ctr = Arrays.copyOf(iv, 16);
		ctr[8] = (byte) (ctr[8] & 0x7F);
		ctr[12] = (byte) (ctr[12] & 0x7F);
		final ByteBuffer ctrBuf = ByteBuffer.wrap(ctr);
		final long initialCtrVal = ctrBuf.getLong(8);

		final byte[] x = new byte[numBlocks * 16];
		final BlockCipher aes = new AESFastEngine();
		aes.init(true, new KeyParameter(k2));
		for (int i = 0; i < numBlocks; i++) {
			final long ctrVal = initialCtrVal + i;
			ctrBuf.putLong(8, ctrVal);
			aes.processBlock(ctrBuf.array(), 0, x, i * 16);
			aes.reset();
		}

		final byte[] ciphertext = xorbegin(plaintext, x);

		return ArrayUtils.addAll(iv, ciphertext);
	}

	static byte[] sivDecrypt(byte[] key, byte[] ciphertext, byte[]... additionalData) throws DecryptFailedException, InvalidKeyException {
		if (key.length != 32 && key.length != 48 && key.length != 64) {
			throw new InvalidKeyException("Invalid key length " + key.length);
		}

		final byte[] k1 = Arrays.copyOf(key, key.length / 2);
		final byte[] k2 = Arrays.copyOfRange(key, k1.length, key.length);

		final byte[] iv = Arrays.copyOf(ciphertext, 16);

		final byte[] actualCiphertext = Arrays.copyOfRange(ciphertext, 16, ciphertext.length);
		final int numBlocks = (actualCiphertext.length + 15) / 16;

		// clear out the 31st and 63rd (rightmost) bit:
		final byte[] ctr = Arrays.copyOf(iv, 16);
		ctr[8] = (byte) (ctr[8] & 0x7F);
		ctr[12] = (byte) (ctr[12] & 0x7F);
		final ByteBuffer ctrBuf = ByteBuffer.wrap(ctr);
		final long initialCtrVal = ctrBuf.getLong(8);

		final byte[] x = new byte[numBlocks * 16];
		final BlockCipher aes = new AESFastEngine();
		aes.init(true, new KeyParameter(k2));
		for (int i = 0; i < numBlocks; i++) {
			final long ctrVal = initialCtrVal + i;
			ctrBuf.putLong(8, ctrVal);
			aes.processBlock(ctrBuf.array(), 0, x, i * 16);
			aes.reset();
		}

		final byte[] plaintext = xorbegin(actualCiphertext, x);

		Hex.encodeHexString(actualCiphertext);

		final byte[] control = s2v(k1, plaintext, additionalData);

		if (MessageDigest.isEqual(control, iv)) {
			return plaintext;
		} else {
			throw new DecryptFailedException("Authentication failed");
		}
	}

	static byte[] s2v(byte[] key, byte[] plaintext, byte[]... additionalData) {
		final CipherParameters params = new KeyParameter(key);
		final BlockCipher aes = new AESFastEngine();
		final CMac mac = new CMac(aes);
		mac.init(params);

		byte[] d = mac(mac, BYTES_ZERO);

		for (byte[] s : additionalData) {
			d = xor(dbl(d), mac(mac, s));
		}

		final byte[] t;
		if (plaintext.length >= 16) {
			t = xorend(plaintext, d);
		} else {
			t = xor(dbl(d), pad(plaintext));
		}

		return mac(mac, t);
	}

	private static byte[] mac(Mac mac, byte[] in) {
		byte[] result = new byte[mac.getMacSize()];
		mac.update(in, 0, in.length);
		mac.doFinal(result, 0);
		return result;
	}

	/**
	 * First bit 1, following bits 0.
	 */
	private static byte[] pad(byte[] in) {
		final byte[] result = Arrays.copyOf(in, 16);
		new ISO7816d4Padding().addPadding(result, in.length);
		return result;
	}

	/**
	 * Code taken from {@link org.bouncycastle.crypto.macs.CMac}
	 */
	private static int shiftLeft(byte[] block, byte[] output) {
		int i = block.length;
		int bit = 0;
		while (--i >= 0) {
			int b = block[i] & 0xff;
			output[i] = (byte) ((b << 1) | bit);
			bit = (b >>> 7) & 1;
		}
		return bit;
	}

	/**
	 * Code taken from {@link org.bouncycastle.crypto.macs.CMac}
	 */
	private static byte[] dbl(byte[] in) {
		byte[] ret = new byte[in.length];
		int carry = shiftLeft(in, ret);
		int xor = 0xff & DOUBLING_CONST;

		/*
		 * NOTE: This construction is an attempt at a constant-time implementation.
		 */
		ret[in.length - 1] ^= (xor >>> ((1 - carry) << 3));

		return ret;
	}

	private static byte[] xor(byte[] in1, byte[] in2) {
		if (in1 == null || in2 == null || in1.length != in2.length) {
			throw new IllegalArgumentException("Inputs must equal in length.");
		}

		return xorbegin(in1, in2);
	}

	private static byte[] xorend(byte[] in1, byte[] in2) {
		if (in1 == null || in2 == null || in1.length < in2.length) {
			throw new IllegalArgumentException("Length of first input must be >= length of second input.");
		}

		final byte[] result = new byte[in2.length];
		final int diff = in1.length - in2.length;
		for (int i = 0; i < in2.length; i++) {
			result[i] = (byte) (in1[i + diff] ^ in2[i]);
		}
		return result;
	}

	private static byte[] xorbegin(byte[] in1, byte[] in2) {
		if (in1 == null || in2 == null || in1.length > in2.length) {
			throw new IllegalArgumentException("Length of first input must be <= length of second input.");
		}

		final byte[] result = new byte[in1.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) (in1[i] ^ in2[i]);
		}
		return result;
	}

}
