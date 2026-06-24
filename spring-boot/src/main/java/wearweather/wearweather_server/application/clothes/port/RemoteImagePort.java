package wearweather.wearweather_server.application.clothes.port;

import wearweather.wearweather_server.application.clothes.RemoteImage;

public interface RemoteImagePort {
    RemoteImage download(String imageUrl);
}
