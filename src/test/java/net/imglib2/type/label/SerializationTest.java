package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.Views;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;


public class SerializationTest {

	@Test
	public void randomSerializationTest() {

		final Random rng = new Random();
		final int dim = 32;
		final long[] dims = {dim, dim, dim};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final int numElements = (int) Views.flatIterable(img).size();
		Views.flatIterable(img).forEach(p -> p.set(rng.nextInt((int) Math.pow(dim, 3.0)/2)));
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final byte[] serializedOut = LabelUtils.serializeLabelMultisetTypes(
				Views.flatIterable(converted),
				numElements);

		final byte[] serializedOutOld = LabelUtils.serializeLabelMultisetTypesOld(
				Views.flatIterable(converted),
				numElements);

		System.out.println(serializedOut.length + "\t" + serializedOutOld.length);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final VolatileLabelMultisetArray arrOld = LabelUtils.fromBytes(serializedOutOld, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);

	}


	@Test
	public void singleIdSerializationTest() {

		final long[] dims = {4, 4, 4};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final int numElements = (int) Views.flatIterable(converted).size();
		final byte[] serializedOut = LabelUtils.serializeLabelMultisetTypes(
				Views.flatIterable(converted),
				numElements);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);

	}

	@Test
	public void invalidIdSerializationTest() {

		final long[] dims = {4, 4, 4};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		Views.flatIterable(img).forEach(p -> p.set(Label.INVALID));
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final int numElements = (int) Views.flatIterable(converted).size();
		final byte[] serializedOut = LabelUtils.serializeLabelMultisetTypes(
				Views.flatIterable(converted),
				numElements);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);
	}

	@Test
	public void copyTest() {

		final FromIntegerTypeConverter<IntType> intToLmt = new FromIntegerTypeConverter<>();

		final LabelMultisetType one = LabelMultisetType.singleEntryWithSingleOccurrence();
		intToLmt.convert(new IntType(1), one);

		final LabelMultisetType two = one.copy();
		intToLmt.convert(new IntType(2), two);

		Assert.assertEquals(1, one.getIntegerLong());
		Assert.assertEquals(2, two.getIntegerLong());

	}

}
