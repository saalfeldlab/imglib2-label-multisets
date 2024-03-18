package net.imglib2.type.label;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

// TOOD: make unmodifiable version
public class MappedObjectArrayList<O extends MappedObject<O, T>, T extends MappedAccess<T>>
		extends AbstractList<O>
		implements RefList<O> {

  private static final int DEFAULT_CAPACITY = 1;

  private final O type;

  protected MappedAccessData<T> data;

  private long baseOffset;

  private long elementBaseOffset;

  private final T access;

  private int size;

  private final ConcurrentLinkedQueue<O> tmpObjRefs = new ConcurrentLinkedQueue<>();

  public MappedObjectArrayList(final O type) {

	this(type, DEFAULT_CAPACITY);
  }

  public MappedObjectArrayList(final O type, final int capacity) {

	this(type, type.storageFactory.createStorage(ByteUtils.INT_SIZE + capacity * type.getSizeInBytes()), 0);
  }

  protected MappedObjectArrayList(final O type, final MappedAccessData<T> data, final long baseOffset) {

	this.type = type;
	this.access = type.storageFactory.createAccess();

	referToDataAt(data, baseOffset);
	ensureCapacity(0);
  }

  /**
   * make this object refer to a different list.
   */
  protected void referToDataAt(final MappedAccessData<T> data, final long baseOffset) {

	this.data = data;
	this.baseOffset = baseOffset;
	this.elementBaseOffset = baseOffset + ByteUtils.INT_SIZE;
	data.updateAccess(access, baseOffset);
	size = access.getInt(0);
  }

  public void createListAt(final MappedAccessData<T> data, final long baseOffset) {

	referToDataAt(data, baseOffset);
	clear();
  }

  /**
   * Ensure capacity for size field and set size to 0.
   */
  @Override
  public void clear() {

	ensureCapacity(0);
	setSize(0);
  }

  protected void setSize(final int size) {

	access.putInt(size, 0);
	this.size = size;
  }

  @Override
  public O createRef() {

	final O obj = tmpObjRefs.poll();
	return obj == null ? type.createRef() : obj;
  }

  private O createRefAt(final int index) {

	final O ref = createRef();
	setRefAt(ref, index);
	return ref;
  }

  private void setRefAt(final O ref, final int index) {

	data.updateAccess(ref.access, elementBaseOffset + index * elementSizeInBytes());
  }

  @Override
  public void releaseRef(final O ref) {

	tmpObjRefs.add(ref);
  }

  private int elementSizeInBytes() {

	return type.getSizeInBytes();
  }

  protected void ensureCapacity(final int size) {

	final int required = (size + 1) * elementSizeInBytes();
	if (data.size() < elementBaseOffset + required)
	  data.resize(2 * (elementBaseOffset + required));
  }

  public long getBaseOffset() {

	return baseOffset;
  }

  public long getSizeInBytes() {

	return ByteUtils.INT_SIZE + size() * type.getSizeInBytes();
  }

  @Override
  public int size() {

	return size;
  }

  @Override
  public O get(final int index) {

	if (index < 0 || index >= size())
	  throw new IndexOutOfBoundsException();
	return createRefAt(index);
  }

  @Override
  public O get(final int index, final O ref) {

	if (index < 0 || index >= size())
	  throw new IndexOutOfBoundsException();
	setRefAt(ref, index);
	return ref;
  }

  @Override
  public O set(final int index, final O element) {

	if (index < 0 || index >= size())
	  throw new IndexOutOfBoundsException();
	final O ref = createRef();
	set(index, element, ref);
	releaseRef(ref);
	return null;
  }

  public O set(final int index, final O element, final O ref) {

	if (index < 0 || index >= size())
	  throw new IndexOutOfBoundsException();
	setRefAt(ref, index);
	ref.set(element);
	return null;
  }

	@Override public boolean addAll(Collection<? extends O> c) {

	  return addAll(size(), c);
	}

	public boolean addAll(Collection<? extends O> c, O ref) {

		return addAll(size(), c, ref);
	}

	@Override public boolean addAll(int index, Collection<? extends O> c) {

	  	final O ref = createRef();
		final boolean modified = addAll(index, c, ref);
		releaseRef(ref);
		return modified;
	}

	public boolean addAll(int index, Collection<? extends O> c, O ref) {

		boolean modified = false;
		for (O e : c) {
			add(index++, e, ref);
			modified = true;
		}
		return modified;
	}

	@Override
  public boolean add(final O obj) {

	final O ref = createRef();
	final boolean ret = add(obj, ref);
	releaseRef(ref);

	return ret;
  }

  public boolean add(final O obj, final O ref) {

	final int size = size();
	ensureCapacity(size + 1);
	setSize(size + 1);

	setRefAt(ref, size);
	ref.set(obj);

	return true;
  }

  @Override
  public void add(final int index, final O obj) {

	final O ref = createRefAt(index);
    add(index, obj ,ref);
	releaseRef(ref);
  }

	public void add(final int index, final O obj, final O ref) {
		final int size = size();
		ensureCapacity(size + 1);
		setSize(size + 1);
		setRefAt(ref, index);
		if (index < size) {
			final O shift = createRefAt(index + 1);
			shift.access.copyFrom(ref.access, elementSizeInBytes() * (size - index));
			releaseRef(shift);
		}
		ref.set(obj);
	}

  @Override
  public RefIterator<O> iterator() {

	return new RefIterator<O>() {

	  private O ref = createRef();

	  private int i = 0;

	  @Override
	  public boolean hasNext() {

		if (i < size())
		  return true;
		else {
		  release();
		  return false;
		}
	  }

	  @Override
	  public O next() {

		return get(i++, ref);
	  }

	  @Override
	  public void release() {

		if (ref != null) {
		  releaseRef(ref);
		  ref = null;
		}
	  }

	  @Override
	  public void reset() {

		if (ref == null)
		  ref = createRef();
		i = 0;
	  }
	};
  }

  @Override
  public boolean equals(final Object o) {

	if (o == this)
	  return true;
	if (!(o instanceof List))
	  return false;

	if (o instanceof RefList) {
	  final RefIterator<O> e1 = iterator();
	  final RefIterator<?> e2 = ((RefList<?>)o).iterator();
	  while (e1.hasNext() && e2.hasNext()) {
		final O o1 = e1.next();
		final Object o2 = e2.next();
		if (!o1.equals(o2)) {
		  e1.release();
		  e2.release();
		  return false;
		}
	  }
	  return !(e1.hasNext() || e2.hasNext());
	}

	final ListIterator<O> e1 = listIterator();
	final ListIterator<?> e2 = ((List<?>)o).listIterator();
	while (e1.hasNext() && e2.hasNext()) {
	  final O o1 = e1.next();
	  final Object o2 = e2.next();
	  if (!(o1 == null ? o2 == null : o1.equals(o2)))
		return false;
	}
	return !(e1.hasNext() || e2.hasNext());
  }

  @Override
  public void sort(final Comparator<? super O> comparator) {

	if (size() < 2)
	  return;
	final O r1 = createRef();
	final O r2 = createRef();
	final O r3 = createRef();
	quicksort(0, size() - 1, comparator, r1, r2, r3);
	releaseRef(r3);
	releaseRef(r2);
	releaseRef(r1);
  }

  /**
   * Limit list of list to sizeLimit if sizeLimit is smaller than current
   * size.
   *
   * @param sizeLimit TODO is this the best way to do this?
   */
  public void limitSize(final int sizeLimit) {

	if (size() > sizeLimit)
		setSize(sizeLimit);
  }

  private void quicksort(final int low, final int high, final Comparator<? super O> comparator, final O tmpRef1, final O tmpRef2, final O tmpRef3) {

	int pivotpos = (low + high) / 2;
	final O pivot = get(pivotpos, tmpRef1);

	int i = low;
	int j = high;

	do {
	  while (comparator.compare(get(i, tmpRef2), pivot) < 0)
		i++;
	  while (comparator.compare(pivot, get(j, tmpRef3)) < 0)
		j--;
	  if (i <= j) {
		get(i, tmpRef2).access.swapWith(get(j, tmpRef3).access, elementSizeInBytes());
		if (pivotpos == i) {
		  pivotpos = j;
		  get(pivotpos, pivot);
		} else if (pivotpos == j) {
		  pivotpos = i;
		  get(pivotpos, pivot);
		}
		i++;
		j--;
	  }
	}
	while (i <= j);

	if (low < j)
	  quicksort(low, j, comparator, tmpRef1, tmpRef2, tmpRef3);
	if (i < high)
	  quicksort(i, high, comparator, tmpRef1, tmpRef2, tmpRef3);
  }

  @Override
  public Stream<O> stream() {

	throw new UnsupportedOperationException("Streams are not compatible with " + getClass().getName() + " because its iterator reuses the same reference.");
  }

  @Override
  public Stream<O> parallelStream() {

	throw new UnsupportedOperationException("Streams are not compatible with " + getClass().getName() + " because its iterator reuses the same reference.");
  }
}
