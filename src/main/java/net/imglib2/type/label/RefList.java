package net.imglib2.type.label;

import java.util.Iterator;
import java.util.List;

public interface RefList<O> extends List<O> {

	interface RefIterator<O> extends Iterator<O> {

		void release();

		void reset();
	}

	O createRef();

	void releaseRef(final O ref);

	O get(final int index, final O ref);

	@Override
	RefIterator<O> iterator();
}
