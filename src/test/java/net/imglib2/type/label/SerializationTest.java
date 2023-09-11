package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.Views;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class SerializationTest {

	@Test
	public void serializationTest() {

		final Random rng = new Random(100);
		final long[] dims = {2, 3, 4};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		Views.flatIterable(img).forEach(p -> p.set(rng.nextInt(3)));
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert(img, new FromIntegerTypeConverter<>(), type);

		final int numElements = (int) Views.flatIterable(converted).size();
		final byte[] serializedOut = LabelUtils.serializeLabelMultisetTypes(
				Views.flatIterable(converted),
				numElements);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);

	}

}
