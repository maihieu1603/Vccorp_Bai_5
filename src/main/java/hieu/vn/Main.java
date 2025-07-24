package hieu.vn;

import static spark.Spark.*;
import com.google.gson.Gson;

import java.util.*;

public class Main {
    private static final CacheTTL<Integer, List<Integer>> ttlCache = new CacheTTL<>(20, 10); // TTL: 20s, Idle: 10s
    private static final PrimeCache guavaCache = new PrimeCache();

    public static void main(String[] args) {
        ipAddress("0.0.0.0");
        port(8080);

        // Đăng nhập (giả lập: username bất kỳ, không cần mật khẩu)
        post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String ip = req.ip();

            String accessToken = JwtUtilRaw.generateToken(username, ip);
            String refreshToken = RefreshTokenStore.generate(username);

            res.type("application/json");
            return new Gson().toJson(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));
        });

        // Middleware kiểm tra token cho mọi route /prime/*
        before("/prime/*", (req, res) -> {
            String ip = req.ip();
            String accessToken = req.headers("Authorization");
            String refreshToken = req.headers("X-Refresh-Token");

            if (accessToken == null || accessToken.isEmpty()) {
                halt(401, "❌ Missing access token. Please login.\n");
            }

            boolean accessOk = JwtUtilRaw.validateToken(accessToken, ip);
            if (!accessOk) {
                // Thử dùng refresh token nếu có
                String username = JwtUtilRaw.extractUsername(accessToken); // trích từ payload đã hết hạn
                if (refreshToken != null && RefreshTokenStore.isValid(username, refreshToken)) {
                    // Gia hạn access token mới
                    String newAccessToken = JwtUtilRaw.generateToken(username, ip);
                    String newRefreshToken = RefreshTokenStore.generate(username);
                    res.header("X-New-Access-Token", newAccessToken);
                    res.header("X-New-Refresh-Token", newRefreshToken);
                } else {
                    halt(401, "❌ Token expired. Please login again.\n");
                }
            }
        });

        // Dùng CacheTTL tự viết
        get("/prime/ttl", (req, res) -> {
            res.type("application/json");
            int n = Integer.parseInt(req.queryParams("n"));

            List<Integer> primes;
            if (ttlCache.containsKey(n)) {
                primes = ttlCache.get(n);
            } else {
                primes = generatePrimes(n);
                ttlCache.put(n, primes);
            }

            return new Gson().toJson(primes);
        });

        // Dùng Guava cache
        get("/prime/guava", (req, res) -> {
            res.type("application/json");
            int n = Integer.parseInt(req.queryParams("n"));

            List<Integer> primes = guavaCache.get(n);
            if (primes == null) {
                primes = generatePrimes(n);
                guavaCache.put(n, primes);
            }

            return new Gson().toJson(primes);
        });

        get("/cache/hit-rate", (req, res) -> {
            return "TTL Cache hit rate: " + ttlCache.getHitRate() + "%\n";
        });
    }

    public static List<Integer> generatePrimes(int limit) {
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (isPrime(i)) {
                primes.add(i);
            }
        }
        return primes;
    }

    public static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
