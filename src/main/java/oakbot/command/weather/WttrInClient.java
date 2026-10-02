package oakbot.command.weather;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangstadt.sochat4j.util.Http;

import oakbot.util.HttpFactory;

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
	 * @throws IOException if there was a problem querying the API
	 */
	public String fetch(PrometheusRequest request) throws IOException {
		var uri = uri(request);
		var response = send(uri);
		return response.getBody();
	}

	/**
	 * Gets weather data in JSON format.
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public JsonNode fetch(JsonRequest request) throws IOException {
		var uri = uri(request);
		var response = send(uri);
		return response.getBodyAsJson();
	}

	/**
	 * Gets weather data in the single-line format. For example:
	 * https://wttr.in/?format=3
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public String fetch(SingleLineRequest request) throws IOException {
		var uri = uri(request, false, null, null);
		var response = send(uri);
		return response.getBody();
	}

	/**
	 * Gets weather data in the single-line format as a PNG. For example:
	 * https://wttr.in/?format=3
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public byte[] fetchAsPng(SingleLineRequest request) throws IOException {
		return fetchAsPng(request, null, null);
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
	 * @throws IOException if there was a problem querying the API
	 */
	public byte[] fetchAsPng(SingleLineRequest request, Integer transparency, String backgroundColor) throws IOException {
		/*
		 * Method parameter for PNG border is missing because it doesn't work
		 * with single line requests.
		 */
		var uri = uri(request, true, transparency, backgroundColor);
		var response = send(uri);
		return response.getBodyAsBytes();
	}

	/**
	 * Gets weather data in ASCII art format. This is the default format. For
	 * example: https://wttr.in
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public String fetch(AsciiArtRequest request) throws IOException {
		var uri = uri(request, false, false, false, null, null);
		var response = send(uri);
		return response.getBody();
	}

	/**
	 * Gets weather data in ASCII art format as HTML. For example:
	 * https://wttr.in
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public String fetchHtml(AsciiArtRequest request) throws IOException {
		var uri = uri(request, true, false, false, null, null);
		var response = send(uri);
		return response.getBody();
	}

	/**
	 * Gets weather data in ASCII art format as a PNG. For example:
	 * https://wttr.in/London.png
	 * @param request the request
	 * @return the response
	 * @throws IOException if there was a problem querying the API
	 */
	public byte[] fetchPng(AsciiArtRequest request, boolean border, Integer transparency, String backgroundColor) throws IOException {
		var uri = uri(request, false, true, border, transparency, backgroundColor);
		var response = send(uri);
		return response.getBodyAsBytes();
	}

	private String uri(PrometheusRequest request) {
		var builder = baseUri(request);

		builder.addParameter("format", "p1");

		return builder.build();
	}

	private String uri(JsonRequest request) {
		var builder = baseUri(request);

		var format = request.isIncludeHourlyData() ? "j1" : "j2";
		builder.addParameter("format", format);

		return builder.build();
	}

	private String uri(SingleLineRequest request, boolean png, Integer transparency, String backgroundColor) {
		var builder = baseUri(request, png, false, transparency, backgroundColor);

		if (request.getUnit() != null) {
			var code = switch (request.getUnit()) {
			case METRIC -> 'm';
			case US -> 'u';
			};
			builder.flags.append(code);
		}

		if (request.isUseMetricAndShowWindSpeedInMetersPerSec()) {
			builder.flags.append("M");
		}
		if (request.isIgnoreUserAgentAndForceANSIOutput()) {
			builder.flags.append("A");
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
		builder.addParameter("format", format);

		return builder.build();
	}

	private String uri(AsciiArtRequest request, boolean html, boolean png, boolean border, Integer transparency, String backgroundColor) {
		var builder = baseUri(request, png, border, transparency, backgroundColor);

		if (request.getView() != null) {
			var flag = switch (request.getView()) {
			case CURRENT -> '0';
			case CURRENT_TODAY_FORECAST -> '1';
			case CURRENT_TODAY_TOMORROW_FORECAST -> '2';
			};
			builder.flags.append(flag);
		}
		if (request.isOnlyShowDayAndNight()) {
			builder.flags.append('n');
		}

		if (request.getUnit() != null) {
			var code = switch (request.getUnit()) {
			case METRIC -> 'm';
			case US -> 'u';
			};
			builder.flags.append(code);
		}
		if (request.isShowWindSpeedInMetersPerSec()) {
			builder.flags.append('M');
		}

		if (request.isHideWeatherReportHeading()) {
			builder.flags.append('q');
		}
		if (request.isHideWeatherReportHeadingAndCityName()) {
			builder.flags.append('Q');
		}
		if (request.isHideSocialMedia()) {
			builder.flags.append('F');
		}

		if (request.isForceANSIOutput()) {
			builder.flags.append('A');
		}
		if (request.isDisableTerminalColorSequences()) {
			builder.flags.append('T');
		}
		if (request.isOnlyUseStandardConsoleFontGlyphs()) {
			builder.flags.append('d');
		}

		return builder.build();
	}

	/**
	 * Starts creating the request URI using information common to all requests.
	 * @param request the request
	 * @return the URI
	 */
	private WttrUriBuilder baseUri(WttrInRequest request) {
		return baseUri(request, false, false, null, null);
	}

	/**
	 * Starts creating the request URI using information common to all requests.
	 * @param request the request
	 * @param png true to generate a PNG
	 * @param border true to render a border around the PNG
	 * @param transparency how transparent the PNG should be (0-255) or null to
	 * not make it transparent
	 * @param backgroundColor background color for the PNG (hexcode, e.g.
	 * "aabbcc") or null not to specify a background color
	 * @return the URI
	 */
	private WttrUriBuilder baseUri(WttrInRequest request, boolean png, boolean border, Integer transparency, String backgroundColor) {
		var builder = new WttrUriBuilder();

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

	private Http.Response send(String uri) throws IOException {
		try (var http = HttpFactory.connect()) {
			return http.get(uri);
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem sending wttr.in request: " + uri);
			throw e;
		}
	}

	private static class WttrUriBuilder {
		private String locationPathSegment;
		private List<NameValuePair> parameters = new ArrayList<>();
		private StringBuilder flags = new StringBuilder();

		private void addParameter(String name, String value) {
			parameters.add(new BasicNameValuePair(name, value));
		}

		public String build() {
			/*
			 * "You can safely use wttr.is anywhere you currently use wttr.in.
			 * Both domains are served from the same backend and kept in sync.
			 * We recommend using wttr.is in scripts, status bars, monitoring
			 * tools, and CI/CD pipelines for improved reliability."
			 */
			var uri = new URIBuilder().setScheme("https").setHost("wttr.is");
			uri.setPathSegments(locationPathSegment);
			uri.addParameter(flags.toString(), "");
			uri.addParameters(parameters);
			return uri.toString();
		}
	}
}
