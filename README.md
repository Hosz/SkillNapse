# 🧠 SkillNapse API

> **Plataforma de Alta Performance para Gestão, Orquestração e Execução de Rotinas de Estudos com Inteligência Artificial**  
> Um projeto do ecossistema **KyoFoundation** | Criado por **Murilo Henrique (Hokyozu)**

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Spring AI](https://img.shields.io/badge/Spring_AI-Google_Gemini_%2F_Groq-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://spring.io/projects/spring-ai)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![JWT](https://img.shields.io/badge/JWT-Stateless_Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)

---

## 📌 Sumário

1. [Visão Geral](#-visão-geral)
2. [Principais Recursos](#-principais-recursos)
3. [Arquitetura de Software (Padrão Kyofuse)](#-arquitetura-de-software-padrão-kyofuse)
4. [Módulos de Domínio](#-módulos-de-domínio)
5. [Orquestração Resiliente de Inteligência Artificial](#-orquestração-resiliente-de-inteligência-artificial)
6. [Stack Tecnológica](#-stack-tecnológica)
7. [Instalação e Execução](#-instalação-e-execução)
8. [Variáveis de Ambiente (.env)](#-variáveis-de-ambiente-env)
9. [Documentação de Endpoints (Swagger)](#-documentação-de-endpoints-swagger)
10. [Padrões de Código e Diretrizes](#-padrões-de-código-e-diretrizes)
11. [Licença e Autoria](#-licença-e-autoria)

---

## 🎯 Visão Geral

O **SkillNapse** é uma API RESTful de alta performance projetada para resolver os gargalos de planejamento e execução de estudos para vestibulares e concursos públicos. 

Integrando inteligência artificial como motor de diagnóstico e personalização, a plataforma permite mapear estruturas complexas de conhecimento (extraídas diretamente de editais em PDF ou cadastradas manualmente) e transformá-las em um cronograma adaptativo de execução com:
- **Cronograma Híbrido** com *daily overrides* (imprevistos diários sem corromper a rotina semanal).
- **Simulados Adaptativos** gerados por IA para atacar lacunas de conhecimento comprovadas por dados reais de tentativas.
- **Repetição Espaçada (SRS)** baseada no algoritmo **SuperMemo SM-2** para fixação de longo prazo.
- **Laboratório de Redação** com auditoria textual rigorosa em 5 competências (padrão ENEM/bancas oficiais).
- **Workpad Integrado**: rastreamento de horas líquidas com Pomodoro e lousa vetorial (*Canvas*) persistida.

---

## 🚀 Principais Recursos

- **Hierarquia de Conhecimento**: Planos de estudo divididos em matérias, tópicos e subtópicos recursivos, com cálculo de proficiência dinâmica e peso de edital.
- **Parser Inteligente de Editais**: Leitura e extração de conteúdo programático de PDFs com IA, transformando o edital em árvore de tópicos estruturada em segundos.
- **Algoritmo SM-2 (Flashcards)**: Cálculo dinâmico de fator de facilidade (*ease factor*), repetições consecutivas e intervalo ótimo entre revisões.
- **Motor de Avaliação Dissertativa**: Análise cirúrgica de textos com detecção de desvios gramaticais, densidade semântica, repertório sociocultural legitimado e proposta de intervenção detalhada em 5 componentes.
- **Multi-Provedor de IA com Contingência**: Suporte a Google Gemini como provedor primário com fallback transparente para Groq Cloud, OpenRouter, Cerebras ou contas secundárias, prevenindo interrupções por estouro de cota (HTTP 429).
- **Cancelamento Assíncrono de Operações**: Possibilidade de cancelar chamadas pesadas de IA em tempo de execução via sessão do usuário.

---

## 🏛 Arquitetura de Software (Padrão Kyofuse)

A aplicação segue rigorosamente o padrão **Kyofuse** (KyoFoundation), adotando alta coesão e baixo acoplamento através de uma organização modular por domínios de negócio:

```
com.kyofoundation.skillnapse.modules.<dominio>/
│
├── controller/         # Thin Controllers: validações @Valid, delega para Service e retorna ResponseEntity
├── service/            # Orquestração pura: sem mappers inline, sem queries repetidas, sem if(...) throw
├── finder/             # @Component *Finder: busca por ID/chave única e lança ResourceNotFoundException
├── validator/          # @Component *Validator: validações de regras de negócio (BadRequest / Conflict)
├── mapper/             # @Component *Mapper: métodos estáticos convertendo Entidade <-> Record DTO
├── support/            # Componentes de suporte (cálculos analíticos, orquestração de IA, parse de PDF)
├── repository/         # Spring Data JPA Repository
├── entity/             # Entidades JPA com Lombok (@Data, @Builder, @EqualsAndHashCode(of = "id"))
├── enums/              # Enumerações de domínio (pacote 'enums' no plural)
└── dto/                # Records Java imutáveis (Requests, Responses e Payloads de IA)
```

### Princípios Chave:
1. **Single Responsibility Principle (SRP)**: Cada classe desempenha uma única função no ciclo de vida da requisição.
2. **DTOs Imutáveis com Records**: Todos os contratos de entrada e saída utilizam `record` do Java 21.
3. **Mappers Estáticos**: Conversões centralizadas e invocadas via `*Mapper.metodo(...)` sem poluir as services com instanciações manuais.
4. **Finders Especializados**: Tratamento unificado de `Optional` que encapsula o repositório e lança `ResourceNotFoundException`.
5. **Auditoria Temporal com `Instant`**: Datas e carimbos de tempo utilizam exclusivamente `java.time.Instant` (UTC) mapeados para colunas `TIMESTAMPTZ` do PostgreSQL.

---

## 📦 Módulos de Domínio

| Módulo | Descrição | Principais Recursos |
| :--- | :--- | :--- |
| `auth` | Autenticação e Usuários | Login, registro, perfil, JWT com access token e refresh token rotativo no banco. |
| `planoestudo` | Árvore de Conhecimento | Gestão de Planos, Matérias, Tópicos e Subtópicos com cálculo de proficiência e peso. |
| `edital` | Processamento de Editais | Upload de PDF, extração via IA de conteúdo programático e cargos com pré-visualização. |
| `cronograma` | Cronograma Híbrido | Templates semanais cíclicos, daily overrides por data, auto-agendamento e blocos de revisão. |
| `sessao` | Registro de Estudos | Sessões de estudo consolidadas, contagem de horas líquidas e resumo do dia. |
| `canvas` | Rascunhos Vetoriais | Lousa livre por tópico com persistência de traços, formas e cores em formato JSON. |
| `flashcard` | Repetição Espaçada | Baralhos, cards com algoritmo SM-2 e geração automática de flashcards por IA. |
| `questao` | Questões e Simulados | Acervo de questões, simulados adaptativos gerados dinamicamente e registro de tentativas. |
| `redacao` | Laboratório de Redação | Temas manuais/oficiais, submissão de dissertações e correção analítica via IA (C1–C5). |
| `gamificacao` | Engajamento | Monitoramento de ofensivas (*streaks* diárias) e metas diárias de horas líquidas. |
| `desempenho` | Análise e Diagnósticos | Matriz de proficiência e diagnóstico de lacunas prioritárias baseado em dados de estudo reais. |
| `ai` | Orquestração de IA | Gerenciamento de provedores (Gemini, Groq), contingência, cotas e cancelamento assíncrono. |

---

## 🤖 Orquestração Resiliente de Inteligência Artificial

O módulo `ai` implementa o padrão **Bring Your Own Key (BYOK)** e uma esteira de resiliência automatizada:

```
Requisição do Usuário (ex: Gerar Simulado / Corrigir Redação)
          │
          ▼
   [AiOrchestratorService]
          │
          ├──> 1. Provedor Primário (Google Gemini Flash)
          │       └── Se Sucesso: Retorna Payload Estruturado
          │
          └──> 2. Falha / Timeout / Cota Esgotada (HTTP 429)
                  │
                  ▼
          [AiFallbackClientSupport]
                  ├──> Provedor de Fallback 1 (Groq Cloud / LPU gpt-oss-120b)
                  ├──> Provedor de Fallback 2 (Segunda Chave Gemini)
                  └──> Provedor de Fallback 3 (OpenRouter / Modelos Livres)
                          └── Se Todos Falharem: Lança AiQuotaExceededException
```

- **Resiliência a Formatos**: Mecanismos de sanitização automática de JSON removem blocos markdown (````json ... ````) e corrigem pequenas inconsistências de nomenclatura em lotes gerados.
- **Cancelamento Ativo**: O endpoint `POST /auth/cancelar-tarefa` permite que o cliente frontend aborte qualquer geração pesada em andamento.

---

## 🛠 Stack Tecnológica

- **Linguagem**: Java 21 LTS
- **Framework Principal**: Spring Boot 4.x
- **Persistência**: Spring Data JPA & Hibernate
- **Banco de Dados**: PostgreSQL 16
- **Controle de Versão de Esquema**: Flyway Database Migrations
- **Segurança**: Spring Security & JWT (HMAC-SHA256) com BCrypt
- **Inteligência Artificial**: Spring AI (Google GenAI Gemini) e cliente REST de contingência compatível com OpenAI/Groq
- **Documentação de API**: SpringDoc OpenAPI 3.x (Swagger UI)
- **Utilitários**: Lombok, Jackson (Core, Databind), Apache PDFBox

---

## 💻 Instalação e Execução

### Pré-requisitos
- **Java JDK 21+** instalado e configurado no `PATH`
- **Maven 3.9+** (ou utilizar o wrapper `./mvnw` incluso)
- **Docker & Docker Compose** (opcional, para rodar o banco de dados)
- Chave de API de IA (ex: [Google AI Studio](https://aistudio.google.com/) ou [Groq Cloud Console](https://console.groq.com/))

### 1. Clonar o Repositório
```bash
git clone https://github.com/Hosz/SkillNapse-API.git
cd SkillNapse-API
```

### 2. Iniciar o Banco de Dados com Docker
O projeto inclui um `docker-compose.yml` pré-configurado com PostgreSQL 16:
```bash
docker compose up -d
```
> O banco será exposto na porta `5432` com usuário `postgres` e senha `postgres`.

### 3. Configurar as Variáveis de Ambiente
Crie um arquivo `.env` na raiz do projeto (ou copie do `.env.example`):
```bash
cp .env.example .env
```
Edite o arquivo `.env` adicionando sua chave do Gemini e/ou Groq:
```env
JWT_SECRET=sua_chave_secreta_jwt_base64_ou_string_longa
GEMINI_API_KEY=sua_chave_do_google_ai_studio
AI_FALLBACK_ENABLED=true
AI_FALLBACK_PROVIDER=groq
AI_FALLBACK_API_KEY=sua_chave_da_groq_aqui
```

### 4. Executar a Aplicação
Compile e inicie a API utilizando o Maven Wrapper:
```bash
# No Linux/macOS:
./mvnw spring-boot:run

# No Windows:
mvnw.cmd spring-boot:run
```

A API estará rodando por padrão em `http://localhost:8080`.

---

## ⚙️ Variáveis de Ambiente (.env)

| Variável | Padrão | Descrição |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | Host do banco PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `skillnapse` | Nome da base de dados |
| `DB_USER` | `postgres` | Usuário do banco de dados |
| `DB_PASSWORD` | `postgres` | Senha do banco de dados |
| `JWT_SECRET` | *(Obrigatório)* | Segredo de 256 bits para assinatura dos tokens JWT |
| `AI_PROVIDER` | `gemini` | Provedor principal de IA (`gemini`, `openai`, `ollama`) |
| `GEMINI_API_KEY` | - | Chave do Google Gemini (Google AI Studio) |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Modelo do provedor principal |
| `AI_FALLBACK_ENABLED`| `true` | Habilita a esteira de contingência multi-provedor |
| `AI_FALLBACK_PROVIDER`| `groq` | Provedor secundário preferencial (`groq` ou `gemini`) |
| `AI_FALLBACK_BASE_URL`| `https://api.groq.com/openai/v1` | URL base da API compatível com OpenAI |
| `AI_FALLBACK_API_KEY`| - | Chave de acesso do provedor de fallback |
| `AI_FALLBACK_MODEL` | `openai/gpt-oss-120b` | Modelo para as chamadas de contingência |
| `GEMINI_FALLBACK_API_KEY` | - | Chave de uma conta secundária do Google Gemini |

---

## 📖 Documentação de Endpoints (Swagger)

Com a aplicação em execução, acesse a documentação interativa e execute requisições via Swagger UI:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### Principais Rotas da API:

```http
# Autenticação
POST /auth/registrar               # Cadastro de novo estudante
POST /auth/login                   # Autenticação e emissão de JWT
POST /auth/refresh                 # Renovação do Access Token via Refresh Token
GET  /auth/me                      # Dados da sessão e perfil ativo
POST /auth/cancelar-tarefa         # Cancelamento de requisições de IA ativas

# Planos de Estudo e Conhecimento
GET    /planos                     # Listar planos de estudo do usuário
GET    /planos/ativo               # Obter o plano de estudo principal
POST   /planos                     # Criar novo plano de estudo
POST   /materias                   # Adicionar matéria ao plano
POST   /topicos                    # Criar tópicos e subtópicos

# Editais e Processamento por IA
POST /editais/upload               # Upload do PDF do edital
POST /editais/processar            # Extração automatizada via IA para rascunho
POST /editais/confirmar            # Converter edital confirmado em plano de estudos

# Cronograma e Agendamento
GET  /cronograma/templates         # Obter template semanal cíclico
POST /cronograma/templates/bloco   # Adicionar bloco de horário ao template
GET  /cronograma/diario            # Obter grade de um dia específico (com overrides)
POST /cronograma/auto-agendar      # Orquestrar alocação inteligente de tópicos via IA

# Sessões e Pomodoro
POST /sessoes                      # Registrar sessão de estudo concluída (horas líquidas)
GET  /sessoes/resumo-hoje          # Total de segundos e sessões realizadas no dia

# Flashcards & Repetição Espaçada (SM-2)
GET  /baralhos                     # Listar baralhos de flashcards
POST /flashcards/revisar           # Enviar nota de avaliação (0 a 5) e calcular próximo intervalo
POST /flashcards/gerar-ia          # Gerar lote de flashcards a partir de um tópico

# Questões & Simulados Adaptativos
GET  /questoes                     # Banco de questões
POST /simulados/adaptativo/gerar   # Gerar simulado sob medida focado em lacunas
POST /simulados/{id}/tentativas    # Submeter respostas e calcular proficiência

# Laboratório de Redação
GET    /redacao/temas              # Listar temas de redação disponíveis
POST   /redacao/temas/manual       # Criar tema de redação manualmente
POST   /redacao/submissoes/corrigir-ia # Submeter texto para correção detalhada por IA
DELETE /redacao/submissoes/{id}    # Excluir item do histórico de redações

# Gamificação e Desempenho
GET /gamificacao/ofensiva          # Consultar streak de dias consecutivos
GET /desempenho/resumo             # Métricas gerais de proficiência
GET /desempenho/lacunas-prioritarias # Alertas de lacunas em tópicos já estudados
```

---

## 📐 Padrões de Código e Diretrizes

O repositório adota regras estritas de qualidade de código estabelecidas no documento [`GEMINI.md`](./GEMINI.md):

- **Sem métodos privados de regra de negócio em services**: cálculos ou lógica auxiliar devem residir em classes `*Support`.
- **Sem `if(...) throw` em services**: todas as regras de validação são delegadas para `@Component *Validator`.
- **Sem instanciar builders ou entidades em services**: conversões ocorrem exclusivamente via `*Mapper`.
- **Commits Convencionais em Português**:
  - `feat(modulo): nova funcionalidade`
  - `fix(modulo): correcao de erro`
  - `refactor(modulo): refatoracao sem impacto em regras`
  - `docs(modulo): alteracao em documentacao`

---

## 👨‍💻 Licença e Autoria

Desenvolvido com excelência por **Murilo Henrique Marques Bento Santos (Hokyozu)** como parte das iniciativas de engenharia de software da **KyoFoundation**.

Distribuído sob a licença open-source **MIT**. Consulte o arquivo de licença para mais informações.
