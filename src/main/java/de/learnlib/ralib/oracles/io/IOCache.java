package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import de.learnlib.ralib.exceptions.NonDeterminismException;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOCache {

	private final CacheNode root;

	static class CacheNode {

		final Map<PSymbolInstance, PSymbolInstance> output = new LinkedHashMap<>();
		final Map<PSymbolInstance, CacheNode> next = new LinkedHashMap<>();

		public String toString() {
			StringBuilder builder = new StringBuilder().append("(");
			output.keySet().forEach(key -> builder.append(key).append("\n ").append(output.get(key)).append("\n ")
					.append(next.get(key).toString()));
			builder.append(")");
			return builder.toString();
		}

		public CacheNode getCacheExcluding(BiPredicate<PSymbolInstance, PSymbolInstance> exclusionPredicate) {
			CacheNode node = new CacheNode();
			for (PSymbolInstance in : this.next.keySet()) {
				PSymbolInstance out = output.get(in);
				CacheNode n = next.get(in);
				if (!exclusionPredicate.test(in, out)) {
					node.output.put(in, out);
					node.next.put(in, n.getCacheExcluding(exclusionPredicate));
				}
			}
			return node;
		}

		public CacheNode getCacheExcluding(Predicate<Word<PSymbolInstance>> exclusionPredicate) {
			CacheNode node = this.getCacheExcluding(Word.epsilon(), exclusionPredicate);
			return node;
		}

		public List<Word<PSymbolInstance>> getWords(Predicate<Word<PSymbolInstance>> selectionPredicate) {
			List<Word<PSymbolInstance>> lst = new ArrayList<>();
			collectWords(lst, Word.epsilon(), selectionPredicate);
			return lst;
		}

		private void collectWords(Collection<Word<PSymbolInstance>> collection, Word<PSymbolInstance> prefix,
				Predicate<Word<PSymbolInstance>> selectionPredicate) {
			for (PSymbolInstance in : this.next.keySet()) {
				CacheNode n = next.get(in);
				PSymbolInstance out = output.get(in);
				Word<PSymbolInstance> newPrefix = prefix.append(in).append(out);
				if (selectionPredicate.test(newPrefix))
					collection.add(newPrefix);
				n.collectWords(collection, newPrefix, selectionPredicate);
			}
		}

		private CacheNode getCacheExcluding(Word<PSymbolInstance> prefix,
				Predicate<Word<PSymbolInstance>> exclusionPredicate) {
			CacheNode node = new CacheNode();
			for (PSymbolInstance in : this.next.keySet()) {
				CacheNode n = next.get(in);
				PSymbolInstance out = output.get(in);
				Word<PSymbolInstance> newPrefix = prefix.append(in).append(out);
				if (!exclusionPredicate.test(newPrefix)) {
					node.output.put(in, out);
					node.next.put(in, n.getCacheExcluding(newPrefix, exclusionPredicate));
				}
			}
			return node;
		}

		public CacheNode getCacheExcluding(Word<PSymbolInstance> exclusionPrefix) {
			CacheNode node = this.getCacheExcluding((w) -> exclusionPrefix.isPrefixOf(w));

			return node;
		}

		public int size() {
			int childrenSize = this.next.values().stream().mapToInt(node -> node.size()).sum();
			return childrenSize + 1;
		}
	}

	public IOCache() {
		this.root = new CacheNode();
	}

	IOCache(CacheNode root) {
		this.root = root;
	}

	/**
	 * Adds query to cache. Returns true if the cache was updated with
	 * information from the query, false, if the query was already found in the
	 * cache.
	 */
	boolean addToCache(Word<PSymbolInstance> query) {
		assert query.length() % 2 == 0;
		Iterator<PSymbolInstance> iter = query.iterator();
		CacheNode cur = root;
		boolean cacheUpdated = false;
		int index = 0;
		while (iter.hasNext()) {
			PSymbolInstance in = iter.next();
			PSymbolInstance out = iter.next();
			index = index + 2;

			CacheNode next = cur.next.get(in);
			if (next != null) {
				// check for non-determinism
				if (!out.equals(cur.output.get(in))) {
					throw new NonDeterminismException(query.prefix(index - 1), cur.output.get(in), out);
				}
			} else {
				next = new CacheNode();
				cur.next.put(in, next);
				cur.output.put(in, out);
				cacheUpdated = true;
			}

			cur = next;
		}
		return cacheUpdated;
	}

	Boolean answerFromCache(Word<PSymbolInstance> query) {
		Iterator<PSymbolInstance> iter = query.iterator();
		PSymbolInstance out = null;
		CacheNode cur = root;

		while (iter.hasNext()) {

			PSymbolInstance in = iter.next();
			out = cur.output.get(in);

			if (out == null) {
				return null;
			}

			if (!iter.hasNext()) {
				// only input is left ...
				return Boolean.TRUE;
			}

			cur = cur.next.get(in);

			PSymbolInstance ref = iter.next();

			if (!out.equals(ref)) {
				return Boolean.FALSE;
			}

		}
		return Boolean.TRUE;
	}

	Word<PSymbolInstance> traceFromCache(Word<PSymbolInstance> query) {
		Word<PSymbolInstance> trace = Word.epsilon();

		Iterator<PSymbolInstance> iter = query.iterator();
		PSymbolInstance out = null;
		CacheNode cur = root;

		while (iter.hasNext()) {

			PSymbolInstance in = iter.next();
			PSymbolInstance ref = iter.next();

			out = cur.output.get(in);
			cur = cur.next.get(in);

			if (out == null) {
				return null;
			}

			trace = trace.append(in).append(out);

		}
		return trace;
	}

	/**
	 * Also performs canonization of the query as it searches the cache. This
	 * helps improve the cache hit rate.
	 * 
	 */
	Word<PSymbolInstance> traceFromCache(Word<PSymbolInstance> query, TraceCanonizer traceCanonizer) {
		Word<PSymbolInstance> trace = Word.epsilon();

		Iterator<PSymbolInstance> iter = query.iterator();
		PSymbolInstance out = null;
		CacheNode cur = root;

		while (iter.hasNext()) {

			PSymbolInstance in = iter.next();
			PSymbolInstance ignoredOut = iter.next();

			trace = trace.append(in);
			trace = traceCanonizer.canonize(trace);
			PSymbolInstance canonizedIn = trace.lastSymbol();

			out = cur.output.get(canonizedIn);
			cur = cur.next.get(canonizedIn);

			if (out == null) {
				return null;
			}

			trace = trace.append(out);

		}
		return trace;
	}

	CacheNode getRoot() {
		return this.root;
	}

	public IOCache asThreadSafeCache() {
		ThreadSafeIOCache tCache = new ThreadSafeIOCache(this);
		return tCache;

	}

	public IOCache getCacheExcluding(BiPredicate<PSymbolInstance, PSymbolInstance> exclusionPredicate) {
		return new IOCache(this.root.getCacheExcluding(exclusionPredicate));
	}

	public IOCache getCacheExcluding(Word<PSymbolInstance> exclusionPrefix) {
		return new IOCache(this.root.getCacheExcluding(exclusionPrefix));
	}

	public IOCache getCacheExcluding(Predicate<Word<PSymbolInstance>> exclusionPredicate) {
		return new IOCache(this.root.getCacheExcluding(exclusionPredicate));
	}

	class ThreadSafeIOCache extends IOCache {
		public ThreadSafeIOCache(IOCache cache) {
			super(cache.getRoot());
		}

		public Object lock = IOCache.class;

		public boolean addToCache(Word<PSymbolInstance> query) {
			synchronized (lock) {
				Boolean res = this.answerFromCache(query);
				if (res == null)
					return super.addToCache(query);
				else
					return false;
			}
		}

		public Boolean answerFromCache(Word<PSymbolInstance> query) {
			synchronized (lock) {
				Boolean res = super.answerFromCache(query);
				return res;
			}
		}
	}

	public int getSize() {
		return this.root.size();
	}

	public List<Word<PSymbolInstance>> getWords(Predicate<Word<PSymbolInstance>> selectionPrefix) {
		return this.root.getWords(selectionPrefix);
	}

}
