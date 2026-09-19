package engine;

import util.Json;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LIVE WEATHER
 *
 * Pulls the current conditions and the five day outlook for a town from
 * OpenWeatherMap, so the farmer can ask "weather in guntur" or "will it rain
 * tomorrow" and plan spraying, irrigation and sowing around real numbers.
 *
 * The API key and the default town live in data/weather.txt:
 *
 *     key=....
 *     city=Hyderabad
 *
 * Nothing here is cached; every question is a fresh call, which is what a
 * farmer wants when a storm is coming.
 */
public class WeatherService {

    /** What it is like right now. */
    public static class Report {
        public String city, country, condition;
        public double temp, feelsLike, humidity, windMs, rainMm;
        public int cloudPct;
        public List<Day> days = new ArrayList<>();     // outlook, today first
    }

    /** One day of the outlook, folded from the 3-hourly slots. */
    public static class Day {
        public String name, condition;
        public double min = 99, max = -99, rainMm, rainChance;
    }

    private String key = "";
    private String defaultCity = "";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).build();

    public WeatherService(String dataFolder) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(dataFolder + "/weather.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                String k = line.substring(0, line.indexOf('=')).trim().toLowerCase();
                String v = line.substring(line.indexOf('=') + 1).trim();
                if (k.equals("key"))  key = v;
                if (k.equals("city")) defaultCity = v;
            }
        } catch (Exception e) {
            // no file: the feature just reports that it is not set up
        }
    }

    public boolean isConfigured() { return !key.isEmpty(); }
    public String defaultCity()   { return defaultCity; }
    public void setDefaultCity(String city) { defaultCity = city; }

    /**
     * Current weather plus the outlook. Throws with a short, farmer-readable
     * message when the town is unknown or the network is down.
     */
    public Report lookup(String city) throws Exception {
        if (!isConfigured()) throw new Exception("no weather key in data/weather.txt");
        if (city == null || city.trim().isEmpty()) city = defaultCity;
        if (city.isEmpty()) throw new Exception("tell me a town, e.g. \"weather in guntur\"");

        Object now = call("weather", city);
        Report r = new Report();
        r.city      = Json.str(now, "name");
        r.country   = Json.str(now, "sys", "country");
        r.condition = Json.str(now, "weather", 0, "description");
        r.temp      = Json.num(now, "main", "temp");
        r.feelsLike = Json.num(now, "main", "feels_like");
        r.humidity  = Json.num(now, "main", "humidity");
        r.windMs    = Json.num(now, "wind", "speed");
        r.rainMm    = Json.num(now, "rain", "1h");
        r.cloudPct  = (int) Json.num(now, "clouds", "all");

        try {
            r.days = outlook(call("forecast", city));
        } catch (Exception e) {
            // the outlook is a bonus; the current reading still stands
        }
        return r;
    }

    private Object call(String endpoint, String city) throws Exception {
        String url = "https://api.openweathermap.org/data/2.5/" + endpoint
                   + "?q=" + URLEncoder.encode(city.trim(), StandardCharsets.UTF_8)
                   + "&units=metric&appid=" + key;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new Exception("could not reach the weather service - check the internet connection");
        }
        Object body = Json.parse(res.body());
        if (res.statusCode() == 404) throw new Exception("I do not know a town called \"" + city.trim() + "\"");
        if (res.statusCode() == 401) throw new Exception("the weather key was refused - check data/weather.txt");
        if (res.statusCode() != 200) throw new Exception("weather service said: " + Json.str(body, "message"));
        return body;
    }

    /** Fold the 3-hourly slots into one row per local day. */
    private static List<Day> outlook(Object fc) {
        int tz = (int) Json.num(fc, "city", "timezone");
        ZoneOffset zone = ZoneOffset.ofTotalSeconds(tz);
        Map<LocalDate, Day> byDate = new LinkedHashMap<>();
        Map<LocalDate, Map<String, Integer>> conditions = new LinkedHashMap<>();

        for (Object slot : Json.list(fc, "list")) {
            LocalDate date = Instant.ofEpochSecond((long) Json.num(slot, "dt"))
                                    .atOffset(zone).toLocalDate();
            Day d = byDate.computeIfAbsent(date, k -> {
                Day nd = new Day();
                nd.name = k.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        + " " + k.getDayOfMonth();
                return nd;
            });
            d.min = Math.min(d.min, Json.num(slot, "main", "temp_min"));
            d.max = Math.max(d.max, Json.num(slot, "main", "temp_max"));
            d.rainMm += Json.num(slot, "rain", "3h");
            d.rainChance = Math.max(d.rainChance, Json.num(slot, "pop") * 100);

            String cond = Json.str(slot, "weather", 0, "description");
            conditions.computeIfAbsent(date, k -> new LinkedHashMap<>()).merge(cond, 1, Integer::sum);
        }

        List<Day> out = new ArrayList<>();
        for (Map.Entry<LocalDate, Day> e : byDate.entrySet()) {
            Day d = e.getValue();
            String best = ""; int bestN = -1;
            for (Map.Entry<String, Integer> c : conditions.get(e.getKey()).entrySet())
                if (c.getValue() > bestN) { best = c.getKey(); bestN = c.getValue(); }
            d.condition = best;
            out.add(d);
        }
        return out;
    }
}
