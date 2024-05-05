package oakbot.command.effective;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

import com.github.mangstadt.sochat4j.util.Leaf;

/**
 * Tests to make sure the data in the XML file containing the Effective Java
 * items are correct.
 * @author Michael Angstadt
 */
public class EffectiveJavaXmlTest {
	private final Leaf document;

	public EffectiveJavaXmlTest() throws Exception {
		try (var in = getClass().getResourceAsStream("effective-java.xml")) {
			document = Leaf.parse(in);
		}
	}

	@Test
	public void itemNumbers() throws Exception {
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
			assertEquals("Item number missing: " + prev, prev, number);
		}
	}

	@Test
	public void pageNumbers() throws Exception {
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
	public void titles() throws Exception {
		var itemElements = document.select("/items/item");

		for (var itemElement : itemElements) {
			var titleElement = itemElement.selectFirst("title");
			assertNotNull("Missing <title> element on item " + itemElement.attribute("number") + ".", titleElement);
			assertFalse("Empty <title> element on item " + itemElement.attribute("number") + ".", titleElement.text().isEmpty());
		}
	}

	/**
	 * The "summary" element is required.
	 */
	@Test
	public void summaries() throws Exception {
		var itemElements = document.select("/items/item");

		for (var itemElement : itemElements) {
			var summaryElement = itemElement.selectFirst("summary");
			assertNotNull("Missing <summary> element on item " + itemElement.attribute("number") + ".", summaryElement);
			assertFalse("Empty <summary> element on item " + itemElement.attribute("number") + ".", summaryElement.text().isEmpty());
		}
	}
}
