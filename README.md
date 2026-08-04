# SupportIQ

Sistema de chatbot com RAG (Retrieval-Augmented Generation) para suporte ao usuário. Ingere documentos PDF, gera embeddings vetoriais e responde perguntas usando LLM com recuperação semântica. Quando a IA não consegue resolver, oferece encaminhamento automático para suporte humano via WhatsApp.

## Arquitetura

O projeto segue **Clean Architecture** combinada com **Domain-Driven Design (DDD)**, organizada por módulos de domínio (features) e separada em camadas com responsabilidades distintas.

### Estrutura de Camadas

```
src/main/java/com/worklyze/supportiq/
├── config/                          ← Configurações de infraestrutura
│   ├── ai/                          ← AiModelRegistry, AiProvider, VectorStoreRegistry
│   ├── exception/                   ← GlobalExceptionHandler, RestErrorMessage
│   ├── objectmapper/                ← ObjectMapperConfig
│   └── pdfparser/                   ← PdfParserConfig
└── feature/                         ← Módulos de domínio (DDD)
    ├── chat/                        ← Chat com RAG
    │   ├── adapter/                 ← ChatController (REST)
    │   ├── application/
    │   │   ├── service/             ← RagChatService, ChatSessionMemoryStore
    │   │   └── usecase/             ← AskQuestionUseCaseImpl
    │   ├── domain/
    │   │   └── usecases/            ← AskQuestionUseCase (interface)
    │   └── shared/                  ← ChatRequest, ChatAnswer, ChatResponse (DTOs)
    ├── ingestion/                   ← Ingestão de PDFs
    │   ├── adapter/                 ← IngestionController (REST)
    │   ├── application/
    │   │   ├── service/             ← DocumentSplitter, PdfDocumentParser, ImageStorageService
    │   │   └── usecase/             ← PdfIngestionUseCaseImpl
    │   ├── domain/
    │   │   └── usecases/            ← PdfIngestionUseCase (interface)
    │   ├── infra/
    │   │   └── repository/          ← PgVectorKnowledgeRepository
    │   └── shared/                  ← DocumentParser, ParsedDocument, EmbeddedChunk
    ├── embedding/                   ← Geração de embeddings
    │   ├── KnowledgeRepository.java ← Interface (porta)
    │   └── KnowledgeEmbeddingService.java
    └── support/                     ← Fluxo de suporte humano
        ├── application/
        │   ├── gateway/             ← SupportTicketGateway (interface/porta)
        │   └── service/             ← SupportFlowHandler, StructuredMessageGenerator,
        │                              SupportFlowSessionStore, YesNoInterpreter
        ├── infra/
        │   └── gateway/             ← WaMeSupportGateway (adapter concreto)
        └── shared/                  ← SupportFlowState (enum), SupportReply
```

### Princípios Aplicados

- **Separação de Responsabilidades**: cada camada tem um papel bem definido — `adapter` (HTTP), `application` (use cases e serviços), `domain` (interfaces/contratos), `infra` (implementações técnicas).
- **Inversão de Dependência (DIP)**: use cases dependem de interfaces, não de implementações. `KnowledgeRepository` e `SupportTicketGateway` são interfaces implementadas na camada de infraestrutura.
- **Use Cases específicos**: cada operação tem seu próprio use case testável, evitando "God Classes".
- **DTOs padronizados**: records de entrada e saída em cada feature (`ChatRequest`, `ChatAnswer`, `ChatResponse`, `SupportReply`).
- **Modularização por domínio (DDD)**: features autossuficientes (`chat`, `ingestion`, `embedding`, `support`), cada uma com suas próprias camadas.
- **Abertura para extensão (OCP)**: novos providers de IA podem ser adicionados sem alterar use cases.

## Features

### Chat com RAG (`feature/chat`)

Pipeline de resposta com recuperação semântica:

1. Recebe a pergunta do usuário via `POST /v1/chat`
2. Gera embedding da pergunta
3. Busca trechos relevantes no vector store (PgVector)
4. Constrói prompt com contexto recuperado + histórico da conversa
5. Chama o LLM e retorna a resposta (com imagens relacionadas, se houver)
6. Se a IA emitir o marcador `[OFFER_SUPPORT]`, oferece suporte humano automaticamente

### Ingestão de PDFs (`feature/ingestion`)

Pipeline de indexação de documentos:

1. Recebe upload via `POST /v1/ingestion/pdf`
2. Extrai texto e imagens do PDF (PDFBox)
3. Divide o texto em chunks (DocumentSplitter)
4. Gera embeddings via modelo configurado
5. Armazena no vector store (PgVector)
6. Reingestão automática: remove embeddings e imagens antigas antes de gravar novas

### Suporte Humano (`feature/support`)

