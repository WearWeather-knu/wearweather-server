import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { location_name } = await req.json();

    if (!location_name) {
      return new Response(
        JSON.stringify({ error: "location_name이 필요합니다." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    const todayStr = new Date().toISOString().split("T")[0];
    const yesterdayDate = new Date();
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);
    const yesterdayStr = yesterdayDate.toISOString().split("T")[0];

    // weather_daily에서 오늘/어제 조회
    const { data: dailyData, error: dailyError } = await supabase
      .from("weather_daily")
      .select("*")
      .eq("location_name", location_name)
      .in("date", [todayStr, yesterdayStr]);

    if (dailyError) {
      return new Response(JSON.stringify({ error: dailyError.message }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    let todayData = dailyData?.find((d) => d.date === todayStr) ?? null;
    const yesterdayData = dailyData?.find((d) => d.date === yesterdayStr) ?? null;

    // 오늘 데이터가 weather_daily에 없으면 weather_logs에서 집계
    if (!todayData) {
      const { data: todayLogs, error: logsError } = await supabase
        .from("weather_logs")
        .select("*")
        .eq("base_date", todayStr)
        .eq("location_name", location_name);

      if (!logsError && todayLogs && todayLogs.length > 0) {
        const avg = (key: string): number | null => {
          const valid = todayLogs.filter((r) => r[key] != null);
          if (valid.length === 0) return null;
          return valid.reduce((sum, r) => sum + r[key], 0) / valid.length;
        };

        const minOf = (key: string): number | null => {
          const valid = todayLogs.map((l) => l[key]).filter((v) => v != null);
          return valid.length > 0 ? Math.min(...valid) : null;
        };

        const maxOf = (key: string): number | null => {
          const valid = todayLogs.map((l) => l[key]).filter((v) => v != null);
          return valid.length > 0 ? Math.max(...valid) : null;
        };

        const avgPop = avg("pop");
        const avgHumidity = avg("humidity");
        const avgPm10 = avg("pm10");
        const avgPm25 = avg("pm25");

        todayData = {
          date: todayStr,
          location_name,
          avg_temp: avg("current_temp"),
          feels_like: avg("feels_like"),
          temp_min: minOf("temp_min"),
          temp_max: maxOf("temp_max"),
          precipitation: avg("precipitation"),
          pop: avgPop != null ? Math.round(avgPop) : null,
          humidity: avgHumidity != null ? Math.round(avgHumidity) : null,
          wind_speed: avg("wind_speed"),
          sky_status: todayLogs[todayLogs.length - 1]?.sky_status ?? null,
          uv_index: avg("uv_index"),
          pm10: avgPm10 != null ? Math.round(avgPm10) : null,
          pm25: avgPm25 != null ? Math.round(avgPm25) : null,
        };

        // weather_daily에 오늘 데이터 upsert
        await supabase
          .from("weather_daily")
          .upsert(todayData, { onConflict: "date,location_name" });
      }
    }

    return new Response(
      JSON.stringify({ today: todayData, yesterday: yesterdayData }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
