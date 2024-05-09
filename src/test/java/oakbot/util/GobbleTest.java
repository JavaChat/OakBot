package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Michael Angstadt
 */
public class GobbleTest {
	@TempDir
	private Path tempDir;

	@Test
	public void file() throws Exception {
		var data = "one two three";

		var file = Files.createTempFile(tempDir, null, null);
		Files.write(file, data.getBytes());

		var stream = new Gobble(file);
		assertEquals(data, stream.asString());
		assertArrayEquals(data.getBytes(), stream.asByteArray());
	}

	@Test
	public void inputStream() throws Exception {
		var data = "one two three";

		var in = new ByteArrayInputStream(data.getBytes());
		var stream = new Gobble(in);
		assertEquals(data, stream.asString());
		assertArrayEquals(new byte[0], stream.asByteArray()); //input stream was consumed

		in = new ByteArrayInputStream(data.getBytes());
		stream = new Gobble(in);
		assertArrayEquals(data.getBytes(), stream.asByteArray());
	}

	@Test
	public void reader() throws Exception {
		var data = "one two three";

		var reader = new StringReader(data);
		var stream = new Gobble(reader);
		assertEquals(data, stream.asString());
		
		assertThrows(IllegalStateException.class, () -> stream.asByteArray());
	}
}
