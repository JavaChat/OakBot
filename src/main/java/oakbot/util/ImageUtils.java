package oakbot.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

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
