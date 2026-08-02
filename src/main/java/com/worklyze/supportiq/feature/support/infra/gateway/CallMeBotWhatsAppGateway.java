package com.worklyze.supportiq.feature.support.infra.gateway;

import com.worklyze.supportiq.feature.support.application.gateway.SupportTicketGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Adapter concreto do {@link SupportTicketGateway} usando CallMeBot (WhatsApp gratuito).
 * <p>
 * Para funcionar, o telefone destinatario precisa ativar o CallMeBot uma vez:
 * envie a mensagem "I allow callmebot to send me messages" para +34 644 51 95 23
 * no WhatsApp. O bot respondera com a API key.
 * <p>
 * URL: https://api.callmebot.com/whatsapp.php?phone=&lt;phone&gt;&text=&lt;text&gt;&apikey=&lt;key&gt;
 */
@Component
public class CallMeBotWhatsAppGateway implements SupportTicketGateway {

    private static final String BASE_URL = "https://api.callmebot.com/whatsapp.php";

    private final String phone;
    private final String apiKey;
    private final HttpClient httpClient;

    public CallMeBotWhatsAppGateway(
            @Value("${supportiq.support.whatsapp.phone:}") String phone,
            @Value("${supportiq.support.whatsapp.api-key:}") String apiKey
    ) {
        this.phone = phone == null ? "" : phone.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return !phone.isBlank() && !apiKey.isBlank();
    }

    @Override
    public void send(String structuredMessage) {

        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Suporte WhatsApp nao configurado. Defina supportiq.support.whatsapp.phone e api-key."
            );
        }

        String url = BASE_URL
                + "?phone=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(structuredMessage, StandardCharsets.UTF_8)
                + "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException(
                        "Falha ao enviar via CallMeBot (status " + response.statusCode() + "): " + response.body()
                );
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Erro enviando mensagem WhatsApp: " + ex.getMessage(), ex);
        }
    }
}
