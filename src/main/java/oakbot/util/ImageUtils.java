package oakbot.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jsoup.UncheckedIOException;

/**
 * @author Michael Angstadt
 */
public class ImageUtils {
	/**
	 * Converts the given image data to PNG format.
	 * @param data the image data
	 * @return the PNG image
	 * @throws IOException if there is a problem converting the image
	 * @throws IllegalArgumentException if the input data is not a recognized
	 * image format
	 */
	public static byte[] convertToPng(byte[] data) throws IOException {
		BufferedImage image;
		try (var in = new ByteArrayInputStream(data)) {
			image = ImageIO.read(in);
		}
		if (image == null) {
			throw new IllegalArgumentException("Input data is not a recognized image format.");
		}

		try (var out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", out);
			return out.toByteArray();
		}
	}

	/**
	 * Converts an image to JPEG.
	 * @param image the image to convert
	 * @return the JPEG data
	 * @see "https://stackoverflow.com/q/17108234/13379"
	 */
	public static byte[] convertToJpeg(BufferedImage image) throws IOException {
		/*
		 * If the image has an alpha channel, an exception is thrown when it
		 * tries to write the image as a JPEG.
		 * 
		 * javax.imageio.IIOException: Bogus input colorspace
		 */
		image = ImageUtils.removeAlphaChannel(image);

		var jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		var jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.9f);

		try (var out = new ByteArrayOutputStream()) {
			jpgWriter.setOutput(ImageIO.createImageOutputStream(out));
			jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
			return out.toByteArray();
		} finally {
			jpgWriter.dispose();
		}
	}

	/**
	 * Converts an SVG image to PNG
	 * @param svg the svg code
	 * @return the PNG image
	 * @throws TranscoderException if there is a problem converting the image
	 */
	public static byte[] svgToPng(String svg) throws TranscoderException {
		var transInput = new TranscoderInput(new StringReader(svg));
		PNGTranscoder transcoder = new PNGTranscoder();

		try (var out = new ByteArrayOutputStream()) {
			var transOutput = new TranscoderOutput(out);
			transcoder.transcode(transInput, transOutput);
			return out.toByteArray();
		} catch (IOException e) {
			/*
			 * Should never be thrown because we're writing to a byte array.
			 */
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Removes the alpha channel from the image, if present.
	 * @param image the image
	 * @return the image with alpha channel removed
	 * @see "https://stackoverflow.com/a/72135983/13379"
	 */
	public static BufferedImage removeAlphaChannel(BufferedImage image) {
		if (!image.getColorModel().hasAlpha()) {
			return image;
		}

		var target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

		var g = target.createGraphics();
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return target;
	}

	private ImageUtils() {
		//hide
	}
}
