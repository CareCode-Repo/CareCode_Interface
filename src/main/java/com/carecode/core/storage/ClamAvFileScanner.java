package com.carecode.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * clamd 의 INSTREAM 명령으로 검사한다. 별도 라이브러리 없이 TCP 로 말한다.
 *
 * <p>프로토콜: {@code zINSTREAM\0} 다음에 [4바이트 길이 + 데이터] 조각들, 마지막에 길이 0.
 * 응답은 {@code stream: OK} 또는 {@code stream: <이름> FOUND}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.scan.enabled", havingValue = "true")
public class ClamAvFileScanner implements FileScanner {

    private static final int CHUNK = 64 * 1024;

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public ClamAvFileScanner(
            @Value("${app.storage.scan.clamd.host:localhost}") String host,
            @Value("${app.storage.scan.clamd.port:3310}") int port,
            @Value("${app.storage.scan.clamd.timeout-millis:10000}") int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public ScanResult scan(byte[] content) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            OutputStream out = socket.getOutputStream();
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int offset = 0; offset < content.length; offset += CHUNK) {
                int length = Math.min(CHUNK, content.length - offset);
                out.write(ByteBuffer.allocate(4).putInt(length).array());
                out.write(content, offset, length);
            }
            out.write(new byte[4]);
            out.flush();

            String reply = readReply(socket.getInputStream());
            if (reply.endsWith("OK")) {
                return ScanResult.ok();
            }
            if (reply.endsWith("FOUND")) {
                String threat = reply.replaceFirst("^stream:\s*", "").replaceFirst("\s*FOUND$", "");
                log.warn("업로드 파일에서 악성코드 탐지: {}", threat);
                return ScanResult.infected(threat);
            }
            throw new ScannerUnavailableException("clamd 응답을 해석할 수 없습니다: " + reply, null);
        } catch (IOException e) {
            throw new ScannerUnavailableException("clamd 에 연결할 수 없습니다: " + host + ":" + port, e);
        }
    }

    private static String readReply(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != 0) {
            buffer.write(b);
        }
        return buffer.toString(StandardCharsets.US_ASCII).trim();
    }
}
