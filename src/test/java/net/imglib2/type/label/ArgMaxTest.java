package net.imglib2.type.label;

import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImg;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.imglib2.type.label.Label.BACKGROUND;
import static net.imglib2.type.label.Label.INVALID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class ArgMaxTest {

	public static final long[] EMPTY = {};

	public static LabelMultisetEntryList list(final long... idCountPairs) {

		final LabelMultisetEntryList entries = new LabelMultisetEntryList();
		for (int i = 0; i < idCountPairs.length; i += 2)
			entries.add(new LabelMultisetEntry(idCountPairs[i], (int)idCountPairs[i + 1]));
		return entries;
	}

	/* outer array per pixel, inner array is 2D [Id,Count] */
	public static byte[] serialize(final long[][] data) {

		final List<LabelMultisetType> pixels = new ArrayList<>();
		for (final long[] pixel : data)
			pixels.add(new LabelMultisetType(list(pixel)));
		return LabelUtils.serializeLabelMultisetTypes(pixels, pixels.size());
	}

	public static long[] argMaxes(final VolatileLabelMultisetArray access, final int numPixels) {

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

		private static final long[] RESERVED_IDS = {
				INVALID,
				Label.OUTSIDE,
				Label.TRANSPARENT,
				Label.MAX_ID
		};

		@Parameters(name = "{0}")
		public static Collection<Object[]> cases() {

			final List<Object[]> cases = new ArrayList<>();
			cases.add(new Object[]{"highest count", new long[]{2, 13, 3, 15, 4, 11, 1, 14}, 3L});
			cases.add(new Object[]{"tie goes to the lowest regular id", new long[]{5, 2, 3, 2, 9, 2}, 3L});
			cases.add(new Object[]{"empty list", EMPTY, INVALID});
			cases.add(new Object[]{"counted INVALID", new long[]{INVALID, 3}, INVALID});
			cases.add(new Object[]{"counted background", new long[]{BACKGROUND, 5}, BACKGROUND});
			cases.add(new Object[]{"zero-count background", new long[]{BACKGROUND, 0}, BACKGROUND});
			cases.add(new Object[]{"tie of two reserved ids is the lowest", new long[]{INVALID, 2, Label.OUTSIDE, 2}, Label.OUTSIDE});
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

		private static final long[][] PIXELS = {
				EMPTY,           //empty data, argmax from deserialization
				{BACKGROUND, 0}, //argmax BACKGROUND even with count 0
				{BACKGROUND, 5}, //argmax BACKGROUND
				{INVALID, 3},    //argmax INVALID
				{7, 2}           //argmax 7
		};
		private final byte[] PIXEL_BYTES = serialize(PIXELS);
		private final int NUM_PIXELS = PIXELS.length;

		private static long[] expectedArgMaxes(final long emptyArgMax) {

			long[] expected = new long[PIXELS.length];
			for (int i = 0; i < expected.length; i++) {
				expected[i] = PIXELS[i] == EMPTY ? emptyArgMax : PIXELS[i][0];
			}
			return expected;
		}

		@Test
		public void emptyListUsesTheGivenEmptyArgMax() {

			long[] emptyAsBackground = argMaxes(LabelUtils.fromBytes(PIXEL_BYTES, NUM_PIXELS, BACKGROUND), NUM_PIXELS);
			assertArrayEquals(expectedArgMaxes(BACKGROUND), emptyAsBackground);

			long[] emptyAsInvalid = argMaxes(LabelUtils.fromBytes(PIXEL_BYTES, NUM_PIXELS, INVALID), NUM_PIXELS);
			assertArrayEquals(expectedArgMaxes(INVALID), emptyAsInvalid);
		}

		@Test
		public void defaultEmptyArgMaxIsInvalid() {

			assertArrayEquals(
					argMaxes(LabelUtils.fromBytes(PIXEL_BYTES, NUM_PIXELS, INVALID), NUM_PIXELS),
					argMaxes(LabelUtils.fromBytes(PIXEL_BYTES, NUM_PIXELS), NUM_PIXELS));
		}

		@Test
		public void copyKeepsTheArgMaxAndEntries() {

			for (final long emptyArgMax : new long[]{BACKGROUND, INVALID}) {
				/* image-backed pixels, loaded with the emptyArgMax */
				final VolatileLabelMultisetArray access = LabelUtils.fromBytes(PIXEL_BYTES, NUM_PIXELS, emptyArgMax);
				final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = new ArrayImg<>(access, new long[]{NUM_PIXELS}, new LabelMultisetType().getEntitiesPerPixel());
				img.setLinkedType(new LabelMultisetType(img));

				/*img backed pixels*/
				final RandomAccess<LabelMultisetType> pixels = img.randomAccess();
				final long[] argMaxFromCopies = new long[NUM_PIXELS];
				for (int i = 0; i < NUM_PIXELS; i++) {
					final LabelMultisetType source = pixels.setPositionAndGet(i, 0);
					final LabelMultisetType copy = source.copy();
					argMaxFromCopies[i] = copy.argMax();
					final LabelMultisetType set = new LabelMultisetType();
					set.set(source);
					assertEquals("argMax of pixel " + i + " after set", source.argMax(), set.argMax());
					assertEquals("entries of pixel " + i, source.entrySet().size(), copy.entrySet().size());
					for (final LabelMultisetType.Entry<Label> entry : source.entrySet())
						assertEquals("count of " + entry.getElement().id() + " in pixel " + i, entry.getCount(), copy.count(entry.getElement().id()));
				}

				assertArrayEquals("emptyArgMax " + emptyArgMax, argMaxes(access, NUM_PIXELS), argMaxFromCopies);

				/* variable style pixels */
				for (int i = 0; i < NUM_PIXELS; i++) {
					final LabelMultisetType lmtAsVariable = new LabelMultisetType(list(PIXELS[i]));
					if (PIXELS[i].length == 0)
						lmtAsVariable.updateArgMax(emptyArgMax);
					assertEquals("lmtAsVariable pixel " + i + ", emptyArgMax " + emptyArgMax, lmtAsVariable.argMax(), lmtAsVariable.copy().argMax());
				}
			}
		}
	}
}
