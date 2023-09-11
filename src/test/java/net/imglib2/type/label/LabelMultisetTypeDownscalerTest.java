package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

		final Random rnd = new Random();

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
}
