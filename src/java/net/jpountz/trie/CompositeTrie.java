package net.jpountz.trie;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.CharCollection;

/**
 * A composite trie composed of a root trie, and several sub-tries.
 * 
 * @param <T> the value type
 */
public class CompositeTrie<T> extends AbstractTrie<T> implements Trie.Optimizable, Trie.Trimmable {

	static class CompositeNode implements Node {
		final Node rootNode;
		final Node childNode;
		public CompositeNode(Node rootNode, Node childNode) {
			this.rootNode = rootNode;
			this.childNode = childNode;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((childNode == null) ? 0 : childNode.hashCode());
			result = prime * result + rootNode.hashCode();
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CompositeNode other = (CompositeNode) obj;
			if (childNode == null) {
				if (other.childNode != null)
					return false;
			} else if (!childNode.equals(other.childNode))
				return false;
			if (!rootNode.equals(other.rootNode))
				return false;
			return true;
		}
	}

	static class CompositeCursor<T> extends AbstractCursor<T> {

		private final CompositeTrie<T> trie;
		private final Cursor<Object> rootCursor;
		private Cursor<T> childCursor;

		private CompositeCursor(CompositeTrie<T> trie,
				Cursor<Object> rootCursor, Cursor<T> childCursor) {
			this.trie = trie;
			this.rootCursor = rootCursor;
			this.childCursor = childCursor;
		}

		public CompositeCursor(CompositeTrie<T> trie) {
			this(trie, trie.backend.getCursor(), null);
		}

		@Override
		protected CharSequence getLabelInternal() {
			// useless for this implementation
			return this.getLabel();
		}

		@Override
		public CompositeCursor<T> clone() {
			return new CompositeCursor<T>(trie, /*subTriesCursor.clone(),*/ rootCursor.clone(),
					childCursor == null ? null : childCursor.clone());
		}

		@Override
		public boolean isAtRoot() {
			return rootCursor.isAtRoot();
		}

		@Override
		public Node getFirstChildNode() {
			if (moveToFirstChild()) {
				Node result = getNode();
				moveToParent();
				return result;
			}
			return null;
		}

		@Override
		public Node getBrotherNode() {
			if (childCursor.isAtRoot() || childCursor.isAtRoot()) {
				return new CompositeNode(rootCursor.getBrotherNode(), null);
			} else {
				return new CompositeNode(rootCursor.getNode(), childCursor.getNode());
			}
		}

		@Override
		public Node getNode() {
			return new CompositeNode(rootCursor.getNode(),
					childCursor == null || childCursor.isAtRoot()
						? null
						: childCursor.getNode());
		}

		@Override
		public void getChildren(Char2ObjectMap<Node> children) {
			if (moveToFirstChild()) {
				do {
					children.put(getEdgeLabel(), getNode());
				} while (moveToBrother());
				moveToParent();
			}
		}

