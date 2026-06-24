package wearweather.wearweather_server.infrastructure.gemini;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import wearweather.wearweather_server.application.gemini.RecommendationException;
import wearweather.wearweather_server.application.gemini.port.OutfitImageRenderer;
import wearweather.wearweather_server.application.gemini.port.RecommendationImageStoragePort.StoredImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class OutfitBoardRenderer implements OutfitImageRenderer {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1400;
    private static final int OUTER_MARGIN = 72;
    private static final int GAP = 36;
    private static final int CARD_PADDING = 28;

    @Override
    public byte[] render(List<StoredImage> images) {
        if (images.isEmpty()) {
            throw new RecommendationException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_RENDERABLE_CLOTHES",
                    "코디 이미지로 만들 수 있는 옷이 없습니다.");
        }

        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(new Color(244, 241, 236));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            int columns = images.size() == 1 ? 1 : 2;
            int rows = (images.size() + columns - 1) / columns;
            int cardWidth = (WIDTH - OUTER_MARGIN * 2 - GAP * (columns - 1)) / columns;
            int cardHeight = (HEIGHT - OUTER_MARGIN * 2 - GAP * (rows - 1)) / rows;

            for (int index = 0; index < images.size(); index++) {
                BufferedImage source = decode(images.get(index));
                int column = index % columns;
                int row = index / columns;
                int x = OUTER_MARGIN + column * (cardWidth + GAP);
                int y = OUTER_MARGIN + row * (cardHeight + GAP);
                drawCard(graphics, source, x, y, cardWidth, cardHeight);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(canvas, "png", output)) {
                throw new IOException("PNG writer is unavailable");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new RecommendationException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTFIT_IMAGE_RENDER_FAILED",
                    "코디 이미지를 생성하지 못했습니다.", exception);
        }
    }

    private BufferedImage decode(StoredImage image) {
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image.bytes()));
            if (decoded == null) throw new IOException("unsupported image data: " + image.contentType());
            return decoded;
        } catch (IOException exception) {
            throw new RecommendationException(HttpStatus.UNPROCESSABLE_ENTITY, "CLOTHES_IMAGE_DECODE_FAILED",
                    "옷 이미지를 해석하지 못했습니다.", exception);
        }
    }

    private void drawCard(Graphics2D graphics, BufferedImage source,
                          int x, int y, int width, int height) {
        graphics.setColor(Color.WHITE);
        graphics.fill(new RoundRectangle2D.Float(x, y, width, height, 34, 34));

        int availableWidth = width - CARD_PADDING * 2;
        int availableHeight = height - CARD_PADDING * 2;
        double scale = Math.min((double) availableWidth / source.getWidth(),
                (double) availableHeight / source.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        graphics.drawImage(source, drawX, drawY, drawWidth, drawHeight, null);
    }
}
