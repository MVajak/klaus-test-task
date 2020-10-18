package klaus.demo.util;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class ResourceReader {

    private ResourceReader() {
        // utility class
    }

    public static String getResourceAsString(String absolutePath) {
        try {
            return new String(new ClassPathResource(absolutePath).getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getResourceAsString(String relativePath, Class<?> relativeFrom) {
        try {
            return new String(new ClassPathResource(relativePath, relativeFrom).getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}