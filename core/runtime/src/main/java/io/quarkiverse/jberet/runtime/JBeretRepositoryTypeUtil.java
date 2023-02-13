package io.quarkiverse.jberet.runtime;

public class JBeretRepositoryTypeUtil {
    public static String normalize(String value) {
        return value.replaceAll("-", "_").toUpperCase();
    }
}
