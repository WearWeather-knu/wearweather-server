package wearweather.wearweather_server.application.clothes.port;

import wearweather.wearweather_server.application.clothes.RemoteImage;

public interface ClothesImageStoragePort {
    String uploadMusinsaProduct(String productId, RemoteImage image);
}
