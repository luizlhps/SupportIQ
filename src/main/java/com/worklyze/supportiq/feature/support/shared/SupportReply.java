package com.worklyze.supportiq.feature.support.shared;

/**
 * Resposta textual do fluxo de suporte, agnostica em relacao a estrutura do
 * chat (nao carrega sessionId nem imagens). Cabe ao chamador combinar o texto
 * com sua propria representacao de resposta.
 */
public record SupportReply(String text) {
}
