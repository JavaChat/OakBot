package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.reply;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.ai.stabilityai.StabilityAIException;
import oakbot.ai.stabilityai.StableImageDiffusionRequest;
import oakbot.ai.stabilityai.VideoRequest;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.imgur.ImgurClient;
import oakbot.imgur.ImgurException;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Generates videos using Stable Diffusion.
 * @author Michael Angstadt
 */
public class VideoCommand implements Command {
	private static final Logger logger = LoggerFactory.getLogger(VideoCommand.class);

	private final StabilityAIClient stabilityAIClient;
	private final ImgurClient imgurClient;

	/**
	 * @param stabilityAIClient the Stability AI client
	 * @param imgurClient the imgur client
	 */
	public VideoCommand(StabilityAIClient stabilityAIClient, ImgurClient imgurClient) {
		this.stabilityAIClient = stabilityAIClient;
		this.imgurClient = imgurClient;
	}

	@Override
	public String name() {
		return "video";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates videos using Stable Diffusion Video (admins only).")
			.detail("Due to the time it takes to generate a video, this command may take several minutes to execute.")
			.example("a cute Java programmer walking on the beach", "Generates an image using Stable Diffusion 3.0, and then animates it.")
			.example("https://example.com/photo.jpg", "Creates a video from the given image.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		/*
		 * Check if admin.
		 */
		var userId = chatCommand.getMessage().getUserId();
		if (!bot.isAdminUser(userId)) {
			return reply("Only admins can invoke this command.", chatCommand);
		}

		var content = chatCommand.getContent().trim();
		if (content.isEmpty()) {
			return reply("Specify a prompt or image URL.", chatCommand);
		}

		try {
			var isUrl = content.matches("https?://.*");
			var image = isUrl ? downloadImage(content) : generateImage(content);

			image = resizeImage(image, 768, 768);

			var video = generateVideo(image);

			var videoUrl = uploadVideo(video);

			return create(new PostMessage(videoUrl).bypassFilters(true));
		} catch (Exception e) {
			logger.atError().setCause(e).log(() -> "Problem generating video.");
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getClass().getName()).append(": ").append(e.getMessage()).code(), chatCommand);
		}
	}

	private byte[] downloadImage(String url) throws IOException {
		var request = new HttpGet(url);

		try (var client = HttpFactory.connect().getClient()) {
			try (var response = client.execute(request)) {
				return EntityUtils.toByteArray(response.getEntity());
			}
		}
	}

	private byte[] generateImage(String prompt) throws StabilityAIException, IOException {
		//@formatter:off
		var request = new StableImageDiffusionRequest.Builder()
			.model("sd3")
			.prompt(prompt)
			.outputFormat("jpeg")
		.build();
		//@formatter:on

		var response = stabilityAIClient.generateImage(request);
		return response.getImage();
	}

	private byte[] resizeImage(byte[] original, int targetWidth, int targetHeight) throws IOException {
		BufferedImage originalImage;
		try (var in = new ByteArrayInputStream(original)) {
			originalImage = ImageIO.read(in);
		}

		var scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);

		var canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		var graphics = canvas.createGraphics();
		try {
			graphics.drawImage(scaledImage, 0, 0, null);
		} finally {
			graphics.dispose();
		}

		return writeAsJpeg(canvas);
	}

	private byte[] writeAsJpeg(BufferedImage image) throws IOException {
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

	private byte[] generateVideo(byte[] image) throws StabilityAIException, IOException, TimeoutException {
		//@formatter:off
		var videoResponse = stabilityAIClient.videoSync(new VideoRequest.Builder()
			.image(image, "image/jpeg")
		.build());
		//@formatter:on

		return videoResponse.getImage();
	}

	private String uploadVideo(byte[] video) throws ImgurException, IOException {
		return imgurClient.uploadFile(video);
	}
}
