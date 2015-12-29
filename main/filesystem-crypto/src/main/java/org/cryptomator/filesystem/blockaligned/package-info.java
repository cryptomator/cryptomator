/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
/**
 * Provides a decoration layer for the {@link org.cryptomator.filesystem Filesystem API}, which guarantees, that all read/write attempts to underlying files always begin at a block start position.
 * Block start positions are integer multiples of a block size + a fixed block shift.
 * <p>
 * In general the formula to align a requested read with a physical read is <code>floor(x / blockSize) * blockSize</code><br/>
 * For example <code>blockSize=10</code> result in the following block-aligned read/write attempts:
 * 
 * <table>
 * <thead>
 * 	<tr><th>Requested Read</th><th>Physical Read</th></tr>
 * </thead>
 * <tbody>
 * 	<tr><td>0</td><td>0</td></td>
 * 	<tr><td>5</td><td>0</td></td>
 * 	<tr><td>9</td><td>0</td></td>
 * 	<tr><td>10</td><td>10</td></td>
 * 	<tr><td>11</td><td>10</td></td>
 * 	<tr><td>35</td><td>30</td></td>
 * </tbody>
 * </table>
 */
package org.cryptomator.filesystem.blockaligned;