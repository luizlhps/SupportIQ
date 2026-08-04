package com.worklyze.supportiq.feature.support.infra.gateway;

import com.worklyze.supportiq.feature.support.application.gateway.SupportTicketGateway;
import com.worklyze.supportiq.feature.support.shared.SupportExceptionCode;
import com.worklyze.supportiq.shared.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Adapter concreto do {@link SupportTicketGateway} que gera links wa.me
 * (https://wa.me/&lt;phone&gt;?text=&lt;message&gt;) para o usuario enviar
 * a mensagem estruturada pelo proprio WhatsApp.
 * <p>
 * So precisa do telefone destinatario no formato internacional sem "+"
 * (ex: 5541999999999). Nao requer API key nem cadastro em servico externo.
 */
@Component
public class WaMeSupportGateway implements SupportTicketGateway {

    private static final String BASE_URL = "https://wa.me/";

    private final String phone;

    public WaMeSupportGateway(
            @Value("${supportiq.support.whatsapp.phone:}") String phone
    ) {
        this.phone = phone == null ? "" : phone.trim();
    }

    @Override
    public boolean isConfigured() {
        return !phone.isBlank();
    }

    @Override
    public String generateLink(String structuredMessage) {
        if (!isConfigured()) {
            throw new BadRequestException(SupportExceptionCode.WHATSAPP_NOT_CONFIGURED);
        }

        String encodedText = URLEncoder.encode(structuredMessage, StandardCharsets.UTF_8);
        return BASE_URL + phone + "?text=" + encodedText;
    }
}
