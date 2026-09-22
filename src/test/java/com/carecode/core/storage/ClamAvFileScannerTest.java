package com.carecode.core.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** clamd INSTREAM 프로토콜을 흉내 내는 가짜 서버로 요청 형식과 응답 해석을 확인한다. */
@DisplayName("ClamAV 검사기 — INSTREAM 프로토콜")
class ClamAvFileScannerTest {

    @Test
    @DisplayName("파일을 길이 접두 조각으로 보내고, OK 면 통과")
    void cleanFile() throws Exception {
        byte[] payload = new byte[150_000]; // 64KB 조각 3개로 나뉜다
        payload[149_999] = 7;
        try (FakeClamd clamd = new FakeClamd("stream: OK")) {
            FileScanner.ScanResult result = new ClamAvFileScanner("127.0.0.1", clamd.port(), 3000).scan(payload);

            assertThat(result.clean()).isTrue();
            assertThat(clamd.received()).isEqualTo(payload);
            assertThat(clamd.command()).isEqualTo("zINSTREAM");
        }
    }

    @Test
    @DisplayName("FOUND 면 위협 이름과 함께 차단")
    void infectedFile() throws Exception {
        try (FakeClamd clamd = new FakeClamd("stream: Eicar-Test-Signature FOUND")) {
            FileScanner.ScanResult result = new ClamAvFileScanner("127.0.0.1", clamd.port(), 3000)
                    .scan("X5O!P%@AP".getBytes(StandardCharsets.US_ASCII));

            assertThat(result.clean()).isFalse();
            assertThat(result.threat()).isEqualTo("Eicar-Test-Signature");
        }
    }

    @Test
    @DisplayName("연결할 수 없으면 ScannerUnavailableException")
    void unreachable() throws Exception {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        assertThatThrownBy(() -> new ClamAvFileScanner("127.0.0.1", closedPort, 1000).scan(new byte[]{1}))
                .isInstanceOf(FileScanner.ScannerUnavailableException.class);
    }

    /** 요청 한 번을 받아 조각을 모으고 정해진 응답을 돌려준다. */
    static final class FakeClamd implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0);
        private final CompletableFuture<byte[]> data = new CompletableFuture<>();
        private final CompletableFuture<String> command = new CompletableFuture<>();

        FakeClamd(String reply) throws Exception {
            CompletableFuture.runAsync(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    ByteArrayOutputStream cmd = new ByteArrayOutputStream();
                    int b;
                    while ((b = in.read()) != 0) {
                        cmd.write(b);
                    }
                    command.complete(cmd.toString(StandardCharsets.US_ASCII));
                    ByteArrayOutputStream body = new ByteArrayOutputStream();
                    int length;
                    while ((length = in.readInt()) > 0) {
                        body.write(in.readNBytes(length));
                    }
                    data.complete(body.toByteArray());
                    OutputStream out = socket.getOutputStream();
                    out.write((reply + "\0").getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (Exception e) {
                    data.completeExceptionally(e);
                    command.completeExceptionally(e);
                }
            });
        }

        int port() {
            return server.getLocalPort();
        }

        byte[] received() throws Exception {
            return data.get(3, TimeUnit.SECONDS);
        }

        String command() throws Exception {
            return command.get(3, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            server.close();
        }
    }
}
