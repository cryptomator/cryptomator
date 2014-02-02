/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.cleartext;

import java.io.Serializable;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonPropertyOrder(value = { "filenames" })
class Metadata implements Serializable {

	private static final long serialVersionUID = -8160643291781073247L;

	@JsonDeserialize(as = DualHashBidiMap.class)
	private final BidiMap<String, String> filenames = new DualHashBidiMap<>();

	public BidiMap<String, String> getFilenames() {
		return filenames;
	}

}
