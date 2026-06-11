package net.imglib2.type.label;

class ComparableLabelMultisetEntryList extends LabelMultisetEntryList implements Comparable<LabelMultisetEntryList> {

	private final LabelMultisetEntry ref;

	ComparableLabelMultisetEntryList() {
		this(new LabelMultisetEntry());
	}

	ComparableLabelMultisetEntryList(final LabelMultisetEntry ref) {

		this.ref = ref;
	}

	ComparableLabelMultisetEntryList(final LabelMultisetEntryList list) {
		this(list, new LabelMultisetEntry());

	}
	ComparableLabelMultisetEntryList(final LabelMultisetEntryList list, final LabelMultisetEntry ref) {

		this.ref = ref;
		referToDataAt(list.data, list.getBaseOffset());
	}

	@Override public LabelMultisetEntry createRef() {

		return ref;
	}

	@Override public int compareTo(LabelMultisetEntryList o) {

		final int size = size();

		/* if different sizes or empty, return result of compare size*/
		final int sizeCompare = Integer.compare(size, o.size());
		if (sizeCompare != 0 || size == 0)
			return sizeCompare;

		final RefIterator<LabelMultisetEntry> thisIter = iterator();
		final RefIterator<LabelMultisetEntry> otherIter = o.iterator();

		for (int i = 0; i < size; i++) {
			final LabelMultisetEntry thisNext = thisIter.next();
			final LabelMultisetEntry otherNext = otherIter.next();

			final int idCompare = Long.compare(thisNext.getId(), otherNext.getId());
			if (idCompare != 0)
				return idCompare;

			final int countCompare = Integer.compare(thisNext.getCount(), otherNext.getCount());
			if (countCompare != 0)
				return countCompare;
		}

		return 0;
	}
}
