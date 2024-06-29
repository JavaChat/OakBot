package oakbot.command.effective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.util.Leaf;

/**
 * Tests to make sure the data in the XML file containing the Effective Java
 * items are correct.
 * @author Michael Angstadt
 */
class EffectiveJavaXmlTest {
	private final Leaf document;

	public EffectiveJavaXmlTest() throws Exception {
		try (var in = getClass().getResourceAsStream("effective-java.xml")) {
			document = Leaf.parse(in);
		}
	}

	@Test
	void itemNumbers() throws Exception {
		var itemElements = document.select("/items/item");

		var numbers = new HashSet<Integer>();
		for (var itemElement : itemElements) {
			int number = Integer.parseInt(itemElement.attribute("number"));
			if (number <= 0) {
				fail("Invalid item number: " + number);
			}
			if (!numbers.add(number)) {
				fail("Duplicate item number: " + number);
			}
		}

		/*
		 * Are any item numbers missing?
		 */
		var sortedNumbers = new ArrayList<Integer>(numbers);
		sortedNumbers.sort(null);
		var prev = 0;
		for (int number : sortedNumbers) {
			prev++;
			final var prev2 = prev;
			assertEquals(prev, number, () -> "Item number missing: " + prev2);
		}
	}

	@Test
	void pageNumbers() throws Exception {
		var itemElements = document.select("/items/item");

		var pageNumbers = new HashMap<Integer, Integer>();
		for (var itemElement : itemElements) {
			int number = Integer.parseInt(itemElement.attribute("number"));
			int pageNumber = Integer.parseInt(itemElement.attribute("page"));
			if (pageNumber <= 0) {
				fail("Invalid page number: " + pageNumber);
			}
			pageNumbers.put(number, pageNumber);
		}

		/*
		 * Is each item's page number greater than the previous item's page
		 * number?
		 */
		var prev = 0;
		for (var i = 1; true; i++) {
			var page = pageNumbers.get(i);
			if (page == null) {
				break;
			}

			assertTrue(page > prev);
			prev = page;
		}
	}

	/**
	 * The "title" element is required.
	 */
	@Test
	void titles() {
		var itemElements = document.select("/items/item");

		for (var itemElement : itemElements) {
			var titleElement = itemElement.selectFirst("title");
			assertNotNull(titleElement, () -> "Missing <title> element on item " + itemElement.attribute("number") + ".");
			assertFalse(titleElement.text().isEmpty(), () -> "Empty <title> element on item " + itemElement.attribute("number") + ".");
		}
	}

	/**
	 * The "summary" element is required.
	 */
	@Test
	void summaries() {
		var itemElements = document.select("/items/item");

		for (var itemElement : itemElements) {
			var summaryElement = itemElement.selectFirst("summary");
			assertNotNull(summaryElement, () -> "Missing <summary> element on item " + itemElement.attribute("number") + ".");
			assertFalse(summaryElement.text().isEmpty(), () -> "Empty <summary> element on item " + itemElement.attribute("number") + ".");
		}
	}
}
