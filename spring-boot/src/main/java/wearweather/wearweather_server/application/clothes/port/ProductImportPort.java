package wearweather.wearweather_server.application.clothes.port;

import wearweather.wearweather_server.application.clothes.MusinsaProduct;

public interface ProductImportPort {
    MusinsaProduct fetch(String originalUrl);
}
