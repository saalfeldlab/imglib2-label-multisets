package net.imglib2.type.label;

public interface Label
{
	static public long BACKGROUND = 0L;

	static public long TRANSPARENT = 0xffffffffffffffffL; // -1L or uint64.MAX_VALUE

	static public long INVALID = 0xfffffffffffffffeL; // -2L or uint64.MAX_VALUE - 1

	static public long OUTSIDE = 0xfffffffffffffffdL; // -3L or uint64.MAX_VALUE - 2

	static public long MAX_ID = 0xfffffffffffffffcL; // -4L or uint64.MAX_VALUE - 3

	static public boolean regular( final long id )
	{
		return max( id, Label.MAX_ID ) == Label.MAX_ID;
	}

	/**
	 * Max of two uint64 passed as long.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	static public long max( final long a, final long b )
	{
		return a + Long.MIN_VALUE > b + Long.MIN_VALUE ? a : b;
	}

	public long id();
}