		@Override
		public boolean isAt(Node n) {
			CompositeNode node = (CompositeNode) n;
			return rootCursor.isAt(node.rootNode) &&
				(childCursor == null || (node.childNode == null && childCursor.isAtRoot()) || childCursor.isAt(node.childNode));
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean moveToChild(char c) {
			if (childCursor != null) {
				return childCursor.moveToChild(c);
			} else {
				if (rootCursor.moveToChild(c)) {
					return true;
				} else {
					Object value = rootCursor.getValue();
					if (value != null) {
						if (depth() == trie.rootDepth) {
							childCursor = ((Trie<T>) value).getCursor();
							return childCursor.moveToChild(c);
						}
					}
					return false;
				}
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean moveToFirstChild() {
			if (childCursor != null) {
				if (childCursor.moveToFirstChild()) {
					return true;
				} else {
					return false;
				}
			} else {
				if (rootCursor.moveToFirstChild()) {
					return true;
				} else {
					Object value = rootCursor.getValue();
					if (value != null) {
						if (depth() == trie.rootDepth) {
							childCursor = ((Trie<T>) value).getCursor();
							return childCursor.moveToFirstChild();
						}
					}
					return false;
				}
			}
		}

		@Override
		public boolean moveToBrother() {
			if (childCursor != null) {
				if (!childCursor.isAtRoot()) {
					return childCursor.moveToBrother();
				} else {
					childCursor = null;
				}
			}
			if (rootCursor.moveToBrother()) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void addChild(char c) {
			int depth = depth();
			if (depth < trie.rootDepth) {
				rootCursor.addChild(c);
			} else {
				if (childCursor == null) {
					@SuppressWarnings("unchecked")
					Trie<T> subTrie = (Trie<T>) rootCursor.getValue();
					if (subTrie == null) {
						subTrie = trie.childFactory.newTrie();
						rootCursor.setValue(subTrie);
					}
					childCursor = subTrie.getCursor();
				}
				childCursor.addChild(c);
			}
		}

		@Override
		public int depth() {
			int result = rootCursor.depth();
			if (childCursor != null) {
				result += childCursor.depth();
			}
			return result;
		}

		@Override
		public String getLabel() {
			String result = rootCursor.getLabel();
			if (childCursor != null && !childCursor.isAtRoot()) {
				result += childCursor.getLabel();
			}
			return result;
		}

		@Override
		public char getEdgeLabel() {
			if (childCursor != null && !childCursor.isAtRoot()) {
				return childCursor.getEdgeLabel();
			} else {
				return rootCursor.getEdgeLabel();
			}
		}

		@Override
		public boolean removeChild(char c) {
			if (childCursor != null) {
				return childCursor.removeChild(c);
			} else {
				return rootCursor.removeChild(c);
			}
		}

		@Override
		public boolean moveToParent() {
			if (childCursor != null) {
				if (childCursor.moveToParent()) {
					return true;
				} else {
					childCursor = null;
				}
			}
			return rootCursor.moveToParent();
		}

		@Override
		public void getChildrenLabels(CharCollection children) {
			if (childCursor != null) {
				childCursor.getChildrenLabels(children);
			} else {
				rootCursor.getChildrenLabels(children);
			}
		}

		@Override
		public int getChildrenSize() {
			if (childCursor != null) {
				return childCursor.getChildrenSize();
			} else {
				return rootCursor.getChildrenSize();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public T getValue() {
			if (childCursor != null) {
				return childCursor.getValue();
			} else if (depth() == trie.rootDepth) {
				Trie<T> subTrie = (Trie<T>) rootCursor.getValue();
				if (subTrie != null) {
					return subTrie.get("");
				} else {
					return null;
				}
			} else {
				return (T) rootCursor.getValue();
			}
		}

		@Override
		public void setValue(T value) {
			if (childCursor != null) {
				childCursor.setValue(value);
			} else if (depth() == trie.rootDepth) {
				@SuppressWarnings("unchecked")
				Trie<T> subTrie = (Trie<T>) rootCursor.getValue();
				if (subTrie == null) {
					subTrie = trie.childFactory.newTrie();
					rootCursor.setValue(subTrie);
				}
				subTrie.put("", value);
			} else {
				rootCursor.setValue(value);
			}
		}

		@Override
		public void reset() {
			rootCursor.reset();
			childCursor = null;
		}

		@Override
		public int size() {
			if (childCursor != null) {
				return childCursor.size();
			} else {
				Node node = rootCursor.getNode();
				int size = 1;
				while (Trie.Traversal.DEPTH_FIRST.moveToNextNode(node, rootCursor)) {
					if (depth() == trie.rootDepth) {
						@SuppressWarnings("unchecked")
						Trie<T> subTrie = (Trie<T>) rootCursor.getValue();
						size += subTrie.size();
					} else {
						++size;
					}
				}
				return size;
			}
		}

	}

	private final TrieFactory<T> childFactory;
	private final Trie<Object> backend;
	private final int rootDepth;
	private final boolean subTriesAreOptimizable;
	private final boolean subTriesAreTrimmable;

	public CompositeTrie(TrieFactory<Object> parentFactory,
			TrieFactory<T> childFactory, int rootDepth) {
		if (rootDepth <= 0) {
			throw new IllegalArgumentException("rootDepth must be > 0");
		}
		this.rootDepth = rootDepth;
		backend = parentFactory.newTrie();
		this.childFactory = childFactory;
		Trie<T> subTrie = this.childFactory.newTrie();
		subTriesAreOptimizable = subTrie instanceof Optimizable;
		subTriesAreTrimmable   = subTrie instanceof Trimmable;
	}

	@Override
	public Cursor<T> getCursor() {
		return new CompositeCursor<T>(this);
	}

	@Override
	public void put(char[] buffer, int offset, int length, T value) {
		if (length < rootDepth) {
			backend.put(buffer, offset, length, value);
		} else {
			@SuppressWarnings("unchecked")
			Trie<T> sub = (Trie<T>) backend.get(buffer, offset, rootDepth);
			if (sub == null) {
				sub = childFactory.newTrie();
				backend.put(buffer, offset, rootDepth, sub);
			}
			sub.put(buffer, offset + rootDepth, length - rootDepth, value);
		}
	}

	@Override
	public void put(CharSequence sequence, int offset, int length, T value) {
		if (length < rootDepth) {
			backend.put(sequence, offset, length, value);
		} else {
			@SuppressWarnings("unchecked")
			Trie<T> sub = (Trie<T>) backend.get(sequence, offset, rootDepth);
			if (sub == null) {
				sub = childFactory.newTrie();
				backend.put(sequence, offset, rootDepth, sub);
			}
			sub.put(sequence, offset + rootDepth, length - rootDepth, value);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(char[] buffer, int offset, int length) {
		if (length < rootDepth) {
			return (T) backend.get(buffer, offset, length);
		} else {
			Trie<T> sub = (Trie<T>) backend.get(buffer, offset, rootDepth);
			if (sub != null) {
				return sub.get(buffer, offset + rootDepth, length - rootDepth);
			} else {
				return null;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(CharSequence sequence, int offset, int length) {
		if (length < rootDepth) {
			return (T) backend.get(sequence, offset, length);
		} else {
			Trie<T> sub = (Trie<T>) backend.get(sequence, offset, rootDepth);
			if (sub != null) {
				return sub.get(sequence, offset + rootDepth, length - rootDepth);
			} else {
				return null;
			}
		}
	}

	@Override
	public void clear() {
		//root.clear();
		//subTries.clear();
		backend.clear();
	}

	@Override
	public void trimToSize() {
		if (backend instanceof Trimmable) {
			((Trimmable) backend).trimToSize();
		}
		if (subTriesAreTrimmable) {
			Cursor<Object> cursor = backend.getCursor();
			Node root = cursor.getNode();
			do {
				if (cursor.depth() == rootDepth) {
					break;
				}
			} while (Trie.Traversal.DEPTH_FIRST.moveToNextNode(root, cursor));
			do {
				if (cursor.depth() != rootDepth) {
					break;
				}
				Object value = cursor.getValue();
				if (value != null) {
					((Trimmable) value).trimToSize();
				}
			} while (Trie.Traversal.BREADTH_FIRST.moveToNextNode(root, cursor));
		}
	}

	@Override
	public void optimizeFor(Trie.Traversal traversal) {
		if (backend instanceof Optimizable) {
			((Optimizable) backend).optimizeFor(traversal);
		}
		if (subTriesAreOptimizable) {
			Cursor<Object> cursor = backend.getCursor();
			Node root = cursor.getNode();
			do {
				if (cursor.depth() == rootDepth) {
					break;
				}
			} while (Trie.Traversal.DEPTH_FIRST.moveToNextNode(root, cursor));
			do {
				if (cursor.depth() != rootDepth) {
					break;
				}
				Object value = cursor.getValue();
				if (value != null) {
					((Optimizable) value).optimizeFor(traversal);
				}
			} while (Trie.Traversal.BREADTH_FIRST.moveToNextNode(root, cursor));
		}
	}

}
