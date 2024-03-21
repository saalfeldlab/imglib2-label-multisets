package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LabelMultisetTypeDownscalerTest {

	private static final long[] DIMENSIONS = new long[]{80, 64, 64};
	public static final int DOWNSCALED_NUM_ELEMENTS = 40960;
	public static final String DOWNSCALE_TEST_SERIALIZED_BYTES = "src/test/resources/downscaleTestSerializedBytes";
	public static final String DOWNSCALE_TEST_N5 = "src/test/resources/downscaleTest.n5";
	public static final String DATASET = "sourceImg";

	private static RandomAccessibleInterval<LabelMultisetType> sourceImg;
	private static byte[] expectedDownScaledBytes;
	private static VolatileLabelMultisetArray expectedDownScaledVlma;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		N5FSReader n5Reader = new N5FSReader(DOWNSCALE_TEST_N5);
		/* Load expected data*/
		sourceImg = N5Utils.open(n5Reader, DATASET);
		expectedDownScaledBytes = Files.readAllBytes(Paths.get(DOWNSCALE_TEST_SERIALIZED_BYTES));
		expectedDownScaledVlma = LabelUtils.fromBytes(expectedDownScaledBytes, DOWNSCALED_NUM_ELEMENTS);
		final byte[] sanityCheck = new byte[expectedDownScaledBytes.length];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(expectedDownScaledVlma, sanityCheck);
		assert Arrays.equals(expectedDownScaledBytes, sanityCheck);
	}

	@Test
	public void testDownscaler() {

		VolatileLabelMultisetArray actualVlma = LabelMultisetTypeDownscaler.createDownscaledCell(sourceImg, new int[]{2, 2, 2}, -1);
		VolatileLabelMultisetArrayTest.assertVolatileLabelMultisetArrayEquality(expectedDownScaledVlma, actualVlma);
	}

	@Test
	public void testSerialization() {

		VolatileLabelMultisetArray test = LabelMultisetTypeDownscaler.createDownscaledCell(sourceImg, new int[]{2, 2, 2}, -1);
		final byte[] serializedBytes = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(test)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(test, serializedBytes);

		final VolatileLabelMultisetArray deserialized = LabelUtils.fromBytes(serializedBytes, test.getCurrentStorageArray().length);
		VolatileLabelMultisetArrayTest.assertVolatileLabelMultisetArrayEquality(test, deserialized);
	}

	private static RandomAccessibleInterval<LabelMultisetType> generateRandomLabelMultisetImg() {

		final Random rnd = new Random(10);

		final int numElements = (int) Intervals.numElements(DIMENSIONS);
		final List<LabelMultisetType> typeElements = new ArrayList<>();
		LabelMultisetEntry obj = new LabelMultisetEntry(0, 1);
		LabelMultisetEntry tmpObj = new LabelMultisetEntry(0, 1);
		for (int i = 0; i < numElements; ++i) {
			final int numEntries = rnd.nextInt(10);
			final LabelMultisetEntryList entries = new LabelMultisetEntryList(numEntries);
			for (int j = 0; j < numEntries; ++j) {
				obj.setId(rnd.nextInt(100));
				obj.setCount(rnd.nextInt(100));
				entries.add(obj, tmpObj);
			}
			typeElements.add(new LabelMultisetType(entries));
		}
		return new ListImg<>(typeElements, DIMENSIONS);
	}

	@Test
	public void copyOverImg() {
		final LabelMultisetType atTen = sourceImg.getAt(10, 10, 10);
		final LabelMultisetType atEleven = sourceImg.getAt(11, 10, 10);
		final LabelMultisetType copy = atTen.copy();


		/* all share the same underlying access */
		assert atTen.getAccess() != copy.getAccess();
		assert atEleven.getAccess() != copy.getAccess();
		assert atEleven.getAccess() == atTen.getAccess();

		/* copy and 10 should be equal */
		assertNotEquals(atTen.index().get(), copy.index().get());
		assertEquals(atTen.entrySet(), copy.entrySet());
		/* copy and 11 should be different */
		assertNotEquals(atEleven.index().get(), copy.index().get());
		assertNotEquals(atEleven.entrySet(), copy.entrySet());

		assertNotEquals(atTen.getAccess().argMax(0), copy.getAccess().argMax(0));
		copy.getAccess().setArgMax(0, 100);
		assertEquals(100, copy.getAccess().argMax(0));
		assertNotEquals(atTen.getAccess().argMax(0), copy.getAccess().argMax(0));

		final LabelMultisetEntry entry = ((LabelMultisetEntry) copy.entrySet().iterator().next());
		entry.setCount(100);
		assertNotEquals(atTen.entrySet(), copy.entrySet());




		/* NOTE: In general its not really safe to increment the index manually.
		*   When moving the index, you also should trigger the `updateContainer` method,
		*   but to do that you need to pass in the object to update against, which
		*   is not available internally to the LabelMultisetType.
		*
		* We only get away with it here because we know the `sourceImg` the copy is over,
		* and know that the access would not change when incrementing from
		* (10, 10, 10) to (11, 10, 10)
		* */
		atTen.index().inc();
		/* we moved the index, but in this test it should not have a different access */
		assert atTen.getAccess() != copy.getAccess();
		assert atEleven.getAccess() != copy.getAccess();

		/* copy and 10 should NOT be equal now */
		assertEquals(atTen.index().get(), atEleven.index().get());
		assertEquals(atTen.entrySet(), atEleven.entrySet());
		assertEquals(atTen.getAccess().argMax(0), atEleven.getAccess().argMax(0));
		/* copy and 11 SHOULD be equal now */
		assertNotEquals(atEleven.index().get(), copy.index().get());
		assertNotEquals(atEleven.entrySet(), copy.entrySet());
	}
}
