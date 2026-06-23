import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (_req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const yesterdayStr = yesterday.toISOString().split("T")[0];

  const { data: logs, error } = await supabase
    .from("weather_logs")
    .select("*")
    .eq("base_date", yesterdayStr);

  if (error) {
    console.error("weather_logs 조회 실패:", error);
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }

  if (!logs || logs.length === 0) {
    return new Response(
      JSON.stringify({ message: `${yesterdayStr} 데이터 없음` }),
      { status: 200 }
    );
  }

  const grouped: Record<string, typeof logs> = {};
  for (const log of logs) {
    if (!grouped[log.location_name]) grouped[log.location_name] = [];
    grouped[log.location_name].push(log);
  }

  const results = [];

  for (const [location_name, locationLogs] of Object.entries(grouped)) {
    const avg = (key: string): number | null => {
      const valid = locationLogs.filter((r) => r[key] != null);
      if (valid.length === 0) return null;
      return valid.reduce((sum, r) => sum + r[key], 0) / valid.length;
    };

    const minOf = (key: string): number | null => {
      const valid = locationLogs.map((l) => l[key]).filter((v) => v != null);
      return valid.length > 0 ? Math.min(...valid) : null;
    };

    const maxOf = (key: string): number | null => {
      const valid = locationLogs.map((l) => l[key]).filter((v) => v != null);
      return valid.length > 0 ? Math.max(...valid) : null;
    };

    const avgTemp = avg("current_temp");
    const avgPop = avg("pop");
    const avgHumidity = avg("humidity");
    const avgPm10 = avg("pm10");
    const avgPm25 = avg("pm25");

    const daily = {
      date: yesterdayStr,
      location_name,
      avg_temp: avgTemp,
      temp_min: minOf("temp_min"),
      temp_max: maxOf("temp_max"),
      precipitation: avg("precipitation"),
      pop: avgPop != null ? Math.round(avgPop) : null,
      humidity: avgHumidity != null ? Math.round(avgHumidity) : null,
      wind_speed: avg("wind_speed"),
      sky_status: locationLogs[locationLogs.length - 1]?.sky_status ?? null,
      uv_index: avg("uv_index"),
      pm10: avgPm10 != null ? Math.round(avgPm10) : null,
      pm25: avgPm25 != null ? Math.round(avgPm25) : null,
    };

    const { error: upsertError } = await supabase
      .from("weather_daily")
      .upsert(daily, { onConflict: "date,location_name" });

    if (upsertError) {
      console.error(`${location_name} upsert 실패:`, upsertError);
    }

    results.push({ location_name, success: !upsertError });
  }

  return new Response(JSON.stringify({ success: true, results }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
