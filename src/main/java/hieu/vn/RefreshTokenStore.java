package hieu.vn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RefreshTokenStore {

    // Class lưu thông tin mỗi refresh token
    public static class TokenEntry {
        public String token;
        public long expiresAt;
    }

    // "Database" giả bằng HashMap (key = username)
    private static final Map<String, TokenEntry> store = new HashMap<>();

    // Tạo và lưu refresh token
    public static String generate(String username) {
        String refreshToken = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000; // 7 ngày

        TokenEntry entry = new TokenEntry();
        entry.token = refreshToken;
        entry.expiresAt = expiresAt;

        store.put(username, entry);
        return refreshToken;
    }

    // Kiểm tra refresh token có hợp lệ không
    public static boolean isValid(String username, String token) {
        if (!store.containsKey(username)) return false;

        TokenEntry entry = store.get(username);
        if (!entry.token.equals(token)) return false;
        if (System.currentTimeMillis() > entry.expiresAt) return false;

        return true;
    }
}
