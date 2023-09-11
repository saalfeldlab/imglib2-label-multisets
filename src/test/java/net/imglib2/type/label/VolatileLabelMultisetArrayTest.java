package net.imglib2.type.label;

import org.junit.Assert;

public class VolatileLabelMultisetArrayTest {

	public static void assertVolatileLabelMultisetArrayEquality(VolatileLabelMultisetArray first, VolatileLabelMultisetArray second) {

		Assert.assertArrayEquals(": ArgMax was not equal!", first.argMaxCopy(), second.argMaxCopy());
		/* This is necessary, since the size of the array may be larger than the actual size of the data, due to how the array is resized when more space is needed. */
		assert first.getListDataUsedSizeInBytes() == second.getListDataUsedSizeInBytes();
		for (int i = 0; i < first.getListDataUsedSizeInBytes() / Long.BYTES; i++) {
			assert first.getListData().data[i] == second.getListData().data[i];
		}
		Assert.assertArrayEquals(": List Data was not equal!", first.getCurrentStorageArray(), second.getCurrentStorageArray());
	}
}
