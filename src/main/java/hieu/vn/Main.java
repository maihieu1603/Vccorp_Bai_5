package hieu.vn;

import static spark.Spark.*;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CacheTTL<Integer, List<Integer>> ttlCache = new CacheTTL<>(20, 10); // TTL: 20s, Idle: 10s
        PrimeCache guavaCache = new PrimeCache();

        port(8080);
         /* Chinh sua tren server github */
        // Đường dẫn dùng CacheTTL tự viết
        get("/prime/ttl", (req, res) -> {
            res.type("application/json");
            String nParam = req.queryParams("n");
            int n = Integer.parseInt(nParam);

            List<Integer> primes;
            if (ttlCache.containsKey(n)) {
                primes = ttlCache.get(n);
            } else {
                primes = generatePrimes(n);
                ttlCache.put(n, primes);
            }

            return new Gson().toJson(primes);
        });
        // Đường dẫn dùng Guava cache
        get("/prime/guava", (req, res) -> {
            res.type("application/json");
            String nParam = req.queryParams("n");
            int n = Integer.parseInt(nParam);

            List<Integer> primes = guavaCache.get(n);
            if (primes == null) {
                primes = generatePrimes(n);
                guavaCache.put(n, primes);
            }

            return new Gson().toJson(primes);
        });

        // Xem hit rate của cả hai cache
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
