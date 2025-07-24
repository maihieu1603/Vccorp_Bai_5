package hieu.vn;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public class JwtUtilRaw {
    // Khóa bí mật dùng để ký token (bạn nên giữ bí mật này cẩn thận)
    private static final String SECRET = "your-secret-key";

    // Hàm mã hóa chuỗi đầu vào theo chuẩn Base64URL (dùng trong JWT)
    private static String base64urlEncode(String input) {
        // Base64 URL-safe, không padding (==) → đúng chuẩn JWT
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    // Hàm ký dữ liệu bằng thuật toán HMAC-SHA256 với secret key
    private static String hmacSha256(String data, String secret) throws Exception {
        // Tạo đối tượng HMAC sử dụng thuật toán SHA-256
        Mac hmac = Mac.getInstance("HmacSHA256");
        // Thiết lập khóa bí mật
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(keySpec);
        // Ký dữ liệu
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // Trả về chuỗi chữ ký đã mã hóa base64url
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    // Hàm tạo JWT cho username, hết hạn sau 10 phút
    public static String generateToken(String username, String ip) throws Exception {
        long exp = System.currentTimeMillis() / 1000 + 600; // 10 phút

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

        // Thêm IP vào payload
        String payloadJson = "{\"sub\":\"" + username + "\",\"exp\":" + exp + ",\"ip\":\"" + ip + "\"}";

        String header = base64urlEncode(headerJson);
        String payload = base64urlEncode(payloadJson);
        String signature = hmacSha256(header + "." + payload, SECRET);

        return header + "." + payload + "." + signature;
    }

    // Hàm xác thực JWT, trả về true nếu hợp lệ và chưa hết hạn
    public static boolean validateToken(String token, String currentIp) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        // Xác thực chữ ký
        String expectedSig = hmacSha256(header + "." + payload, SECRET);
        if (!expectedSig.equals(signature)) {
            System.out.println("❌ Signature mismatch");
            return false;
        }

        // Giải mã payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);

        // Kiểm tra thời hạn
        long expTime = Long.parseLong(payloadJson.replaceAll(".*\"exp\":(\\d+).*", "$1"));
        long now = System.currentTimeMillis() / 1000;
        if (now > expTime) {
            System.out.println("❌ Token expired");
            return false;
        }

        // Kiểm tra IP
        String tokenIp = payloadJson.replaceAll(".*\"ip\":\"([^\"]+)\".*", "$1");
        if (!tokenIp.equals(currentIp)) {
            System.out.println("❌ IP mismatch: token=" + tokenIp + ", current=" + currentIp);
            return false;
        }

        System.out.println("✅ Token valid for IP: " + currentIp);
        return true;
    }

    public static String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = parts[1];
            String payloadJson = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            return payloadJson.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
        } catch (Exception e) {
            return null;
        }
    }
}
