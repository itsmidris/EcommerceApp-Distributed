import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {

    public static void main(String[] args) throws Exception {

        Path keysDirectory =
                Path.of("src", "main", "resources", "keys");

        Files.createDirectories(keysDirectory);

        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair =
                keyPairGenerator.generateKeyPair();

        String privateKey =
                "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String publicKey =
                "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        Files.writeString(
                keysDirectory.resolve("private_key.pem"),
                privateKey
        );

        Files.writeString(
                keysDirectory.resolve("public_key.pem"),
                publicKey
        );

        System.out.println();
        System.out.println("========================================");
        System.out.println("JWT RSA KEY PAIR GENERATED SUCCESSFULLY");
        System.out.println("========================================");
        System.out.println();
        System.out.println(
                "Private key: " +
                keysDirectory.resolve("private_key.pem")
        );
        System.out.println(
                "Public key : " +
                keysDirectory.resolve("public_key.pem")
        );
        System.out.println();
    }
}