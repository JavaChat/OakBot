package oakbot.command.effective;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

import oakbot.util.Leaf;

/**
 * Tests to make sure the data in the XML file containing the Effective Java
 * items are correct.
 * @author Michael Angstadt
 */
public class EffectiveJavaXmlTest {
	private final Leaf document;

	public EffectiveJavaXmlTest() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("effective-java.xml")) {
			document = new Leaf(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in));
		}
	}

	@Test
	public void itemNumbers() throws Exception {
		List<Leaf> itemElements = document.select("/items/item");

		Set<Integer> numbers = new HashSet<>();
		for (Leaf itemElement : itemElements) {
			int number = Integer.parseInt(itemElement.attribute("number"));
			if (number <= 0) {
				fail("Invalid item number: " + number);
			}
			if (!numbers.add(number)) {
				fail("Duplicate item number: " + number);
			}
		}

		/**
		 * Are any item numbers missing?
		 */
		List<Integer> sortedNumbers = new ArrayList<>(numbers);
		sortedNumbers.sort(null);
		int prev = 0;
		for (int number : sortedNumbers) {
			prev++;
			assertEquals("Item number missing: " + prev, prev, number);
		}
	}

	@Test
	public void pageNumbers() throws Exception {
		List<Leaf> itemElements = document.select("/items/item");

		Map<Integer, Integer> pageNumbers = new HashMap<>();
		for (Leaf itemElement : itemElements) {
			int number = Integer.parseInt(itemElement.attribute("number"));
			int pageNumber = Integer.parseInt(itemElement.attribute("page"));
			if (pageNumber <= 0) {
				fail("Invalid page number: " + pageNumber);
			}
			pageNumbers.put(number, pageNumber);
		}

		/**
		 * Is each item's page number greater than the previous item's page
		 * number?
		 */
		int prev = 0;
		for (int i = 1; true; i++) {
			Integer page = pageNumbers.get(i);
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
		List<Leaf> itemElements = document.select("/items/item");

		for (Leaf itemElement : itemElements) {
			Leaf titleElement = itemElement.selectFirst("title");
			assertNotNull("Missing <title> element on item " + itemElement.attribute("number") + ".", titleElement);
			assertFalse("Empty <title> element on item " + itemElement.attribute("number") + ".", titleElement.text().isEmpty());
		}
	}

	/**
	 * The "summary" element is required.
	 */
	@Test
	public void summaries() throws Exception {
		List<Leaf> itemElements = document.select("/items/item");

		for (Leaf itemElement : itemElements) {
			Leaf summaryElement = itemElement.selectFirst("summary");
			assertNotNull("Missing <summary> element on item " + itemElement.attribute("number") + ".", summaryElement);
			assertFalse("Empty <summary> element on item " + itemElement.attribute("number") + ".", summaryElement.text().isEmpty());
		}
	}
}
