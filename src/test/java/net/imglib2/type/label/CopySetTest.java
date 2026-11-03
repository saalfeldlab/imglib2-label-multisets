package net.imglib2.type.label;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static net.imglib2.type.label.ArgMaxTest.EMPTY;
import static net.imglib2.type.label.ArgMaxTest.list;
import static net.imglib2.type.label.ArgMaxTest.serialize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class CopySetTest {

	private static final long[][] PIXELS = {
			EMPTY,
			{Label.BACKGROUND, 0},
			{Label.BACKGROUND, 5},
			{Label.INVALID, 3},
			{7, 2},
			{7, 2},
			{1, 4, 2, 4, 3, 1},
			{5, 2, Label.INVALID, 2},
			{9, 1, Label.OUTSIDE, 1, Label.MAX_ID, 1}
	};

	private static final long[] EMPTY_ARG_MAXES = {Label.BACKGROUND, Label.INVALID};

	public static ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> imgOf(final long[][] pixels, final long emptyArgMax) {

		return imgOf(LabelUtils.fromBytes(serialize(pixels), pixels.length, emptyArgMax));
	}

	public static ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> imgOf(final VolatileLabelMultisetArray access) {

		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = new ArrayImg<>(access, new long[]{access.getArrayLength()}, new LabelMultisetType().getEntitiesPerPixel());
		img.setLinkedType(new LabelMultisetType(img));
		return img;
	}

	public static LabelMultisetType lmtAsVariable(final long[] pixel, final long emptyArgMax) {

		final LabelMultisetType variable = new LabelMultisetType(list(pixel));
		if (pixel.length == 0)
			variable.updateArgMax(emptyArgMax);
		return variable;
	}

	public static String describe(final LabelMultisetType type) {

		final StringBuilder description = new StringBuilder("argMax=").append(type.argMax()).append(':');
		for (final LabelMultisetType.Entry<Label> entry : type.entrySet())
			description.append(' ').append(entry.getElement().id()).append('x').append(entry.getCount());
		return description.toString();
	}

	private static String[] describeEach(final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img) {

		final String[] descriptions = new String[(int)img.size()];
		final ArrayRandomAccess<LabelMultisetType> imgRa = img.randomAccess();
		for (int i = 0; i < descriptions.length; i++)
			descriptions[i] = describe(imgRa.setPositionAndGet(i));
		return descriptions;
	}

	private static String describe(final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img) {

		return String.join(";", describeEach(img));
	}

	/* serialize the img's access, and read it back */
	private static String[] describeEachAfterSerializing(final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img, final long emptyArgMax) {

		final VolatileLabelMultisetArray access = img.update(null);
		final byte[] bytes = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(access)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(access, bytes);
		return describeEach(imgOf(LabelUtils.fromBytes(bytes, access.getArrayLength(), emptyArgMax)));
	}

	private static void forEachSource(final Consumer<LabelMultisetType> check) {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
			final ArrayRandomAccess<LabelMultisetType> imgRa = img.randomAccess();
			for (int i = 0; i < PIXELS.length; i++) {
				LabelMultisetType imgBackedType = imgRa.setPositionAndGet(i);
				check.accept(imgBackedType);

				LabelMultisetType variableBackedType = lmtAsVariable(PIXELS[i], emptyArgMax);
				check.accept(variableBackedType);
			}
		}
	}

	/* [Id, Count] pairs for 300 entries; the highest count is in the middle of the list */
	private static long[] manyEntries() {

		final long[] manyEntries = new long[600];
		for (int i = 0; i < 300; i++) {
			manyEntries[2 * i] = i + 1;
			manyEntries[2 * i + 1] = i % 7 + 1;
		}
		manyEntries[2 * 149 + 1] = 1_000_000;
		return manyEntries;
	}

	private static Map<String, LabelMultisetType> copyAndSet(final LabelMultisetType source) {

		final LabelMultisetType set = new LabelMultisetType(list(10, 9, 11, 9, 12, 9));
		set.set(source);
		final Map<String, LabelMultisetType> derived = new LinkedHashMap<>();
		derived.put("copy", source.copy());
		derived.put("set", set);
		return derived;
	}

	@Test
	public void matchesTheSource() {

		forEachSource(source -> {
			final String expected = describe(source);
			copyAndSet(source).forEach((name, derived) -> {
				final String message = name + " of " + expected;
				assertNotSame(message, source, derived);
				assertEquals(message, expected, describe(derived));
				assertTrue(message, source.valueEquals(derived));
				assertEquals(message, source, derived);
				assertEquals(message, 0, source.compareTo(derived));
				assertEquals(message, source.listHashCode(), derived.listHashCode());
			});
		});
	}

	@Test
	public void doesntModifySourceAfter() {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			/* img backed instances */
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
			final String imgBefore = describe(img);
			final ArrayRandomAccess<LabelMultisetType> imgRa = img.randomAccess();
			for (int i = 0; i < PIXELS.length; i++) {
				final String pixel = "pixel " + i;
				copyAndSet(imgRa.setPositionAndGet(i)).forEach((name, lmt) -> {
					lmt.add(999, 50);
					lmt.add(7, 100);
					assertEquals(pixel + " " + name + " after adds", 7, lmt.argMax());
					assertEquals(pixel + " " + name + " after adds", imgBefore, describe(img));
					lmt.clear();
					assertEquals(pixel + " " + name + " after clear", imgBefore, describe(img));
					lmt.add(5, 1);
					assertEquals(pixel + " " + name + " after clear and add", "argMax=5: 5x1", describe(lmt));
					assertEquals(pixel + " " + name + " after clear and add", imgBefore, describe(img));
				});
			}

			/* variable type instances */
			for (final long[] pixel : PIXELS) {
				final LabelMultisetType source = lmtAsVariable(pixel, emptyArgMax);
				final String sourceBefore = describe(source);
				final Map<String, LabelMultisetType> copyAndSet = copyAndSet(source);
				copyAndSet.forEach((name, lmt) -> {
					lmt.add(7, 100);
					lmt.add(999, 50);
					assertEquals(name, sourceBefore, describe(source));
				});

				final Map<String, String> before = new LinkedHashMap<>();
				copyAndSet.forEach((name, derived) -> before.put(name, describe(derived)));
				source.add(1000, 500);
				source.clear();
				copyAndSet.forEach((name, derived) -> assertEquals(name, before.get(name), describe(derived)));
			}
		}
	}

	@Test
	public void setOnImgWritesToTheImg() {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			for (int target = 0; target < PIXELS.length; target++) {
				for (int source = 0; source < PIXELS.length; source++) {
					final String pair = "target " + target + " source " + source + " emptyArgMax " + emptyArgMax;
					final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
					final String[] expected = describeEach(img);
					expected[target] = expected[source];

					/* `getAt` makes a new access each time, so these are different type instances */
					final ArrayRandomAccess<LabelMultisetType> targetRa = img.randomAccess();
					targetRa.setPositionAndGet(target).set(img.getAt(source));

					/* only the target pixel changes, including when other pixels shared its list */
					assertArrayEquals(pair, expected, describeEach(img));

					/* the type is still attached, and follows its access */
					for (int pixel = 0; pixel < PIXELS.length; pixel++)
						assertEquals(pair + " pixel " + pixel, expected[pixel], describe(targetRa.setPositionAndGet(pixel)));
				}
			}
		}
	}

	@Test
	public void setOnImgIsSerialized() {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			for (int target = 0; target < PIXELS.length; target++) {
				for (int source = 0; source < PIXELS.length; source++) {
					final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
					final String[] expected = describeEach(img);
					expected[target] = expected[source];

					img.getAt(target).set(img.getAt(source));

					assertArrayEquals("target " + target + " source " + source + " emptyArgMax " + emptyArgMax, expected, describeEachAfterSerializing(img, emptyArgMax));
				}
			}
		}
	}

	@Test
	public void repeatedSetOnImgKeepsTheListDataBounded() {

		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(new long[][]{{1, 1}, {2, 2}, {3, 3}}, Label.BACKGROUND);
		final VolatileLabelMultisetArray access = img.update(null);
		final long usedBefore = access.getListDataUsedSizeInBytes();
		final String[] expected = describeEach(img);

		final int sets = 2000;
		for (int set = 0; set < sets; set++) {
			final int source = 1 + set % 2;
			expected[0] = expected[source];
			img.getAt(0).set(img.getAt(source));
		}

		assertArrayEquals(expected, describeEach(img));
		assertArrayEquals(expected, describeEachAfterSerializing(img, Label.BACKGROUND));
		/* each set appends one small list, so growth is linear, and the spare size is a fraction of the used size */
		assertTrue(access.getListDataUsedSizeInBytes() <= usedBefore + sets * 40L);
		assertTrue(access.getListData().size() <= 2 * access.getListDataUsedSizeInBytes() + 64);
	}

	@Test
	public void setOnImgWithALongerListLeavesTheNextListAlone() {

		final long[][] pixels = {{10, 1}, {20, 1}, {30, 1, 31, 1, 32, 1}};
		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(pixels, Label.BACKGROUND);
		final String[] expected = describeEach(img);
		expected[0] = expected[2];

		img.getAt(0).set(img.getAt(2));

		assertArrayEquals(expected, describeEach(img));
	}

	@Test
	public void setOnImgHandlesManyEntries() {

		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(new long[][]{EMPTY, manyEntries(), {3, 1}}, Label.BACKGROUND);
		final String[] expected = describeEach(img);
		expected[0] = expected[1];
		expected[2] = expected[1];

		img.getAt(0).set(img.getAt(1));
		img.getAt(2).set(img.getAt(1));

		assertArrayEquals(expected, describeEach(img));
		assertArrayEquals(expected, describeEachAfterSerializing(img, Label.BACKGROUND));
	}

	@Test
	public void setOnImgLeavesPixelsSharingTheListAlone() {

		final long[][] pixels = {{7, 2}, {7, 2}, {7, 2}, {3, 1}};
		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(pixels, Label.BACKGROUND);
		final String[] expected = describeEach(img);
		expected[0] = "argMax=9: 9x2";

		img.getAt(0).set(lmtAsVariable(new long[]{9, 2}, Label.BACKGROUND));

		assertArrayEquals(expected, describeEach(img));
	}

	@Test
	public void setOnImgFromAnotherTypeOverTheSamePixelKeepsTheValue() {

		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, Label.BACKGROUND);
		final String[] expected = describeEach(img);

		/* `getAt` makes a new access each time, so these are different type instances over the same pixel */
		img.getAt(0).set(img.getAt(0));

		assertArrayEquals(expected, describeEach(img));
	}

	@Test
	public void setOnImgFromAPixelSharingTheTargetsListKeepsTheValue() {

		final long[][] pixels = {{7, 2}, {7, 2}, {3, 1}};
		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(pixels, Label.BACKGROUND);
		final String[] expected = describeEach(img);

		img.getAt(0).set(img.getAt(1));

		assertArrayEquals(expected, describeEach(img));
	}

	@Test
	public void setFromItselfKeepsTheValue() {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
			final String[] imgBefore = describeEach(img);
			final ArrayRandomAccess<LabelMultisetType> imgRa = img.randomAccess();
			for (int i = 0; i < PIXELS.length; i++) {
				final LabelMultisetType lmtOverImg = imgRa.setPositionAndGet(i);
				lmtOverImg.set(lmtOverImg);
				assertEquals("image-backed pixel " + i, imgBefore[i], describe(lmtOverImg));
				assertArrayEquals("image-backed pixel " + i, imgBefore, describeEach(img));

				/* the type is still attached, and follows its access */
				final int next = (i + 1) % PIXELS.length;
				assertEquals("image-backed pixel " + i, imgBefore[next], describe(imgRa.setPositionAndGet(next)));

				final LabelMultisetType lmtAsVariable = lmtAsVariable(PIXELS[i], emptyArgMax);
				final String variableBackedBefore = describe(lmtAsVariable);
				lmtAsVariable.set(lmtAsVariable);
				assertEquals("variable pixel " + i, variableBackedBefore, describe(lmtAsVariable));
			}
		}
	}

	@Test
	public void emptyPixelIsValid() {

		for (final long emptyArgMax : EMPTY_ARG_MAXES) {
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(PIXELS, emptyArgMax);
			final String imgBefore = describe(img);
			copyAndSet(img.getAt(0)).forEach((name, lmt) -> {
				assertEquals(name, emptyArgMax, lmt.argMax());
				assertTrue(name, lmt.isEmpty());
				lmt.add(5, 1);
				assertEquals(name, "argMax=5: 5x1", describe(lmt));
				assertEquals(name, imgBefore, describe(img));
			});
		}
	}

	@Test
	public void handlesManyEntries() {

		final long[] manyEntries = manyEntries();
		final long[][] pixels = {EMPTY, manyEntries, {3, 1}};

		final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> img = imgOf(pixels, Label.BACKGROUND);
		final LabelMultisetType[] sources = {img.getAt(1), lmtAsVariable(manyEntries, Label.BACKGROUND)};
		for (final LabelMultisetType source : sources) {
			final String expected = describe(source);
			assertEquals(150, source.argMax());
			copyAndSet(source).forEach((name, lmt) -> {
				assertEquals(name, expected, describe(lmt));
				lmt.add(1000, 5);
				assertEquals(name, expected, describe(source));
			});
		}
	}
}
