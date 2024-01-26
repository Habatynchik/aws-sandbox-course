package demo.custom.sdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OpenMeteoAPI {
    private final static String API_URL = "https://api.open-meteo.com/v1/forecast?";
    private final static String ERROR_MESSAGE = "Something went wrong.";

    public String getLatestForecast() {
        Map<String, String> params = new HashMap<>();
        HttpURLConnection httpURLConnection = null;

        params.put("latitude", "50.4547");
        params.put("longitude", "30.5238");
        params.put("hourly", "temperature_2m");

        try {
            String urlWithParams = API_URL.concat(ParameterStringBuilder.getParamsString(params));
            URL url = new URL(urlWithParams);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                return content.toString();
            } else {
                return ERROR_MESSAGE;
            }
        } catch (Exception e) {
            return "Exception:" + e.getMessage();
        }
    }
}
