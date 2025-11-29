package org.Kloppie74.giftCards.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GiftCardService {

    public boolean createGiftCard(String apiUrl, String code, double amount, String expireDate, String note, String username) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String params = "code=" + encode(code) +
                    "&balance=" + encode(Double.toString(amount)) +
                    "&expire=" + encode(expireDate) +
                    "&note=" + encode(note) +
                    "&username=" + encode(username);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public GiftCardInfo fetchGiftCardInfo(String code, String baseUrl) {
        try {
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            String apiUrl = baseUrl + "/api/cart/getGift";

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String body = "{\"gift\":\"" + escapeJson(code) + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            InputStream is = responseCode >= 200 && responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            String response = sb.toString();

            boolean status = response.contains("\"status\":true");
            if (status) {
                double endBalance = 0;
                String currency = "";
                int balIdx = response.indexOf("\"end_balance\":");
                if (balIdx != -1) {
                    int start = balIdx + 14;
                    int end = response.indexOf(",", start);
                    if (end == -1) end = response.length();
                    String bal = response.substring(start, end).replaceAll("[^0-9.\\-]", "");
                    try {
                        endBalance = Double.parseDouble(bal);
                    } catch (Exception ignored) {}
                }
                int currIdx = response.indexOf("\"currency\":\"");
                if (currIdx != -1) {
                    int start = currIdx + 12;
                    int end = response.indexOf("\"", start);
                    if (end != -1) currency = response.substring(start, end);
                }
                return new GiftCardInfo(true, endBalance, currency, null);
            } else {
                String errorMsg = null;
                int errIdx = response.indexOf("\"error\":\"");
                if (errIdx != -1) {
                    int start = errIdx + 9;
                    int end = response.indexOf("\"", start);
                    if (end != -1) errorMsg = response.substring(start, end);
                }
                return new GiftCardInfo(false, 0, "", errorMsg);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String encode(String input) {
        if (input == null) return "";
        return input.replace(" ", "%20").replace("\n", "%0A");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class GiftCardInfo {
        private final boolean valid;
        private final double endBalance;
        private final String currency;
        private final String errorMessage;

        public GiftCardInfo(boolean valid, double endBalance, String currency, String errorMessage) {
            this.valid = valid;
            this.endBalance = endBalance;
            this.currency = currency;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public double getEndBalance() { return endBalance; }
        public String getCurrency() { return currency; }
        public String getErrorMessage() { return errorMessage; }
    }
}