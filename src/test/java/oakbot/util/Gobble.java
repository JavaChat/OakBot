package oakbot.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gets the entire contents of an input stream, reader, or file.
 * @author Michael Angstadt
 */
public class Gobble {
	private final Path file;
	private final InputStream in;
	private final Reader reader;

	/**
	 * Gets the contents of a file.
	 * @param file the file
	 */
	public Gobble(Path file) {
		this(file, null, null);
	}

	/**
	 * Gets the contents of an input stream.
	 * @param in the input stream
	 */
	public Gobble(InputStream in) {
		this(null, in, null);
	}

	/**
	 * Gets the contents of a reader.
	 * @param reader the reader
	 */
	public Gobble(Reader reader) {
		this(null, null, reader);
	}

	private Gobble(Path file, InputStream in, Reader reader) {
		this.file = file;
		this.in = in;
		this.reader = reader;
	}

	/**
	 * Gets the stream contents as a string. If something other than a
	 * {@link Reader} was passed into this class's constructor, this method
	 * decodes the stream data using the system's default character encoding.
	 * @return the string
	 * @throws IOException if there was a problem reading from the stream
	 */
	public String asString() throws IOException {
		return asString(Charset.defaultCharset());
	}

	/**
	 * Gets the stream contents as a string.
	 * @param charset the character set to decode the stream data with (this
	 * parameter is ignored if a {@link Reader} was passed into this class's
	 * constructor)
	 * @return the string
	 * @throws IOException if there was a problem reading from the stream
	 */
	public String asString(Charset charset) throws IOException {
		Reader reader = buildReader(charset);
		return consumeReader(reader);
	}

	/**
	 * Gets the stream contents as a byte array.
	 * @return the byte array
	 * @throws IOException if there was a problem reading from the stream
	 * @throws IllegalStateException if a {@link Reader} object was passed into
	 * this class's constructor
	 */
	public byte[] asByteArray() throws IOException {
		if (reader != null) {
			throw new IllegalStateException("Cannot get raw bytes from a Reader object.");
		}

		InputStream in = buildInputStream();
		return consumeInputStream(in);
	}

	private Reader buildReader(Charset charset) throws IOException {
		if (reader != null) {
			return reader;
		}
		if (file != null) {
			return Files.newBufferedReader(file, charset);
		}
		return new InputStreamReader(in, charset);
	}

	private InputStream buildInputStream() throws IOException {
		return (in == null) ? Files.newInputStream(file) : in;
	}

	private String consumeReader(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[4096];
		int read;
		try {
			while ((read = reader.read(buffer)) != -1) {
				sb.append(buffer, 0, read);
			}
		} finally {
			reader.close();
		}
		return sb.toString();
	}

	private byte[] consumeInputStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		try {
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		} finally {
			in.close();
		}
		return out.toByteArray();
	}
}
