package oakbot.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Michael Angstadt
 */
public class GobbleTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void file() throws Exception {
		String data = "one two three";

		Path file = folder.newFile().toPath();
		Files.write(file, data.getBytes());

		Gobble stream = new Gobble(file);
		assertEquals(data, stream.asString());
		assertArrayEquals(data.getBytes(), stream.asByteArray());
	}

	@Test
	public void inputStream() throws Exception {
		String data = "one two three";

		InputStream in = new ByteArrayInputStream(data.getBytes());
		Gobble stream = new Gobble(in);
		assertEquals(data, stream.asString());
		assertArrayEquals(new byte[0], stream.asByteArray()); //input stream was consumed

		in = new ByteArrayInputStream(data.getBytes());
		stream = new Gobble(in);
		assertArrayEquals(data.getBytes(), stream.asByteArray());
	}

	@Test(expected = IllegalStateException.class)
	public void reader() throws Exception {
		String data = "one two three";

		Reader reader = new StringReader(data);
		Gobble stream = new Gobble(reader);
		assertEquals(data, stream.asString());
		stream.asByteArray();
	}
}
