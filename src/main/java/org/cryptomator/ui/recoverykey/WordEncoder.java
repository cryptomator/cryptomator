package org.cryptomator.ui.recoverykey;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
class WordEncoder {

	private static final String DEFAULT_WORD_FILE = "/i18n/4096words_en.txt";
	private static final int WORD_COUNT = 4096;
	private static final char DELIMITER = ' ';

	private final List<String> words;
	private final Map<String, Integer> indices;

	@Inject
	public WordEncoder() {
		this(DEFAULT_WORD_FILE);
	}

	public List<String> getWords() {
		return words;
	}

	public WordEncoder(String wordFile) {
		try (InputStream in = getClass().getResourceAsStream(wordFile); //
			 Reader reader = new InputStreamReader(in, StandardCharsets.US_ASCII.newDecoder()); //
			 BufferedReader bufferedReader = new BufferedReader(reader)) {
			this.words = bufferedReader.lines().limit(WORD_COUNT).collect(Collectors.toUnmodifiableList());
		} catch (IOException e) {
			throw new IllegalArgumentException("Unreadable file: " + wordFile, e);
		}
		if (words.size() < WORD_COUNT) {
			throw new IllegalArgumentException("Insufficient input file: " + wordFile);
		}
		this.indices = Map.ofEntries(IntStream.range(0, WORD_COUNT).mapToObj(i -> Map.entry(words.get(i), i)).toArray(Map.Entry[]::new));
	}

	/**
	 * Encodes the given input as a sequence of words.
	 *
	 * @param input A multiple of three bytes
	 * @return A String that can be {@link #decode(String) decoded} to the input again.
	 * @throws IllegalArgumentException If input is not a multiple of three bytes
	 */
	public String encodePadded(byte[] input) {
		Preconditions.checkArgument(input.length % 3 == 0, "input needs to be padded to a multipe of three");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length; i += 3) {
			byte b1 = input[i];
			byte b2 = input[i + 1];
			byte b3 = input[i + 2];
			int firstWordIndex = (0xFF0 & (b1 << 4)) + (0x00F & (b2 >> 4)); // 0xFFF000
			int secondWordIndex = (0xF00 & (b2 << 8)) + (0x0FF & b3); // 0x000FFF
			assert firstWordIndex < WORD_COUNT;
			assert secondWordIndex < WORD_COUNT;
			sb.append(words.get(firstWordIndex)).append(DELIMITER);
			sb.append(words.get(secondWordIndex)).append(DELIMITER);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1); // remove last space
		}
		return sb.toString();
	}

	/**
	 * Decodes a String that has previously been {@link #encodePadded(byte[]) encoded} to a word sequence.
	 *
	 * @param encoded The word sequence
	 * @return Decoded bytes
	 * @throws IllegalArgumentException If the encoded string doesn't consist of a multiple of two words or one of the words is unknown to this encoder.
	 */
	public byte[] decode(String encoded) {
		List<String> splitted = Splitter.on(DELIMITER).omitEmptyStrings().splitToList(Strings.nullToEmpty(encoded));
		Preconditions.checkArgument(splitted.size() % 2 == 0, "%s needs to be a multiple of two words", encoded);
		byte[] result = new byte[splitted.size() / 2 * 3];
		for (int i = 0; i < splitted.size(); i += 2) {
			String w1 = splitted.get(i);
			String w2 = splitted.get(i + 1);
			int firstWordIndex = indices.getOrDefault(w1, -1);
			int secondWordIndex = indices.getOrDefault(w2, -1);
			Preconditions.checkArgument(firstWordIndex != -1, "%s not in dictionary", w1);
			Preconditions.checkArgument(secondWordIndex != -1, "%s not in dictionary", w2);
			byte b1 = (byte) (0xFF & (firstWordIndex >> 4));
			byte b2 = (byte) ((0xF0 & (firstWordIndex << 4)) + (0x0F & (secondWordIndex >> 8)));
			byte b3 = (byte) (0xFF & secondWordIndex);
			result[i / 2 * 3] = b1;
			result[i / 2 * 3 + 1] = b2;
			result[i / 2 * 3 + 2] = b3;
		}
		return result;
	}


}
