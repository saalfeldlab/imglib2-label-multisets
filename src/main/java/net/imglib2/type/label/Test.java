package net.imglib2.type.label;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.Views;

import java.util.Random;

public class Test {

  public static void main(final String[] args) {

	final Random rng = new Random(100);
	final long[] dims = {2, 3, 4};
	final RandomAccessibleInterval<LongType> img = ArrayImgs.longs(dims);
	Views.flatIterable(img).forEach(p -> p.set(rng.nextInt(3)));
	final LabelMultisetType type = FromIntegerTypeConverter.getAppropriateType();
	final RandomAccessibleInterval<LabelMultisetType> converted = Converters.convert(img, new FromIntegerTypeConverter<>(), type);
	for (final LabelMultisetType c : Views.flatIterable(converted)) {
	  System.out.println(c + " " + c.argMax());
	}
	final int numElements = (int)Views.flatIterable(converted).size();
	final byte[] bytes = LabelUtils.serializeLabelMultisetTypes(
			Views.flatIterable(converted),
			numElements);

	final VolatileLabelMultisetArray arr = LabelUtils.fromBytes(bytes, numElements);
	final ArrayImg<LabelMultisetType, VolatileLabelMultisetArray> lmtImg = new ArrayImg<>(
			arr,
			dims,
			new LabelMultisetType().getEntitiesPerPixel());
	lmtImg.setLinkedType(new LabelMultisetType(lmtImg));
	System.out.println("Converted");
	for (final LabelMultisetType lmt : lmtImg) {
	  System.out.println(lmt + " " + lmt.argMax());
	}

  }

}
