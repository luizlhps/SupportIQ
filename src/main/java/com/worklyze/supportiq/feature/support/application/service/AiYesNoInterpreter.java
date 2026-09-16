package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Classificador de respostas afirmativas/negativas utilizando IA.
 * Interpreta variações coloquiais, sarcasmo e respostas ambíguas com
 * maior precisão que a heurística baseada em keywords.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiYesNoInterpreter {

    private static final String SYSTEM_PROMPT = """
            Você é um classificador de intenção. Analise a mensagem do usuário e classifique como:
            
            YES - O usuário está confirmando, aceitando ou concordando. Exemplos:
              - "sim", "ok", "pode ser", "acho que sim", "claro", "com certeza", "manda ver"
              - "tá bom", "beleza", "fechado", "isso", "confirmo", "pode enviar"
            
            NO - O usuário está negando, recusando ou cancelando. Exemplos:
              - "não", "nao", "cancela", "deixa pra lá", "melhor não", "agora não"
              - "não quero", "prefiro não", "esquece", "para", "desisto"
            
            UNCLEAR - A mensagem não indica claramente sim ou não, ou é uma pergunta/comentário:
              - Perguntas sobre o processo
              - Pedidos de esclarecimento
              - Mudanças de assunto
              - Respostas ambíguas demais
            
            Responda APENAS com uma palavra: YES, NO ou UNCLEAR
            Não adicione explicações ou pontuação.
            """;

    private final AiModelRegistry aiModelRegistry;
    private final YesNoInterpreter heuristicInterpreter;

    /**
     * Classifica a resposta do usuário usando IA, com fallback para heurística
     * em caso de erro ou timeout.
     */
    public YesNoInterpreter.Answer classify(AiProvider provider, String text) {
        if (text == null || text.isBlank()) {
            return YesNoInterpreter.Answer.UNCLEAR;
        }

        // Primeiro tenta heurística (rápido e sem custo)
        YesNoInterpreter.Answer heuristicResult = heuristicInterpreter.classify(text);
        if (heuristicResult != YesNoInterpreter.Answer.UNCLEAR) {
            return heuristicResult;
        }

        // Se heurística não resolve, usa IA
        try {
            return classifyWithAi(provider, text);
        } catch (Exception e) {
            log.warn("Falha na classificação via IA, usando UNCLEAR: {}", e.getMessage());
            return YesNoInterpreter.Answer.UNCLEAR;
        }
    }

    private YesNoInterpreter.Answer classifyWithAi(AiProvider provider, String text) {
        ChatModel model = aiModelRegistry.chat(provider);

        ChatResponse response = model.chat(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(text)
        ));

        String answer = response.aiMessage().text();
        if (answer == null) {
            return YesNoInterpreter.Answer.UNCLEAR;
        }

        String normalized = answer.trim().toUpperCase().replaceAll("[^A-Z]", "");

        return switch (normalized) {
            case "YES" -> YesNoInterpreter.Answer.YES;
            case "NO" -> YesNoInterpreter.Answer.NO;
            default -> YesNoInterpreter.Answer.UNCLEAR;
        };
    }
}
