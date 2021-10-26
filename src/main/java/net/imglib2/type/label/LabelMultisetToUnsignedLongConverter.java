package net.imglib2.type.label;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.integer.UnsignedLongType;

public class LabelMultisetToUnsignedLongConverter implements Converter<LabelMultisetType, UnsignedLongType> {

  @Override
  public void convert(final LabelMultisetType input, final UnsignedLongType output) {

	output.set(input.argMax());
  }
}
