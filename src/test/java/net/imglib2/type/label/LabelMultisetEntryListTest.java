package net.imglib2.type.label;

import com.google.common.collect.Streams;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class LabelMultisetEntryListTest {

	@Test
	public void sizeCheck() {

		LabelMultisetEntryList lmel = new LabelMultisetEntryList();
		assertEquals(0, lmel.size());

		LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(0);
		lmel = new LabelMultisetEntryList(listData, 0);
		assertEquals(0, lmel.size());

		listData = LongMappedAccessData.factory.createStorage(16);
		lmel = new LabelMultisetEntryList(listData, 0);
		assertEquals(0, lmel.size());

		listData = LongMappedAccessData.factory.createStorage(16);
		lmel = new LabelMultisetEntryList(listData, 0);
		lmel.add(new LabelMultisetEntry(1, 10));
		assertEquals(1, lmel.size());

		listData = LongMappedAccessData.factory.createStorage(0);
		lmel = new LabelMultisetEntryList(listData, 0);
		lmel.add(new LabelMultisetEntry(1, 10));
		lmel.add(new LabelMultisetEntry(1, 10));
		lmel.add(new LabelMultisetEntry(2, 10));
		assertEquals(2, lmel.size());
	}

}
