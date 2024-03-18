package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SerializationTest {

	@Test
	public void randomSingleEntryImageSerializationTest() {

		final Random rng = new Random();
		final int dim = 32;
		final long[] dims = {dim, dim, dim};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final int numElements = (int)Views.flatIterable(img).size();
		Views.flatIterable(img).forEach(p -> p.set(rng.nextInt((int)Math.pow(dim, 3.0) / 2)));
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final byte[] serializedOut = serialize(
				converted,
				numElements);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);

	}

	@Test
	public void singleIdImageSerializationTest() {

		final long[] dims = {4, 4, 4};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final int numElements = (int)Views.flatIterable(converted).size();
		final byte[] serializedOut = serialize(
				converted,
				numElements);

		final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
		final byte[] serializedOutAfterDeserialication = new byte[LabelMultisetTypeDownscaler.getSerializedVolatileLabelMultisetArraySize(arr)];
		LabelMultisetTypeDownscaler.serializeVolatileLabelMultisetArray(arr, serializedOutAfterDeserialication);

		Assert.assertArrayEquals("Serialized bytes differed after deserialization", serializedOut, serializedOutAfterDeserialication);

	}

	@Test
	public void randomMultipleEntryImageSerializationTest() throws IOException {

		N5Writer n5 = new N5FSWriter(Files.createTempDirectory("n5-test").toString());
		try {
			final long[] dims = {10, 20, 30};
			final Random rand = new Random();

			final int numElements = (int)Intervals.numElements(dims);
			final List<LabelMultisetType> typeElements = new ArrayList<>();
			for (int i = 0; i < numElements; ++i) {
				final int numEntries = rand.nextInt(10);
				final LabelMultisetEntryList entries = new LabelMultisetEntryList(numEntries);
				for (int j = 0; j < numEntries; ++j) {
					final int id = rand.nextInt(10_000);
					final int count = rand.nextInt(100);
					entries.add(new LabelMultisetEntry(id, count));
				}
				typeElements.add(new LabelMultisetType(entries));
			}
			final ListImg<LabelMultisetType> originalImg = new ListImg<>(typeElements, dims);

			final byte[] serializedOut = serialize(originalImg, numElements);

			final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, numElements);
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> deserializedImg = new ArrayImg<>(arr, dims, new Fraction());
			deserializedImg.setLinkedType(new LabelMultisetType(deserializedImg));

			Assert.assertTrue(Intervals.equals(originalImg, deserializedImg));

			final Iterator<LabelMultisetType> iterOverOriginalImg = Views.flatIterable(originalImg).iterator();
			final Iterator<LabelMultisetType> iterOverDeserializedImg = Views.flatIterable(deserializedImg).iterator();
			int i = -1;
			while (iterOverOriginalImg.hasNext() || iterOverDeserializedImg.hasNext()) {
				i++;
				final LabelMultisetType original = iterOverOriginalImg.next();
				final LabelMultisetType deserialized = iterOverDeserializedImg.next();
				assertEquals(deserialized.argMax(), original.argMax());
				assertEquals(deserialized.size(), original.size());
				assertEquals(deserialized.entrySet().size(), original.entrySet().size());
				final Iterator<LabelMultisetType.Entry<Label>> iterOverDeserializedEntries = original.entrySet().iterator();
				final Iterator<LabelMultisetType.Entry<Label>> iterOverOriginalEntries = deserialized.entrySet().iterator();
				while (iterOverDeserializedEntries.hasNext() || iterOverOriginalEntries.hasNext()) {
					final LabelMultisetType.Entry<Label> expectedEntry = iterOverDeserializedEntries.next();
					final LabelMultisetType.Entry<Label> actualEntry = iterOverOriginalEntries.next();
					assertEquals(expectedEntry.getElement().id(), actualEntry.getElement().id());
					assertEquals(actualEntry.getCount(), expectedEntry.getCount());
				}
			}

		} finally {
			n5.remove("");
			n5.close();
		}
	}

	@Test
	public void randomMultipleEntrySingleTypeSerializationTest() throws IOException {

		N5Writer n5 = new N5FSWriter(Files.createTempDirectory("n5-test").toString());
		try {
			final long[] dims = {1};
			final int[] blockSize = {1};

			final Random rand = new Random();

			final int numElements = (int)Intervals.numElements(dims);
			final List<LabelMultisetType> typeElements = new ArrayList<>();
			for (int i = 0; i < numElements; ++i) {
				final int numEntries = rand.nextInt(10);
				final LabelMultisetEntryList entries = new LabelMultisetEntryList(numEntries);
				for (int j = 0; j < numEntries; ++j) {
					final int id = rand.nextInt(4);
					final int count = rand.nextInt(10);
					entries.add(new LabelMultisetEntry(id, count));
				}
				typeElements.add(new LabelMultisetType(entries));
			}
			final ListImg<LabelMultisetType> originalImg = new ListImg<>(typeElements, dims);

			final byte[] serializedOut = serialize(originalImg, 2);

			final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(serializedOut, 1);
			final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> deserializedImg = new ArrayImg<>(arr, dims, new Fraction());
			deserializedImg.setLinkedType(new LabelMultisetType(deserializedImg));

			final byte[] serializedOut2 = serialize(deserializedImg, (int)Intervals.numElements(deserializedImg));
			assertArrayEquals(serializedOut, serializedOut2);

			Assert.assertTrue(Intervals.equals(originalImg, deserializedImg));
			final LabelMultisetType original = originalImg.firstElement();
			final LabelMultisetType deserialized = deserializedImg.firstElement();
			assertEquals(deserialized.argMax(), original.argMax());
			assertEquals(deserialized.size(), original.size());
			assertEquals(deserialized.entrySet().size(), original.entrySet().size());
			final Iterator<LabelMultisetType.Entry<Label>> iterOverDeserializedEntries = original.entrySet().iterator();
			final Iterator<LabelMultisetType.Entry<Label>> iterOverOriginalEntries = deserialized.entrySet().iterator();
			while (iterOverDeserializedEntries.hasNext() || iterOverOriginalEntries.hasNext()) {
				final LabelMultisetType.Entry<Label> expectedEntry = iterOverDeserializedEntries.next();
				final LabelMultisetType.Entry<Label> actualEntry = iterOverOriginalEntries.next();
				assertEquals(expectedEntry.getElement().id(), actualEntry.getElement().id());
				assertEquals(actualEntry.getCount(), expectedEntry.getCount());
			}

		} finally {
			n5.remove("");
			n5.close();
		}
	}

	private static byte[] serialize(RandomAccessibleInterval<LabelMultisetType> img, int numElements) {

		return LabelUtils.serializeLabelMultisetTypes(
				Views.flatIterable(img),
				numElements);
	}

	@Test
	public void invalidIdSerializationTest() {

		final long[] dims = {4, 4, 4};
		final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
		final LabelMultisetType type = LabelMultisetType.singleEntryWithSingleOccurrence();
		Views.flatIterable(img).forEach(p -> p.set(Label.INVALID));
		final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert2(img, new FromIntegerTypeConverter<>(), () -> type);

		final int numElements = (int)Views.flatIterable(converted).size();
		final byte[] serializedOut = serialize(converted, numElements);

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

		assertEquals(1, one.getIntegerLong());
		assertEquals(2, two.getIntegerLong());

	}

}
