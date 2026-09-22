package com.carecode.core.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 파일 앞부분(매직 넘버)이 확장자와 맞는지 확인한다.
 *
 * <p>확장자와 Content-Type 은 클라이언트가 정하는 값이라 얼마든지 속일 수 있다.
 * 예를 들어 HTML·SVG 를 {@code .png} 로 올리면 둘 다 통과하고, 브라우저가 내용을 보고
 * 문서로 해석하면 저장형 XSS 가 된다. 실제 바이트를 봐야 막을 수 있다.
 */
public final class FileSignatureValidator {

    /** 판별에 필요한 최대 길이. HEIC 의 ftyp 브랜드가 8~12 바이트에 있다. */
    public static final int HEADER_LENGTH = 16;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87 = ascii("GIF87a");
    private static final byte[] GIF89 = ascii("GIF89a");
    private static final byte[] RIFF = ascii("RIFF");
    private static final byte[] WEBP = ascii("WEBP");
    private static final byte[] FTYP = ascii("ftyp");
    private static final byte[] PDF = ascii("%PDF-");
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1");

    private FileSignatureValidator() {
    }

    /** 앞부분 바이트가 확장자가 말하는 형식과 맞으면 true. 모르는 확장자는 false. */
    public static boolean matches(String extension, byte[] header) {
        if (extension == null || header == null) {
            return false;
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> startsWith(header, 0, JPEG);
            case "png" -> startsWith(header, 0, PNG);
            case "gif" -> startsWith(header, 0, GIF87) || startsWith(header, 0, GIF89);
            case "webp" -> startsWith(header, 0, RIFF) && startsWith(header, 8, WEBP);
            case "heic" -> startsWith(header, 4, FTYP) && header.length >= 12
                    && HEIC_BRANDS.contains(new String(header, 8, 4, StandardCharsets.US_ASCII));
            case "pdf" -> startsWith(header, 0, PDF);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