Máquina de estados para encaminhamento ao suporte:

```
NORMAL → AWAITING_SUPPORT_CONFIRMATION → AWAITING_NAME → AWAITING_MESSAGE_CONFIRMATION → envio
```

1. IA detecta que não pode resolver e oferece suporte
2. Usuário confirma interesse
3. Sistema pede o nome do usuário
4. Gera rascunho de mensagem estruturada (com histórico da conversa)
5. Usuário confirma, ajusta ou cancela
6. Gera link `wa.me` para envio pelo WhatsApp

### Embeddings (`feature/embedding`)

Serviço de geração de embeddings abstraído por provider, com interface `KnowledgeRepository` para persistência e busca vetorial.

## Stack

| Categoria | Tecnologia |
|-----------|-----------|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1 |
| LLM / RAG | LangChain4j 1.11 |
| Providers de IA | Ollama, OpenAI, Google Gemini |
| Vector Store | PostgreSQL + pgvector |
| Parse de PDF | Apache PDFBox 3.0 |
| Migração | Flyway |
| Documentação API | springdoc-openapi (Swagger UI) |
| Build | Maven |

## Pré-requisitos

- Java 25+
- Docker e Docker Compose
- (Opcional) API key da OpenAI e/ou Google Gemini

## Configuração

1. Copie o arquivo de exemplo de variáveis de ambiente:

```bash
cp .env.example .env
```

2. Ajuste os valores conforme necessário. Para usar OpenAI ou Gemini, defina as API keys:

```env
OPENAI_API_KEY=sk-...
GEMINI_API_KEY=AIza...
```

3. Suba os serviços de infraestrutura (PostgreSQL + pgvector e Ollama):

```bash
docker compose up -d
```

O Ollama fará o pull automático dos modelos `nomic-embed-text` (embedding) e `llama3.2` (chat) no primeiro startup.

4. (Opcional) Configure o telefone de suporte para o fluxo de WhatsApp:

```env
SUPPORT_WHATSAPP_PHONE=5541999999999
```

## Execução

```bash
./mvnw spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

## Endpoints

### `POST /v1/chat`

Envia uma pergunta ao chatbot com RAG.

```json
{
  "question": "Como resetar minha senha?",
  "sessionId": "opcional-uuid",
  "provider": "ollama"
}
```

Resposta:

```json
{
  "answer": "Para resetar sua senha...",
  "sessionId": "uuid-da-sessao",
  "images": ["/images/doc1-page2.png"]
}
```

### `POST /v1/ingestion/pdf`

Faz upload de um PDF para indexação (multipart/form-data).

```
curl -X POST http://localhost:8080/v1/ingestion/pdf \
  -F "file=@documento.pdf" \
  -F "provider=ollama"
```

### Swagger UI

Disponível em `http://localhost:8080/swagger-ui.html`.

## Providers de IA

O sistema suporta múltiplos providers simultaneamente. O provider padrão é definido por `supportiq.ai.default-provider` (default: `ollama`).

| Provider | Chat Model | Embedding Model | Requer API Key |
|----------|-----------|-----------------|----------------|
| Ollama | llama3.2 | nomic-embed-text | Não (local) |
| OpenAI | gpt-4o-mini | text-embedding-3-small | Sim |
| Gemini | gemini-2.0-flash | gemini-embedding-001 | Sim |

Cada request pode especificar o provider via parâmetro `provider`. Se omitido, usa o padrão configurado.

## Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_HOST` | localhost | Host do PostgreSQL |
| `DB_PORT` | 5433 | Porta do PostgreSQL |
| `DB_NAME` | supportiq | Nome do banco |
| `DB_USER` | supportiq | Usuário do banco |
| `DB_PASSWORD` | supportiq | Senha do banco |
| `OLLAMA_BASE_URL` | http://localhost:11434 | URL do Ollama |
| `OLLAMA_CHAT_MODEL` | llama3.2 | Modelo de chat do Ollama |
| `OLLAMA_EMBEDDING_MODEL` | nomic-embed-text | Modelo de embedding do Ollama |
| `AI_DEFAULT_PROVIDER` | ollama | Provider padrão |
| `OPENAI_API_KEY` | (vazio) | API key da OpenAI |
| `GEMINI_API_KEY` | (vazio) | API key do Google Gemini |
| `CHAT_MAX_RESULTS` | 5 | Número máximo de trechos recuperados |
| `CHAT_MEMORY_MAX_MESSAGES` | 20 | Máximo de mensagens na memória da sessão |
| `IMAGE_STORAGE_DIR` | ./data/images | Diretório de armazenamento de imagens |
| `SUPPORT_WHATSAPP_PHONE` | (vazio) | Telefone de suporte (formato internacional sem "+") |
