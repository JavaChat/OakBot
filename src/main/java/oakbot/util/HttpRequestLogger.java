package oakbot.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Logs HTTP requests and responses to a CSV file.
 * @author Michael Angstadt
 */
public class HttpRequestLogger {
	private final CSVWriter writer;
	private final DateTimeFormatter dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("America/New_York"));

	/**
	 * @param path the path to the CSV file (will append onto the file)
	 * @throws IOException if there is a problem creating the file
	 */
	public HttpRequestLogger(String path) throws IOException {
		this(Paths.get(path));
	}

	/**
	 * @param path the path to the CSV file (will append onto the file)
	 * @throws IOException if there is a problem creating the file
	 */
	public HttpRequestLogger(Path path) throws IOException {
		var writeHeaders = !Files.exists(path);

		writer = new CSVWriter(Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND));

		if (writeHeaders) {
			writer.writeNext(new String[] { "Timestamp", "Request", "Response" });
			writer.flush();
		}
	}

	/**
	 * Logs a request/response.
	 * @param requestMethod the request method (e.g. "POST")
	 * @param requestUrl the request URL
	 * @param requestBody the request body
	 * @param responseStatusCode the response status code (e.g. "200")
	 * @param responseBody the response body
	 * @throws IOException if there was a problem writing to the file
	 */
	public void log(String requestMethod, String requestUrl, String requestBody, int responseStatusCode, String responseBody) throws IOException {
		var timestamp = Now.local().format(dtFormatter);
		var request = "HTTP " + requestMethod + " " + requestUrl + "\n" + requestBody;
		var response = "HTTP " + responseStatusCode + "\n" + responseBody;

		var line = new String[] { timestamp, request, response };
		synchronized (this) {
			writer.writeNext(line);
			writer.flush();
		}
	}
}
