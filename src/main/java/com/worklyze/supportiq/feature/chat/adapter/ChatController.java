package com.worklyze.supportiq.feature.chat.adapter;

import com.worklyze.supportiq.feature.chat.domain.usecases.AskQuestionUseCase;
import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;
import com.worklyze.supportiq.feature.chat.shared.ChatRequest;
import com.worklyze.supportiq.feature.chat.shared.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AskQuestionUseCase askQuestionUseCase;

    @PostMapping
    @Operation(summary = "Ask a question based on the ingested knowledge base, optionally continuing a session for multi-turn context")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {

        ChatAnswer chatAnswer = askQuestionUseCase.execute(request.sessionId(), request.question());

        return ResponseEntity.ok(new ChatResponse(chatAnswer.answer(), chatAnswer.sessionId(), chatAnswer.images()));
    }
}
