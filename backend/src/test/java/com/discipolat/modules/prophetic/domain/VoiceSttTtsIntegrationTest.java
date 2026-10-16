package com.discipolat.modules.prophetic.domain;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("STT/TTS — intégration avec mock serveur OpenAI-compatible")
class VoiceSttTtsIntegrationTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger transcriptionCalls = new AtomicInteger(0);
    private final AtomicInteger ttsCalls = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> lastReceivedBody = new ConcurrentHashMap<>();

    @BeforeEach
    void startMockServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/audio/transcriptions", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            transcriptionCalls.incrementAndGet();
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                lastReceivedBody.put("stt", baos.toString(StandardCharsets.UTF_8));
            }
            String json = "{\"text\":\"bonjour le monde, ceci est une transcription de test\"}";
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (var os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/v1/audio/speech", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            ttsCalls.incrementAndGet();
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                lastReceivedBody.put("tts", baos.toString(StandardCharsets.UTF_8));
            }
            byte[] fakeMp3 = new byte[]{
                'I', 'D', '3', 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x49, 0x4E, 0x46, 0x4F, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00,
                0x00, 'T', 'E', 'S', 'T', 0x00, 0x00, 0x00, 0x04, 0x00, 0x00,
                0x00, 'M', 'P', '3', ' '
            };
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(200, fakeMp3.length);
            try (var os = exchange.getResponseBody()) {
                os.write(fakeMp3);
            }
        });
        server.start();
    }

    @AfterEach
    void stopMockServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    // ---------------- TESTS STT ----------------
    @Test
    @DisplayName("Whisper transcrit un fichier audio via le mock et retourne le texte")
    void sttTranscribeReturnsTextFromMock() {
        WhisperSpeechToTextProvider provider = new WhisperSpeechToTextProvider(
                baseUrl() + "/v1", "sk-test-key-12345", "whisper-1");
        assertTrue(provider.isConfigured(), "le provider doit être configuré");
        assertEquals("whisper", provider.name());
        String transcription = provider.transcribe(
                new byte[]{0x00, 0x01, 0x02, 0x03}, "test.m4a", "fr");
        assertEquals("bonjour le monde, ceci est une transcription de test", transcription);
        assertEquals(1, transcriptionCalls.get(), "une seule appel au mock STT");
        assertTrue(lastReceivedBody.get("stt").contains("whisper-1"),
                "le modèle doit être envoyé dans le corps multipart");
        assertTrue(lastReceivedBody.get("stt").contains("fr"),
                "la langue doit être envoyée dans le corps multipart");
    }

    @Test
    @DisplayName("Whisper lève une exception si aucun audio n'est fourni")
    void sttTranscribeRejectsEmptyAudio() {
        WhisperSpeechToTextProvider provider = new WhisperSpeechToTextProvider(
                baseUrl() + "/v1", "sk-test-key-12345", "whisper-1");
        assertThrows(SpeechToTextException.class,
                () -> provider.transcribe(new byte[0], "test.m4a", "fr"));
        assertThrows(SpeechToTextException.class,
                () -> provider.transcribe(null, "test.m4a", "fr"));
    }

    @Test
    @DisplayName("Whisper lève une exception si non configuré (fail-closed)")
    void sttNotConfiguredThrows() {
        WhisperSpeechToTextProvider provider = new WhisperSpeechToTextProvider(
                "", "", "whisper-1");
        assertFalse(provider.isConfigured(), "non configuré sans URL ni clé");
        assertThrows(SpeechToTextException.class,
                () -> provider.transcribe(new byte[]{0x01}, "test.m4a", "fr"));
    }

    @Test
    @DisplayName("Whisper lève une exception si le mock est injoignable")
    void sttTranscribeHandlesUnreachableMock() {
        WhisperSpeechToTextProvider badProvider = new WhisperSpeechToTextProvider(
                "http://127.0.0.1:1", "sk-test-key-12345", "whisper-1");
        assertTrue(badProvider.isConfigured(), "URL + clé = configuré même si injoignable");
        assertThrows(SpeechToTextException.class,
                () -> badProvider.transcribe(new byte[]{0x01}, "test.m4a", "fr"));
    }

    // ---------------- TESTS TTS ----------------
    @Test
    @DisplayName("OpenAI TTS synthétise du texte en bytes MP3 via le mock")
    void ttsSynthesizeReturnsMp3BytesFromMock() {
        OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(
                baseUrl() + "/v1", "sk-test-key-tts", "tts-1", "alloy");
        assertTrue(provider.isConfigured(), "le provider TTS doit être configuré");
        assertEquals("openai-tts", provider.name());
        byte[] mp3 = provider.synthesize("Bonjour le monde", "fr", "alloy");
        assertNotNull(mp3);
        assertTrue(mp3.length > 0, "des bytes audio doivent être retournés");
        assertArrayEquals(new byte[]{'I', 'D', '3'}, Arrays.copyOf(mp3, 3),
                "le mock retourne un en-tête ID3");
        assertEquals(1, ttsCalls.get(), "une seule appel au mock TTS");
        assertTrue(lastReceivedBody.get("tts").contains("Bonjour le monde"),
                "le texte doit être envoyé dans le corps JSON");
    }

    @Test
    @DisplayName("TTS lève une exception si texte vide")
    void ttsSynthesizeRejectsEmptyText() {
        OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(
                baseUrl() + "/v1", "sk-test-key-tts", "tts-1", "alloy");
        assertThrows(TextToSpeechException.class, () -> provider.synthesize("", "fr", "alloy"));
        assertThrows(TextToSpeechException.class, () -> provider.synthesize("   ", "fr", "alloy"));
        assertThrows(TextToSpeechException.class, () -> provider.synthesize(null, "fr", "alloy"));
    }

    @Test
    @DisplayName("TTS lève une exception si non configuré (fail-closed)")
    void ttsNotConfiguredThrows() {
        OpenAiTextToSpeechProvider provider = new OpenAiTextToSpeechProvider(
                "", "", "tts-1", "alloy");
        assertFalse(provider.isConfigured(), "non configuré sans URL ni clé");
        assertThrows(TextToSpeechException.class,
                () -> provider.synthesize("Bonjour", "fr", "alloy"));
    }

    @Test
    @DisplayName("Statut des providers reflète l'état configuré")
    void statusReflectsConfiguredProviders() {
        WhisperSpeechToTextProvider stt = new WhisperSpeechToTextProvider(
                baseUrl() + "/v1", "sk-key", "whisper-1");
        OpenAiTextToSpeechProvider tts = new OpenAiTextToSpeechProvider(
                baseUrl() + "/v1", "sk-key", "tts-1", "alloy");
        assertTrue(stt.isConfigured());
        assertTrue(tts.isConfigured());
        WhisperSpeechToTextProvider sttEmpty = new WhisperSpeechToTextProvider("", "", "whisper-1");
        OpenAiTextToSpeechProvider ttsEmpty = new OpenAiTextToSpeechProvider("", "", "tts-1", "alloy");
        assertFalse(sttEmpty.isConfigured());
        assertFalse(ttsEmpty.isConfigured());
    }

    @Test
    @DisplayName("La clé API n'apparaît pas dans toString")
    void apiKeyNeverLeaksInToString() {
        WhisperSpeechToTextProvider provider = new WhisperSpeechToTextProvider(
                baseUrl() + "/v1", "sk-super-secret-key", "whisper-1");
        String s = provider.toString();
        assertFalse(s.contains("sk-super-secret-key"),
                "la clé ne doit jamais apparaître dans toString : " + s);
    }
}
