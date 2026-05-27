import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const OPENWEATHER_API_KEY = Deno.env.get("OPENWEATHER_API_KEY")!;
const OPENUV_API_KEY = Deno.env.get("OPENUV_API_KEY")!;
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req) => {
  try {
    const { lat, lon, location_name } = await req.json();
    console.log("요청 받음:", { lat, lon, location_name });

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    // 현재 날씨
    const weatherRes = await fetch(
      `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}&units=metric`
    );
    const weather = await weatherRes.json();
    console.log("날씨 응답:", JSON.stringify(weather));

    // 대기질 (pm10, pm25)
    const airRes = await fetch(
      `https://api.openweathermap.org/data/2.5/air_pollution?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}`
    );
    const air = await airRes.json();
    console.log("대기질 응답:", JSON.stringify(air));

    // UV 지수
    const uvRes = await fetch(
      `https://api.openuv.io/api/v1/uv?lat=${lat}&lng=${lon}`,
      { headers: { "x-access-token": OPENUV_API_KEY } }
    );
    const uv = await uvRes.json();
    console.log("UV 응답:", JSON.stringify(uv));

    const { error } = await supabase.from("weather_logs").insert({
      location_name,
      base_date: new Date().toISOString().split("T")[0],
      current_temp: weather.main.temp,
      temp_min: weather.main.temp_min,
      temp_max: weather.main.temp_max,
      feels_like: weather.main.feels_like,
      humidity: weather.main.humidity,
      wind_speed: weather.wind.speed,
      precipitation: weather.rain?.["1h"] ?? 0,
      sky_status: weather.weather[0].description,
      pm10: Math.round(air.list[0].components.pm10),
      pm25: Math.round(air.list[0].components.pm2_5),
      uv_index: uv.result?.uv ?? null,
    });

    if (error) {
      console.log("DB 저장 에러:", JSON.stringify(error));
      return new Response(JSON.stringify({ error }), { status: 500 });
    }

    const weatherData = {
      location_name,
      base_date: new Date().toISOString().split("T")[0],
      current_temp: weather.main.temp,
      temp_min: weather.main.temp_min,
      temp_max: weather.main.temp_max,
      feels_like: weather.main.feels_like,
      humidity: weather.main.humidity,
      wind_speed: weather.wind.speed,
      precipitation: weather.rain?.["1h"] ?? 0,
      sky_status: weather.weather[0].description,
      pm10: Math.round(air.list[0].components.pm10),
      pm25: Math.round(air.list[0].components.pm2_5),
      uv_index: uv.result?.uv ?? null,
    };

    return new Response(JSON.stringify({ success: true, data: weatherData }), { status: 200 });
  } catch (e) {
    console.log("예외 발생:", e.message);
    return new Response(JSON.stringify({ error: e.message }), { status: 500 });
  }
});
