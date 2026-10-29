package net.imglib2.type.label;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class LabelMultisetArgMaxTest {

	private static final long[] EMPTY = {};

	private static LabelMultisetEntryList list(final long... idCountPairs) {

		final LabelMultisetEntryList entries = new LabelMultisetEntryList();
		for (int i = 0; i < idCountPairs.length; i += 2)
			entries.add(new LabelMultisetEntry(idCountPairs[i], (int)idCountPairs[i + 1]));
		return entries;
	}

	/* outer array per pixel, inner array is 2D [Id,Count] */
	private static byte[] serialize(final long[][] data) {

		final List<LabelMultisetType> pixels = new ArrayList<>();
		for (final long[] pixel : data)
			pixels.add(new LabelMultisetType(list(pixel)));
		return LabelUtils.serializeLabelMultisetTypes(pixels, pixels.size());
	}

	private static long[] argMaxes(final VolatileLabelMultisetArray access, final int numPixels) {

		final long[] argMaxes = new long[numPixels];
		for (int i = 0; i < numPixels; i++)
			argMaxes[i] = access.argMax(i);
		return argMaxes;
	}

	/**
	 * The argMax is the highest count, and the lowest unsigned id on a tie. {@code sortByCount} ends with the same entry.
	 */
	@RunWith(Parameterized.class)
	public static class ArgMax {

		private static final long[] RESERVED_IDS = {Label.INVALID, Label.OUTSIDE, Label.TRANSPARENT, Label.MAX_ID};

		@Parameters(name = "{0}")
		public static Collection<Object[]> cases() {

			final List<Object[]> cases = new ArrayList<>();
			cases.add(new Object[]{"highest count", new long[]{2, 13, 3, 15, 4, 11, 1, 14}, 3L});
			cases.add(new Object[]{"tie goes to the lowest regular id", new long[]{5, 2, 3, 2, 9, 2}, 3L});
			cases.add(new Object[]{"empty list", EMPTY, Label.INVALID});
			cases.add(new Object[]{"counted INVALID", new long[]{Label.INVALID, 3}, Label.INVALID});
			cases.add(new Object[]{"counted background", new long[]{Label.BACKGROUND, 5}, Label.BACKGROUND});
			cases.add(new Object[]{"zero-count background", new long[]{Label.BACKGROUND, 0}, Label.BACKGROUND});
			cases.add(new Object[]{"tie of two reserved ids is the lowest", new long[]{Label.INVALID, 2, Label.OUTSIDE, 2}, Label.OUTSIDE});
			for (final long reserved : RESERVED_IDS) {
				cases.add(new Object[]{"regular preferred over reserved ID " + Long.toHexString(reserved) + " on a tie", new long[]{5, 2, reserved, 2}, 5L});
				cases.add(new Object[]{"regular preferred over reserved ID " + Long.toHexString(reserved) + " on a tie, reversed", new long[]{reserved, 2, 5, 2}, 5L});
			}
			return cases;
		}

		@Parameter(0)
		public String name;

		@Parameter(1)
		public long[] idCountPairs;

		@Parameter(2)
		public long expected;

		@Test
		public void getArgMax() {

			assertEquals(name, expected, LabelUtils.getArgMax(list(idCountPairs)));
		}

		@Test
		public void sortByCountEndsWithTheArgMax() {

			final LabelMultisetEntryList entries = list(idCountPairs);
			if (entries.isEmpty())
				return;
			entries.sortByCount();
			assertEquals(name, expected, entries.get(entries.size() - 1).getId());
		}
	}

	public static class FromBytes {

		private static final long[][] PIXELS = {EMPTY, {Label.BACKGROUND, 0}, {Label.BACKGROUND, 5}, {Label.INVALID, 3}, {7, 2}};

		@Test
		public void emptyListUsesTheGivenEmptyArgMax() {

			final byte[] bytes = serialize(PIXELS);
			assertArrayEquals(
					new long[]{Label.BACKGROUND, Label.BACKGROUND, Label.BACKGROUND, Label.INVALID, 7},
					argMaxes(LabelUtils.fromBytes(bytes, PIXELS.length, Label.BACKGROUND), PIXELS.length));
			assertArrayEquals(
					new long[]{Label.INVALID, Label.BACKGROUND, Label.BACKGROUND, Label.INVALID, 7},
					argMaxes(LabelUtils.fromBytes(bytes, PIXELS.length, Label.INVALID), PIXELS.length));
		}

		@Test
		public void defaultEmptyArgMaxIsInvalid() {

			final byte[] bytes = serialize(PIXELS);
			assertArrayEquals(
					argMaxes(LabelUtils.fromBytes(bytes, PIXELS.length, Label.INVALID), PIXELS.length),
					argMaxes(LabelUtils.fromBytes(bytes, PIXELS.length), PIXELS.length));
		}
	}
}
