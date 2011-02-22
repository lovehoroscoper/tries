package net.jpountz.trie;

import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Fake trie implementation based on a HashMap for lookup performance comparison.
 */
public class HashMapTrie<T> extends AbstractTrie<T> {

	private static class MutableString {
		private static final int DEFAULT_CAPACITY = 40;
		private static final float growthFactor = 2;

		private char[] buffer;
		private int length;

		public MutableString() {
			length = 0;
			buffer = new char[DEFAULT_CAPACITY];
		}

		public MutableString(MutableString that) {
			this.length = that.length;
			this.buffer = Arrays.copyOf(that.buffer, length);
		}

		public MutableString(char[] buffer, int offset, int length) {
			this.length = length;
			this.buffer = Arrays.copyOfRange(buffer, offset, offset+length);
		}

		public MutableString(CharSequence sequence, int offset, int length) {
			this.length = length;
			buffer = new char[length];
			for (int i = 0; i < length; ++i) {
				buffer[i] = sequence.charAt(offset + i);
			}
		}

		public int length() {
			return length;
		}

		/*public char charAt(int offset) {
			return buffer[offset];
		}*/

		public MutableString append(char c) {
			if (length == buffer.length) {
				int newCapacity = (int) Math.ceil(growthFactor * length);
				buffer = Arrays.copyOf(buffer, newCapacity);
			}
			buffer[length] = c;
			++length;
			return this;
		}

		public void reset() {
			length = 0;
		}

		public void removeLast() {
			--length;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof MutableString) {
				MutableString that = (MutableString) obj;
				if (this.length == that.length) {
					for (int i = 0; i < this.length; ++i) {
						if (this.buffer[i] != that.buffer[i]) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int hashCode = 0;
			for (int i = 0; i < length; ++i) {
				hashCode = prime * hashCode + buffer[i];
			}
			hashCode = prime * hashCode + length;
			return hashCode;
		}

	}

	private static class HashMapTrieCursor<T> implements Cursor<T> {

		private final HashMapTrie<T> trie;
		private final MutableString prefix;

		public HashMapTrieCursor(HashMapTrie<T> trie, MutableString prefix) {
			this.trie = trie;
			this.prefix = prefix;
		}

		public HashMapTrieCursor(HashMapTrie<T> trie) {
			this(trie, new MutableString());
		}

		@Override
		public boolean moveToChild(char c) {
			prefix.append(c);
			return true;
		}

		@Override
		public void addChild(char c) {
			prefix.append(c);
		}

		@Override
		public void removeChild(char c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean moveToParent() {
			if (prefix.length() == 0) {
				return false;
			} else {
				prefix.removeLast();
				return true;
			}
		}

		@Override
		public void getChildren(CharArrayList children) {
			throw new UnsupportedOperationException();
		}

		@Override
		public T getValue() {
			return trie.map.get(prefix);
		}

		@Override
		public void setValue(T value) {
			trie.map.put(new MutableString(prefix), value);
		}

		@Override
		public void reset() {
			prefix.reset();
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		public HashMapTrieCursor<T> clone() {
			return new HashMapTrieCursor<T>(trie, new MutableString(prefix));
		}
	}

	private HashMap<MutableString, T> map;

	public HashMapTrie(int initialCapacity, float loadFactor) {
		map = new HashMap<MutableString, T>(initialCapacity, loadFactor);
	}

	public HashMapTrie() {
		map = new HashMap<MutableString, T>();
	}

	@Override
	public Cursor<T> getCursor() {
		return new HashMapTrieCursor<T>(this);
	}

	@Override
	public void put(char[] buffer, int offset, int length, T value) {
		map.put(new MutableString(buffer, offset, length), value);
	}

	public void put(CharSequence sequence, int offset, int length, T value) {
		map.put(new MutableString(sequence, offset, length), value);
	}

	@Override
	public T get(char[] buffer, int offset, int length) {
		return map.get(new MutableString(buffer, offset, length));
	}

	@Override
	public T get(CharSequence sequence, int offset, int length) {
		return map.get(new MutableString(sequence, offset, length));
	}

	@Override
	public void clear() {
		map.clear();
	}

}