package net.imglib2.type.label;

import org.junit.Test;

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
