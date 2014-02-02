/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class PseudonymRepository {
	
	private static final Node ROOT = new Node(null, "/", "/");
	
	private PseudonymRepository() {
		throw new IllegalStateException();
	}
	
	/**
	 * @return The deepest resolvable cleartext path for the requested pseudonymized path.
	 */
	public static List<String> cleartextPathComponents(final List<String> pseudonymizedPathComponents) {
		final List<String> result = new ArrayList<>(pseudonymizedPathComponents.size());
		Node node = ROOT;
		for (final String pseudonym : pseudonymizedPathComponents) {
			node = node.subnodesByPseudonym.get(pseudonym);
			if (node == null) {
				return result;
			}
			result.add(node.cleartext);
		}
		return result;
	}
	
	/**
	 * @return The deepest resolvable pseudonymized path for the requested cleartext path.
	 */
	public static List<String> pseudonymizedPathComponents(final List<String> cleartextPathComponents) {
		final List<String> result = new ArrayList<>(cleartextPathComponents.size());
		Node node = ROOT;
		for (final String cleartext : cleartextPathComponents) {
			Node subnode = node.subnodesByCleartext.get(cleartext);
			if (subnode == null) {
				return result;
			}
			node = subnode;
			result.add(node.pseudonym);
		}
		return result;
	}
	
	/**
	 * Caches a path of cleartext/pseudonym pairs.
	 */
	public static void registerPath(final List<String> cleartextPathComponents, final List<String> pseudonymPathComponents) {
		if (cleartextPathComponents.size() != pseudonymPathComponents.size()) {
			throw new IllegalArgumentException("Cannot register pseudonymized path, that isn't matching the length of its cleartext equivalent.");
		}
		
		Node node = ROOT;
		for (int i=0; i<cleartextPathComponents.size(); i++) {
			final String cleartextComp = cleartextPathComponents.get(i);
			final String pseudonymComp = pseudonymPathComponents.get(i);
			node = node.getOrCreateSubnode(cleartextComp, pseudonymComp);
		}
	}
	
	/**
	 * Removes a path of cleartext/pseudonym pairs from the cache.
	 */
	public static void unregisterPath(final List<String> pseudonymPathComponents) {
		Node node = ROOT;
		for (final String pseudonymComp : pseudonymPathComponents) {
			node = node.subnodesByPseudonym.get(pseudonymComp);
		}
		if (!ROOT.equals(node)) {
			node.detach();
		}
	}
	
	
	/**
	 * Node in a tree of cleartext/pseudonym pairs, that can be traversed root to leaf. The whole tree is threadsafe.
	 * As each node of the tree has its own synchronization, multithreaded access is balanced.
	 */
	private static final class Node {
		private final Node parent;
		private final String cleartext;
		private final String pseudonym;
		private final Map<String, Node> subnodesByCleartext;
		private final Map<String, Node> subnodesByPseudonym;
		
		Node(Node parent, String cleartext, String pseudonym) {
			this.parent = parent;
			this.cleartext = cleartext;
			this.pseudonym = pseudonym;
			this.subnodesByCleartext = new ConcurrentHashMap<>();
			this.subnodesByPseudonym = new ConcurrentHashMap<>();
		}
		
		/**
		 * @return New subnode attached to this.
		 */
		Node getOrCreateSubnode(String cleartext, String pseudonym) {
			if (subnodesByCleartext.containsKey(cleartext) && subnodesByPseudonym.containsKey(pseudonym)) {
				return subnodesByCleartext.get(cleartext);
			}
			final Node subnode = new Node(this, cleartext, pseudonym);
			this.subnodesByCleartext.put(cleartext, subnode);
			this.subnodesByPseudonym.put(pseudonym, subnode);
			return subnode;
		}

		/**
		 * Removes a node from its parent node.
		 */
		void detach() {
			// the following two lines don't need to be synchronized,
			// as inconsistencies are self-healing over the transactional metadata files.
			this.parent.subnodesByCleartext.remove(this.cleartext);
			this.parent.subnodesByPseudonym.remove(this.pseudonym);
		}
		
		@Override
		public int hashCode() {
			final HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(parent);
			hash.append(cleartext);
			hash.append(pseudonym);
			return hash.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Node) {
				final Node other = (Node) obj;
				final EqualsBuilder eq = new EqualsBuilder();
				eq.append(this.parent, other.parent);
				eq.append(this.cleartext, other.cleartext);
				eq.append(this.pseudonym, other.pseudonym);
				return eq.isEquals();
			} else {
				return false;
			}
		}
		
	}

}
