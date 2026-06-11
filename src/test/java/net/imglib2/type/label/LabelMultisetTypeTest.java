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

public class LabelMultisetTypeTest {

	private static final Random rnd = new Random(10);

	@Test
	public void addEntries() {

		final LabelMultisetType lmt = new LabelMultisetType();
		lmt.add(1, 1);
		lmt.add(2, 1);
		lmt.add(3, 1);
		lmt.add(4, 1);
		assertEquals(4, lmt.size());

		Streams.forEachPair(
				lmt.entrySet().stream(),
				Stream.of(
						new LabelMultisetEntry(1, 1),
						new LabelMultisetEntry(2, 1),
						new LabelMultisetEntry(3, 1),
						new LabelMultisetEntry(4, 1)
				),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);
	}

	@Test
	public void setEntries() {

		final LabelMultisetType lmt = new LabelMultisetType();
		lmt.add(1, 1);
		lmt.add(2, 1);
		lmt.add(3, 1);
		lmt.set(4, 1);
		assertEquals(1, lmt.size());

		Streams.forEachPair(
				lmt.entrySet().stream(),
				Stream.of(
						new LabelMultisetEntry(4, 1)
				),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);
	}

	@Test
	public void addType() {

		final LabelMultisetType lmt = new LabelMultisetType();
		lmt.add(1, 1);
		lmt.add(2, 1);
		lmt.add(3, 1);
		lmt.add(4, 1);
		final LabelMultisetType lmt2 = new LabelMultisetType();
		lmt2.add(1, 1);
		lmt2.add(2, 1);
		lmt2.add(3, 1);
		lmt2.add(4, 1);

		assertEquals(4, lmt.size());
		lmt.add(lmt2);
		assertEquals("Total count of all entries is 8", 8, lmt.size());

		Streams.forEachPair(
				lmt2.entrySet().stream(),
				Stream.of(
						new LabelMultisetEntry(1, 1),
						new LabelMultisetEntry(2, 1),
						new LabelMultisetEntry(3, 1),
						new LabelMultisetEntry(4, 1)
				),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);

		Streams.forEachPair(
				lmt.entrySet().stream(),
				Stream.of(
						new LabelMultisetEntry(1, 2),
						new LabelMultisetEntry(2, 2),
						new LabelMultisetEntry(3, 2),
						new LabelMultisetEntry(4, 2)
				),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);
	}

	@Test
	public void setType() {

		final LabelMultisetType lmt = new LabelMultisetType();
		final LabelMultisetType lmt2 = new LabelMultisetType();
		lmt2.add(1, 1);
		lmt2.add(2, 1);
		lmt2.add(3, 1);
		lmt2.add(4, 1);

		lmt.set(lmt2);
		Streams.forEachPair(
				lmt.entrySet().stream(),
				Stream.of(
						new LabelMultisetEntry(1, 1),
						new LabelMultisetEntry(2, 1),
						new LabelMultisetEntry(3, 1),
						new LabelMultisetEntry(4, 1)
				),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);

		Streams.forEachPair(
				lmt.entrySet().stream(),
				lmt2.entrySet().stream(),
				(actual, expected) -> {
					assertEquals(expected, actual);
				}
		);
	}

	@Test
	public void addAll() {

		final LabelMultisetEntry lme1 = new LabelMultisetEntry(1, 1);
		final LabelMultisetEntry lme2 = new LabelMultisetEntry(2, 1);
		final LabelMultisetEntry lme3 = new LabelMultisetEntry(3, 1);
		final LabelMultisetEntry lme4 = new LabelMultisetEntry(4, 1);

		final LabelMultisetEntryList lmel = new LabelMultisetEntryList(4);
		lmel.add(lme1);
		lmel.add(lme2);
		lmel.add(lme3);
		lmel.add(lme4);

		final List<LabelMultisetEntry> list = new ArrayList<>();
		list.add(lme1);
		list.add(lme2);
		list.add(lme3);
		list.add(lme4);

		List<List<LabelMultisetEntry>> lists = new ArrayList<>();
		lists.add(lmel);
		lists.add(list);
		for (List<LabelMultisetEntry> entries : lists) {
			final LabelMultisetType lmt = new LabelMultisetType();
			lmt.addAll(entries);
			assertEquals(4, lmt.size());

			Streams.forEachPair(
					lmt.entrySet().stream(),
					Stream.of(
							new LabelMultisetEntry(1, 1),
							new LabelMultisetEntry(2, 1),
							new LabelMultisetEntry(3, 1),
							new LabelMultisetEntry(4, 1)
					),
					(actual, expected) -> {
						assertEquals(expected, actual);
					}
			);
		}
	}

