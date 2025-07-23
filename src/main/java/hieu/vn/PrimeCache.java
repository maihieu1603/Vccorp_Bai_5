package hieu.vn;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PrimeCache {
    private final Cache<Integer, List<Integer>> cache;

    public PrimeCache() {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)  // Xóa sau 20s kể từ khi ghi
                .expireAfterAccess(10, TimeUnit.SECONDS) // Xóa sau 10s không có request
                .build();
    }

    public List<Integer> get(int n) {
        return cache.getIfPresent(n);
    }

    public void put(int n, List<Integer> primes) {
        cache.put(n, primes);
    }
}
