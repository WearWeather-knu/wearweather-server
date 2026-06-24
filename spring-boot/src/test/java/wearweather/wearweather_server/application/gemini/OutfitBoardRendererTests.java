package wearweather.wearweather_server.application.gemini;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutfitBoardRendererTests {
    private final OutfitBoardRenderer renderer = new OutfitBoardRenderer();

    @Test
    void rendersMultipleSourceImagesAsOnePngBoard() throws Exception {
        byte[] portrait = image("png", 240, 420, Color.BLUE);
        byte[] landscape = image("jpg", 500, 260, Color.RED);

        byte[] result = renderer.render(List.of(
                new RecommendationImageStorageClient.StoredImage(portrait, "image/png"),
                new RecommendationImageStorageClient.StoredImage(landscape, "image/jpeg")
        ));

        BufferedImage board = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(board.getWidth()).isEqualTo(1000);
        assertThat(board.getHeight()).isEqualTo(1400);
        assertThat(result).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    }

    @Test
    void webpDecoderIsRegistered() {
        assertThat(ImageIO.getImageReadersByMIMEType("image/webp").hasNext()).isTrue();
    }

    private byte[] image(String format, int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return output.toByteArray();
        }
    }
}