	@Test
	public void setAll() {

		final LabelMultisetEntry lme1 = new LabelMultisetEntry(1, 1);
		final LabelMultisetEntry lme2 = new LabelMultisetEntry(2, 1);
		final LabelMultisetEntry lme3 = new LabelMultisetEntry(3, 1);
		final LabelMultisetEntry lme4 = new LabelMultisetEntry(4, 1);

		final LabelMultisetEntryList lmel = new LabelMultisetEntryList(4);
		lmel.add(lme1);
		lmel.add(lme2);
		lmel.add(lme3);
		lmel.add(lme4);

		final List<LabelMultisetEntry> list = new ArrayList<>();
		list.add(lme1);
		list.add(lme2);
		list.add(lme3);
		list.add(lme4);

		final LabelMultisetType lmt = new LabelMultisetType();
		List<List<LabelMultisetEntry>> lists = new ArrayList<>();
		lists.add(lmel);
		lists.add(list);
		for (List<LabelMultisetEntry> entries : lists) {
			lmt.set(entries);
			assertEquals(4, lmt.size());

			Streams.forEachPair(
					lmt.entrySet().stream(),
					Stream.of(
							new LabelMultisetEntry(1, 1),
							new LabelMultisetEntry(2, 1),
							new LabelMultisetEntry(3, 1),
							new LabelMultisetEntry(4, 1)
					),
					(actual, expected) -> assertEquals(expected, actual)
			);
		}
	}

	@Test
	public void testClearEntries() {

		final LabelMultisetType lmtEmpty = new LabelMultisetType();
		assertEquals(Label.INVALID, lmtEmpty.argMax());
		Assert.assertTrue(lmtEmpty.isEmpty());

		final LabelMultisetEntry entry = new LabelMultisetEntry(1, 1);
		lmtEmpty.add(entry);
		assertEquals(lmtEmpty.entrySet().iterator().next(), entry);

		lmtEmpty.clear();
		assertEquals(Label.INVALID, lmtEmpty.argMax());
		Assert.assertTrue(lmtEmpty.isEmpty());
	}

	@Test
	public void testInitializationEmpty() {

		final LabelMultisetType lmtEmpty = new LabelMultisetType();
		assertEquals(Label.INVALID, lmtEmpty.argMax());
		Assert.assertTrue(lmtEmpty.isEmpty());
	}

	@Test
	public void testInitializationSingleEntry() {

		final LabelMultisetType lmtSingleEntry = new LabelMultisetType(new LabelMultisetEntry(5, 2));
		assertEquals(5, lmtSingleEntry.argMax());
		assertEquals(2, lmtSingleEntry.size());
		assertEquals(1, lmtSingleEntry.entrySet().size());
	}

	@Test
	public void testInitializationMultipleEntries() {

		final LabelMultisetEntryList multipleEntries = new LabelMultisetEntryList();
		final Set<Long> uniqueEntries = new HashSet<>();
		int totalCount = 0;
		for (int i = 0; i < rnd.nextInt(20); ++i) {
			final LabelMultisetEntry entry = new LabelMultisetEntry(rnd.nextInt(1000), rnd.nextInt(10));
			uniqueEntries.add(entry.getId());
			totalCount += entry.getCount();
			multipleEntries.add(entry);
			assertEquals("Sum of all counts", totalCount, multipleEntries.multisetSize());
			assertEquals("Unique entry IDs", uniqueEntries.size(), multipleEntries.size());
		}
		final LabelMultisetType lmtMultipleEntries = new LabelMultisetType(multipleEntries);
		assertEquals(uniqueEntries.size(), lmtMultipleEntries.entrySet().size());
		assertEquals(totalCount, lmtMultipleEntries.size());
		final Iterator<? extends LabelMultisetEntry> itExpected = multipleEntries.iterator();
		final Iterator<LabelMultisetType.Entry<Label>> itActual = lmtMultipleEntries.entrySet().iterator();
		while (itExpected.hasNext() || itActual.hasNext()) {
			final LabelMultisetEntry entryExpected = itExpected.next();
			final LabelMultisetType.Entry<Label> entryActual = itActual.next();
			assertEquals(entryExpected.getElement().id(), entryActual.getElement().id());
			assertEquals(entryExpected.getCount(), entryActual.getCount());
		}
	}

	@Test
	public void testArgMax() {

		final LabelMultisetEntryList entries = new LabelMultisetEntryList();
		entries.add(new LabelMultisetEntry(2, 13));
		entries.add(new LabelMultisetEntry(3, 15));
		entries.add(new LabelMultisetEntry(4, 11));
		entries.add(new LabelMultisetEntry(1, 14));
		assertEquals(3, LabelUtils.getArgMax(entries));
		final LabelMultisetType lmt = new LabelMultisetType(entries);
		assertEquals(3, lmt.argMax());
		entries.sortByCount();
		assertEquals(3, entries.get(entries.size() - 1).getId());
	}
}
