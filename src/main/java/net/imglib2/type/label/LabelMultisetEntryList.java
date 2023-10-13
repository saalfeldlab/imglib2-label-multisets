package net.imglib2.type.label;

import net.imglib2.type.label.LabelMultisetType.Entry;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class LabelMultisetEntryList
		extends MappedObjectArrayList<LabelMultisetEntry, LongMappedAccess> {

	public LabelMultisetEntryList() {

		super(LabelMultisetEntry.type);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LabelMultisetEntryList)) return false;
		final long[] thisData = ((LongMappedAccessData) data).getData();
		final long[] otherData = ((LongMappedAccessData) ((LabelMultisetEntryList) o).data).getData();
		return Arrays.equals(thisData, otherData);
	}

	public LabelMultisetEntryList(final int capacity) {

		super(LabelMultisetEntry.type, capacity);
	}

	public LabelMultisetEntryList(final LongMappedAccessData data, final long baseOffset) {

		super(LabelMultisetEntry.type, data, baseOffset);
	}

	protected int multisetSize() {

		int size = 0;
		for (final LabelMultisetEntry e : this) {
			size += e.getCount();
		}
		return size;
	}

	/**
	 * Performs a binary search for entry with
	 * {@link LabelMultisetEntry#getId()} <tt>id</tt> in the entire list. Note
	 * that you <b>must</b> {@link #sortById sort} the list before doing a
	 * search.
	 *
	 * @param id the value to search for
	 * @return the absolute offset in the list of the value, or its negative
	 * insertion point into the sorted list.
	 */
	public int binarySearch(final long id) {

		return binarySearch(id, 0, size());
	}

	/**
	 * Performs a binary search for entry with
	 * {@link LabelMultisetEntry#getId()} <tt>id</tt> in the entire list. Note
	 * that you <b>must</b> {@link #sortById sort} the list before doing a
	 * search.
	 *
	 * @param id  the value to search for
	 * @param ref the reference used to store the intermediate values while comparing
	 * @return the absolute offset in the list of the value, or its negative
	 * insertion point into the sorted list.
	 */
	public int binarySearch(final long id, LabelMultisetEntry ref) {

		return binarySearch(id, 0, size(), ref);
	}

	/**
	 * Performs a binary search for entry with
	 * {@link LabelMultisetEntry#getId()} <tt>id</tt> in the specified range.
	 * Note that you <b>must</b> {@link #sortById sort} the list or the range
	 * before doing a search.
	 *
	 * @param id        the value to search for
	 * @param fromIndex the lower boundary of the range (inclusive)
	 * @param toIndex   the upper boundary of the range (exclusive)
	 * @return the absolute offset in the list of the value, or its negative
	 * insertion point into the sorted list.
	 */
	public int binarySearch(final long id, final int fromIndex, final int toIndex) {

		if (fromIndex < 0) {
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		}
		if (toIndex > size()) {
			throw new ArrayIndexOutOfBoundsException(toIndex);
		}

		final LabelMultisetEntry ref = createRef();
		final int idx = binarySearch(id, fromIndex, toIndex, ref);
		releaseRef(ref);
		return idx;
	}

	/**
	 * Performs a binary search for entry with
	 * {@link LabelMultisetEntry#getId()} <tt>id</tt> in the specified range.
	 * Note that you <b>must</b> {@link #sortById sort} the list or the range
	 * before doing a search.
	 *
	 * @param id        the value to search for
	 * @param fromIndex the lower boundary of the range (inclusive)
	 * @param toIndex   the upper boundary of the range (exclusive)
	 * @param ref       the reference used to store the intermediate values while comparing
	 * @return the absolute offset in the list of the value, or its negative
	 * insertion point into the sorted list.
	 */
	public int binarySearch(final long id, final int fromIndex, final int toIndex, LabelMultisetEntry ref) {

		if (fromIndex < 0) {
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		}
		if (toIndex > size()) {
			throw new ArrayIndexOutOfBoundsException(toIndex);
		}

		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			final int mid = low + high >>> 1;
			final long midVal = get(mid, ref).getId();
			if (midVal < id) {
				low = mid + 1;
			} else if (midVal > id) {
				high = mid - 1;
			} else {
				return mid; // value found
			}
		}
		return -(low + 1); // value not found.
	}

	/**
	 * Sort the list by {@link LabelMultisetEntry#getId()}.
	 */
	// TODO: should this be protected / package private?
	public void sortById() {

		sort((o1, o2) -> {
			final long i1 = o1.getId();
			final long i2 = o2.getId();
			return Long.compare(i1, i2);
		});
	}

	/**
	 * Merge consecutive {@link LabelMultisetEntry entries} with the same
	 * {@link LabelMultisetEntry#getId() id}.
	 */
	public void mergeConsecutiveEntries() {

		final int oldSize = size();
		if (oldSize < 2) {
			return;
		}

		int newSize = oldSize;
		final LabelMultisetEntry oldTail = createRef();
		final LabelMultisetEntry newTail = createRef();
		int newPos = 0;
		get(newPos, newTail);
		for (int oldPos = 1; oldPos < oldSize; ++oldPos) {
			get(oldPos, oldTail);
			if (oldTail.getId() == newTail.getId()) {
				newTail.setCount(newTail.getCount() + oldTail.getCount());
				--newSize;
			} else {
				get(++newPos, newTail);
				if (newPos != oldPos) {
					newTail.set(oldTail);
				}
			}
		}

		setSize(newSize);
	}

	/**
	 * Merge with other list. Both lists must be sorted.
	 *
	 * @param list
	 */
	public void mergeWith(final LabelMultisetEntryList list) {

		if (list.isEmpty()) {
			return;
		}

		if (isEmpty()) {
			for (final LabelMultisetEntry e : list) {
				this.add(e);
			}
			return;
		}

		final LabelMultisetEntry e1 = createRef();
		final LabelMultisetEntry e2 = createRef();
		int i = 0;
		int j = 0;
		long id1 = this.get(i, e1).getId();
		long id2 = list.get(j, e2).getId();
		A:
		while (true) {
			if (id1 == id2) {
				e1.setCount(e1.getCount() + e2.getCount());
				if (++j >= list.size()) {
					break;
				}
				id2 = list.get(j, e2).getId();
			} else if (id2 < id1) {
				this.add(i, e2); // insert e2 at i
				get(++i, e1).getId(); // e1 ends up at same element which is
				// now shifted
				if (++j >= list.size()) {
					break;
				}
				id2 = list.get(j, e2).getId();
			} else // ( id2 > id1 )
			{
				while (++i < size()) {
					id1 = get(i, e1).getId();
					if (id2 <= id1) {
						continue A;
					}
				}
				for (; j < list.size(); ++j) {
					this.add(list.get(j, e2));
				}
				break;
			}
		}
		releaseRef(e2);
		releaseRef(e1);
	}

	public void mergeWith(final Multiset<Label> multiset) {

		mergeWith(multiset.entrySet());
	}

	public void mergeWith(final Set<Entry<Label>> entrySet) {

		if (entrySet.isEmpty()) {
			return;
		}

		if (isEmpty()) {
			final LabelMultisetEntry e1 = createRef();
			this.ensureCapacity(entrySet.size());
			this.setSize(entrySet.size());
			int i = 0;
			for (final Entry<Label> e : entrySet) {
				get(i++, e1);
				e1.setId(e.getElement().id());
				e1.setCount(e.getCount());
			}
			releaseRef(e1);
			return;
		}

		final LabelMultisetEntry e1 = createRef();
		final Iterator<Entry<Label>> iter = entrySet.iterator();
		Entry<Label> e2 = iter.next();
		int i = 0;
		long id1 = get(i, e1).getId();
		long id2 = e2.getElement().id();
		A:
		while (true) {
			if (id1 == id2) {
				e1.setCount(e1.getCount() + e2.getCount());
				if (!iter.hasNext()) {
					break;
				}
				e2 = iter.next();
				id2 = e2.getElement().id();
			} else if (id2 < id1) {
				this.add(i, e1); // insert e2 at i
				e1.setId(id2);
				e1.setCount(e2.getCount());
				get(++i, e1).getId(); // e1 ends up at same element which is
				// now shifted
				if (!iter.hasNext()) {
					break;
				}
				e2 = iter.next();
				id2 = e2.getElement().id();
			} else // ( id2 > id1 )
			{
				while (++i < size()) {
					id1 = get(i, e1).getId();
					if (id2 <= id1) {
						continue A;
					}
				}
				while (true) {
					this.add(e1);
					get(i++, e1);
					e1.setId(id2);
					e1.setCount(e2.getCount());
					if (!iter.hasNext()) {
						break;
					}
					e2 = iter.next();
					id2 = e2.getElement().id();
				}
				break;
			}
		}
		releaseRef(e1);
	}
}
