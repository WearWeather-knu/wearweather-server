package wearweather.wearweather_server.application.clothes.port;

import wearweather.wearweather_server.application.clothes.ClothesInferenceResult;
import wearweather.wearweather_server.application.clothes.MusinsaProduct;
import wearweather.wearweather_server.application.clothes.RemoteImage;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

public interface ClothesInferencePort {
    ClothesInferenceResult infer(MusinsaProduct product, ClothesCategory category, RemoteImage image);
}
