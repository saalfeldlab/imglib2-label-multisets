package net.imglib2.type.label;

import java.util.Collection;
import java.util.Set;

/**
 * minimal subset of Guava Multiset interface.
 */
public interface Multiset< E > extends Collection< E >
{
	public int count( Object element );

//	public Set< E > elementSet();

	public Set< LabelMultisetType.Entry< E > > entrySet();
}
