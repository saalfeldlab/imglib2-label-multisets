package net.imglib2.type.label;

import java.util.Collection;
import java.util.Set;

/**
 * minimal subset of Guava Multiset interface.
 */
public interface Multiset<E> extends Collection<E> {

	int count(E element);

	Set<LabelMultisetType.Entry<E>> entrySet();
}
