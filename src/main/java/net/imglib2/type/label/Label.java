package net.imglib2.type.label;

public interface Label
{
	long BACKGROUND = 0L;

	long TRANSPARENT = 0xffffffffffffffffL; // -1L or uint64.MAX_VALUE

	long INVALID = 0xfffffffffffffffeL; // -2L or uint64.MAX_VALUE - 1

	long OUTSIDE = 0xfffffffffffffffdL; // -3L or uint64.MAX_VALUE - 2

	long MAX_ID = 0xfffffffffffffffcL; // -4L or uint64.MAX_VALUE - 3

	enum ReservedValue
	{
		BACKGROUND(Label.BACKGROUND),
		TRANSPARENT(Label.TRANSPARENT),
		INVALID(Label.INVALID),
		OUTSIDE(Label.OUTSIDE),
		MAX_ID(Label.MAX_ID);

		private final long value;

		ReservedValue(long value)
		{
			this.value = value;
		}

		public long id()
		{
			return value;
		}

		public ReservedValue fromString(String str)
		{
			try {
				return ReservedValue.valueOf(str.toUpperCase());
			}
			catch (IllegalArgumentException | NullPointerException e)
			{
				return null;
			}
		}

		public ReservedValue fromId(long id)
		{
			for (ReservedValue rv : ReservedValue.values())
				if (rv.value == id)
					return rv;
			return null;
		}
	}

	static boolean regular( final long id )
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
	static long max( final long a, final long b )
	{
		return a + Long.MIN_VALUE > b + Long.MIN_VALUE ? a : b;
	}

	long id();
}
