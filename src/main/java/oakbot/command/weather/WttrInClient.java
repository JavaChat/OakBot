package oakbot.command.weather;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.HttpFactory;
import oakbot.util.JsonUtils;

/**
 * Queries the wttr.in API.
 * @author Michael Angstadt
 * @see "https://github.com/chubin/wttr.in"
 * @see "https://wttr.in/:help"
 */
public class WttrInClient {
	private static final Logger logger = LoggerFactory.getLogger(WttrInClient.class);

	/**
	 * Gets weather data in Prometheus format.
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<String> fetch(PrometheusRequest request) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request);
		var response = sendRequestAndParseResponseAsString(httpRequest);
		return new WttrInResponse<String>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in JSON format.
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<JsonNode> fetch(JsonRequest request) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request);
		var response = sendRequestAndParseResponseAsJson(httpRequest);
		return new WttrInResponse<JsonNode>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in the single-line format. For example:
	 * https://wttr.in/?format=3
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<String> fetch(SingleLineRequest request) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request, false, null, null);
		var response = sendRequestAndParseResponseAsString(httpRequest);
		return new WttrInResponse<String>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in the single-line format as a PNG. For example:
	 * https://wttr.in/?format=3
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<byte[]> fetchPng(SingleLineRequest request) throws WttrInException, IOException {
		return fetchPng(request, null, null);
	}

	/**
	 * Gets weather data in the single-line format as a PNG. For example:
	 * https://wttr.in/London.png?format=3
	 * @param request the request
	 * @param transparency how transparent the PNG should be (0-255) or null to
	 * not make it transparent
	 * @param backgroundColor background color for the PNG (hexcode, e.g.
	 * "aabbcc") or null not to specify a background color
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<byte[]> fetchPng(SingleLineRequest request, Integer transparency, String backgroundColor) throws WttrInException, IOException {
		/*
		 * Method parameter for PNG border is missing because it doesn't work
		 * with single line requests.
		 */
		var httpRequest = buildHttpRequest(request, true, transparency, backgroundColor);
		var response = sendRequestAndParseResponseAsBytes(httpRequest);
		return new WttrInResponse<byte[]>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in ASCII art format. This is the default format. For
	 * example: https://wttr.in
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<String> fetch(AsciiArtRequest request) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request, false, false, false, null, null);
		var response = sendRequestAndParseResponseAsString(httpRequest);
		return new WttrInResponse<String>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in ASCII art format as HTML. For example:
	 * https://wttr.in
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<String> fetchHtml(AsciiArtRequest request) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request, true, false, false, null, null);
		var response = sendRequestAndParseResponseAsString(httpRequest);
		return new WttrInResponse<String>(httpRequest.getURI(), response);
	}

	/**
	 * Gets weather data in ASCII art format as a PNG. For example:
	 * https://wttr.in/London.png
	 * @param request the request
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<byte[]> fetchPng(AsciiArtRequest request) throws WttrInException, IOException {
		return fetchPng(request, false, null, null);
	}

	/**
	 * Gets weather data in ASCII art format as a PNG. For example:
	 * https://wttr.in/London.png
	 * @param request the request
	 * @param border true to render a border around the PNG
	 * @param transparency how transparent the PNG should be (0-255) or null to
	 * not make it transparent
	 * @param backgroundColor background color for the PNG (hexcode, e.g.
	 * "aabbcc") or null not to specify a background color
	 * @return the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem querying the API
	 */
	public WttrInResponse<byte[]> fetchPng(AsciiArtRequest request, boolean border, Integer transparency, String backgroundColor) throws WttrInException, IOException {
		var httpRequest = buildHttpRequest(request, false, true, border, transparency, backgroundColor);
		var response = sendRequestAndParseResponseAsBytes(httpRequest);
		return new WttrInResponse<byte[]>(httpRequest.getURI(), response);
	}

	private HttpUriRequest buildHttpRequest(PrometheusRequest request) {
		var rc = buildBaseRequestComponents(request);

		rc.addParameter("format", "p1");

		return rc.build();
	}

	private HttpUriRequest buildHttpRequest(JsonRequest request) {
		var rc = buildBaseRequestComponents(request);

		var format = request.isIncludeHourlyData() ? "j1" : "j2";
		rc.addParameter("format", format);

		return rc.build();
	}

	private HttpUriRequest buildHttpRequest(SingleLineRequest request, boolean png, Integer transparency, String backgroundColor) {
		var rc = buildBaseRequestComponents(request, png, false, transparency, backgroundColor);

		if (request.getUnit() != null) {
			var code = switch (request.getUnit()) {
			case METRIC -> 'm';
			case US -> 'u';
			};
			rc.flags.append(code);
		}

		if (request.isUseMetricAndShowWindSpeedInMetersPerSec()) {
			rc.flags.append('M');
		}
		if (request.isIgnoreUserAgentAndForceANSIOutput()) {
			rc.flags.append('A');
		}

		var format = request.getCustomFormat();
		if (format == null) {
			if (request.isShowWind() && request.isShowLocation()) {
				format = "4";
			} else if (request.isShowWind()) {
				format = "2";
			} else if (request.isShowLocation()) {
				format = "3";
			} else {
				format = "1";
			}
		}
		rc.addParameter("format", format);

		return rc.build();
	}

	private HttpUriRequest buildHttpRequest(AsciiArtRequest request, boolean html, boolean png, boolean border, Integer transparency, String backgroundColor) {
		var rc = buildBaseRequestComponents(request, png, border, transparency, backgroundColor);

		if (html) {
			rc.addHeader("User-Agent", ".");
		}

		if (request.getView() != null) {
			var flag = switch (request.getView()) {
			case CURRENT -> '0';
			case CURRENT_TODAY_FORECAST -> '1';
			case CURRENT_TODAY_TOMORROW_FORECAST -> '2';
			case CURRENT_TODAY_TOMORROW_OVERMORROW_FORECAST -> '3';
			};
			rc.flags.append(flag);
		}
		if (request.isOnlyShowDayAndNight()) {
			rc.flags.append('n');
		}

		if (request.getUnit() != null) {
			var code = switch (request.getUnit()) {
			case METRIC -> 'm';
			case US -> 'u';
			};
			rc.flags.append(code);
		}
		if (request.isShowWindSpeedInMetersPerSec()) {
			rc.flags.append('M');
		}

		if (request.isHideWeatherReportHeading()) {
			rc.flags.append('q');
		}
		if (request.isHideWeatherReportHeadingAndCityName()) {
			rc.flags.append('Q');
		}
		if (request.isHideSocialMedia()) {
			rc.flags.append('F');
		}

		if (request.isForceANSIOutput()) {
			rc.flags.append('A');
		}
		if (request.isDisableTerminalColorSequences()) {
			rc.flags.append('T');
		}
		if (request.isOnlyUseStandardConsoleFontGlyphs()) {
			rc.flags.append('d');
		}

		return rc.build();
	}

	private RequestComponents buildBaseRequestComponents(WttrInRequest request) {
		return buildBaseRequestComponents(request, false, false, null, null);
	}

	private RequestComponents buildBaseRequestComponents(WttrInRequest request, boolean png, boolean border, Integer transparency, String backgroundColor) {
		var builder = new RequestComponents();

		if (request.getLocation() != null) {
			builder.locationPathSegment = request.getLocation() + (png ? ".png" : "");
		}

		if (request.getLanguage() != null) {
			builder.addParameter("lang", request.getLanguage());
		}

		if (png) {
			if (border) {
				builder.flags.append('p');
			}
			if (transparency != null) {
				builder.addParameter("transparency", transparency + "");
			}
			if (backgroundColor != null) {
				builder.addParameter("background", backgroundColor);
			}
		}

		return builder;
	}

	private <T> T sendRequest(HttpUriRequest request, EntityProcessor<T> entityProcessor) throws WttrInException, IOException {
		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				checkForError(response);
				var entity = response.getEntity();
				return entityProcessor.apply(entity);
			}
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem sending wttr.in request: " + request.getURI());
			throw e;
		}
	}

	private String sendRequestAndParseResponseAsString(HttpUriRequest request) throws WttrInException, IOException {
		return sendRequest(request, EntityUtils::toString);
	}

	private JsonNode sendRequestAndParseResponseAsJson(HttpUriRequest request) throws WttrInException, IOException {
		return sendRequest(request, entity -> {
			try (var in = entity.getContent()) {
				return JsonUtils.parse(in);
			}
		});
	}

	private byte[] sendRequestAndParseResponseAsBytes(HttpUriRequest request) throws WttrInException, IOException {
		return sendRequest(request, EntityUtils::toByteArray);
	}

	/**
	 * Checks the response for an error message and throws an exception an error
	 * was found.
	 * @param response the response
	 * @throws WttrInException if the API returned an error
	 * @throws IOException if there was a problem getting the response body
	 */
	private void checkForError(HttpResponse response) throws WttrInException, IOException {
		/*
		 * Treat all non-200 responses as an error.
		 */
		var statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			var entity = response.getEntity();
			var responseBody = EntityUtils.toString(entity);
			throw new WttrInException(responseBody);
		}
	}

	private static class RequestComponents {
		private String locationPathSegment;
		private List<NameValuePair> parameters = new ArrayList<>();
		private List<Header> headers = new ArrayList<>();
		private StringBuilder flags = new StringBuilder();

		private void addParameter(String name, String value) {
			parameters.add(new BasicNameValuePair(name, value));
		}

		private void addHeader(String name, String value) {
			headers.add(new BasicHeader(name, value));
		}

		private URI buildUri() {
			/*
			 * "You can safely use wttr.is anywhere you currently use wttr.in.
			 * Both domains are served from the same backend and kept in sync.
			 * We recommend using wttr.is in scripts, status bars, monitoring
			 * tools, and CI/CD pipelines for improved reliability."
			 */
			var uri = new URIBuilder().setScheme("https").setHost("wttr.is");
			if (locationPathSegment != null) {
				uri.setPathSegments(locationPathSegment);
			}
			if (!flags.isEmpty()) {
				uri.addParameter(flags.toString(), null);
			}
			uri.addParameters(parameters);

			return URI.create(uri.toString());
		}

		private HttpUriRequest build() {
			var uri = buildUri();
			var request = new HttpGet(uri);
			headers.forEach(request::addHeader);
			return request;
		}
	}

	private interface EntityProcessor<T> {
		/**
		 * Extracts the content from the response body.
		 * @param entity the response entity
		 * @return the parsed response
		 * @throws IOException if there is a problem parsing the response
		 */
		T apply(HttpEntity entity) throws IOException;
	}
}
