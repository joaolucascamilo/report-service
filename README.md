# report-service

Microsserviço responsável pelo registro, gerenciamento e acompanhamento de ocorrências de infraestrutura urbana. Faz parte de um ecossistema de microsserviços que inclui `user-service`, `ms-geo` e `ms-priorizacao`.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Configuração e Execução](#configuração-e-execução)
- [Autenticação e Segurança](#autenticação-e-segurança)
- [Documentação Interativa (Swagger)](#documentação-interativa-swagger)
- [API REST](#api-rest)
- [Upload de Fotos (AWS S3)](#upload-de-fotos-aws-s3)
- [Modelos de Dados](#modelos-de-dados)
- [Enumerações](#enumerações)
- [Integração com Outros Serviços](#integração-com-outros-serviços)
- [Fluxos Principais](#fluxos-principais)

---

## Visão Geral

O `report-service` permite que cidadãos registrem problemas de infraestrutura urbana (buracos, falhas de iluminação, acúmulo de lixo etc.) e que agentes da prefeitura acompanhem e atualizem o status dessas ocorrências. Cidadãos também podem "apoiar" (endossar) ocorrências já existentes, aumentando sua prioridade de atendimento.

Ao registrar uma ocorrência, o serviço automaticamente:
- Envia os dados geográficos para o `ms-geo` (exibição no mapa);
- Solicita o cálculo de prioridade ao `ms-priorizacao`;
- Concede pontos ao cidadão via `user-service` quando a ocorrência é resolvida.

Fotos são enviadas diretamente para um bucket AWS S3 usando URLs pré-assinadas geradas pelo próprio serviço, sem que as credenciais AWS transitem pelo cliente.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.3 |
| Spring Security | (incluso no Boot) |
| Spring Cloud OpenFeign | 2025.1.1 |
| PostgreSQL | — |
| Hibernate / JPA | — |
| JWT (jjwt) | 0.11.5 |
| AWS SDK (S3) | 2.28.11 |
| springdoc-openapi (Swagger UI) | 2.8.8 |
| Lombok | — |

---

## Arquitetura

```
report-service
├── controller      # Endpoints REST (OcorrenciaController, FotoController)
├── service         # Regras de negócio (OcorrenciaService, JwtService, S3Service)
├── repository      # Acesso ao banco (Spring Data JPA)
├── entity          # Entidades JPA (Ocorrencia, Endereco, FotoOcorrencia)
├── dto             # Objetos de transferência de dados
├── client          # Feign clients para ms-geo, ms-priorizacao, user-service
├── config          # SecurityConfig, SecurityFilter, CorsConfig, S3Config, OpenApiConfig
├── enums           # StatusOcorrencia, TipoOcorrencia, NivelPrioridade
└── exception       # ResourceExceptionHandler, ConflictException
```

---

## Configuração e Execução

### Pré-requisitos

- JDK 17+
- PostgreSQL rodando em `localhost:5432`
- Banco de dados `infra_urbana` criado
- Um bucket AWS S3 e credenciais IAM com permissão de leitura/escrita/exclusão de objetos (usados para armazenar fotos das ocorrências)

### Variáveis / `application.properties`

```properties
spring.application.name=report-service
server.port=8081

# Banco de dados
spring.datasource.url=jdbc:postgresql://localhost:5432/infra_urbana
spring.datasource.username=admin
spring.datasource.password=password123
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
api.security.token.secret=<chave-base64>
api.security.token.expiration=86400000   # 24 horas em ms

# Microsserviços externos
ms-priorizacao.url=http://localhost:8085

# AWS S3 (valores lidos de variáveis de ambiente)
aws.region=${APP_AWS_REGION}
aws.access-key=${APP_AWS_ACCESS_KEY}
aws.secret-key=${APP_AWS_SECRET_KEY}
aws.bucket=${APP_AWS_BUCKET}

# Swagger / OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.operationsSorter=method
```

> `ms-geo` está configurado em `http://localhost:8084` e `user-service` em `http://localhost:8082` diretamente nas anotações `@FeignClient`.

As credenciais AWS **não têm valor padrão** e devem ser definidas como variáveis de ambiente antes de subir a aplicação:

```bash
export APP_AWS_REGION=us-east-1
export APP_AWS_ACCESS_KEY=<access-key>
export APP_AWS_SECRET_KEY=<secret-key>
export APP_AWS_BUCKET=<nome-do-bucket>
```

### Executando

```bash
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8081`.

---

## Autenticação e Segurança

O serviço utiliza **JWT stateless**. Cada requisição protegida deve enviar o header:

```
Authorization: Bearer <token>
```

O token é gerado pelo `user-service` e contém as seguintes claims:

| Claim | Descrição |
|---|---|
| `sub` | E-mail do usuário |
| `perfil` | `ROLE_CIDADAO` ou `ROLE_AGENTE_PREFEITURA` |
| `id` | ID do usuário no banco |

### Permissões por Endpoint

| Endpoint | Perfil necessário |
|---|---|
| `POST /api/ocorrencias` | `ROLE_CIDADAO` |
| `POST /api/ocorrencias/{id}/apoiar` | `ROLE_CIDADAO` |
| `GET /api/ocorrencias/minhas` | `ROLE_CIDADAO` |
| `GET /api/ocorrencias` | `ROLE_AGENTE_PREFEITURA` |
| `PUT /api/ocorrencias/{id}/status` | `ROLE_AGENTE_PREFEITURA` |
| `DELETE /api/ocorrencias/{id}` | Autenticado |
| `GET /api/ocorrencias/verificar` | Público (documentado como tal) |
| `GET /api/ocorrencias/mapa` | Público |
| `GET /api/ocorrencias/{id}` | Público |
| `GET /api/fotos/presigned-url` | `ROLE_CIDADAO` |
| `/swagger-ui/**`, `/v3/api-docs/**` | Público |

> **Atenção (débito técnico):** o `SecurityConfig` atual restringe explicitamente por `hasRole` apenas `POST /api/ocorrencias`, `GET /api/ocorrencias/minhas`, `PUT /api/ocorrencias/{id}/status` e `GET /api/fotos/presigned-url`. A regra para a listagem administrativa aponta para o caminho `/api/ocorrencias/todas`, que **não corresponde** ao caminho real do endpoint (`GET /api/ocorrencias`); e não há matcher explícito para `POST /api/ocorrencias/{id}/apoiar` nem `.permitAll()` para `GET /api/ocorrencias/verificar`. Na prática, esses três endpoints caem na regra genérica `anyRequest().authenticated()`: `GET /api/ocorrencias` e `POST /api/ocorrencias/{id}/apoiar` aceitam qualquer usuário autenticado (não apenas o perfil pretendido), e `GET /api/ocorrencias/verificar` exige autenticação apesar de ser documentado como público. Vale corrigir os matchers em `config/SecurityConfig.java`.

### CORS

Origens permitidas (`config/CorsConfig.java`): `http://localhost:5500`, `http://127.0.0.1:5500`, `https://somar.up.railway.app`. Métodos: `GET, POST, PUT, DELETE, OPTIONS`. Credenciais habilitadas (`allowCredentials(true)`).

---

## Documentação Interativa (Swagger)

Com a aplicação em execução, a documentação OpenAPI fica disponível em:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- JSON da especificação: `http://localhost:8081/v3/api-docs`

Ambos os caminhos são públicos.

---

## API REST

**Base URL:** `http://localhost:8081/api/ocorrencias`

---

### `POST /api/ocorrencias`

Registra uma nova ocorrência.

**Perfil:** `ROLE_CIDADAO`

**Request Body:**
```json
{
  "enderecoOcorrencia": {
    "rua": "Rua das Flores",
    "numero": 123,
    "bairro": "Centro",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": 01310100
  },
  "tipoOcorrencia": 1,
  "detalhes": "Grande buraco no meio da via, risco de acidente.",
  "fotoOcorrencia": [
    {
      "nomeArquivo": "foto1.jpg",
      "url": "fotos/uuid-foto1.jpg",
      "bucket": "nome-do-bucket"
    }
  ],
  "latitude": -23.5505,
  "longitude": -46.6333
}
```

> As fotos devem ser enviadas antes ao S3 usando a URL pré-assinada obtida em `GET /api/fotos/presigned-url` — veja [Upload de Fotos](#upload-de-fotos-aws-s3).

**Response `201 Created`:**
```json
{
  "id": 42,
  "data": "2026-06-17T10:30:00",
  "status": 1,
  "tipo": 1,
  "descricaoTipo": "Buraco em via pública",
  "detalhes": "Grande buraco no meio da via, risco de acidente.",
  "endereco": {
    "rua": "Rua das Flores",
    "numero": 123,
    "bairro": "Centro",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": 01310100
  },
  "fotos": [
    { "id": 1, "url": "https://bucket.s3.amazonaws.com/fotos/uuid-foto1.jpg?X-Amz-Signature=..." }
  ],
  "nivelPrioridade": "MEDIA",
  "quantidadeDenuncias": 1
}
```

**Erros:**
| Código | Descrição |
|---|---|
| `400` | Já existe ocorrência pendente do mesmo tipo no mesmo local |

---

### `POST /api/ocorrencias/{id}/apoiar`

Registra o apoio (endosso) do cidadão autenticado a uma ocorrência já existente, incrementando `quantidadeDenuncias` e disparando um novo cálculo de prioridade.

**Perfil:** `ROLE_CIDADAO`

**Path Parameter:** `id` — ID da ocorrência

**Response `200 OK`:** `OcorrenciaResponse` atualizado, com `quantidadeDenuncias` incrementado

**Erros:**
| Código | Descrição |
|---|---|
| `404` | Ocorrência não encontrada |
| `409` | O cidadão autenticado já apoiou esta ocorrência anteriormente |

---

### `GET /api/ocorrencias/minhas`

Lista as ocorrências registradas pelo cidadão autenticado.

**Perfil:** `ROLE_CIDADAO`

**Response `200 OK`:** Array de `OcorrenciaResponse`

---

### `GET /api/ocorrencias`

Lista todas as ocorrências (visão administrativa).

**Perfil:** `ROLE_AGENTE_PREFEITURA`

**Response `200 OK`:** Array de `OcorrenciaResponse`

---

### `PUT /api/ocorrencias/{id}/status`

Atualiza o status de uma ocorrência.

**Perfil:** `ROLE_AGENTE_PREFEITURA`

**Path Parameter:** `id` — ID da ocorrência

**Request Body:** `Integer` — Código do novo status (ver tabela de enums)

**Response `200 OK`:** `OcorrenciaResponse` atualizado

**Erros:**
| Código | Descrição |
|---|---|
| `400` | Código de status inválido |
| `404` | Ocorrência não encontrada |

> Ao atualizar para status `RESOLVIDO (3)`, o sistema automaticamente concede **50 pontos** ao cidadão que registrou a ocorrência.

---

### `DELETE /api/ocorrencias/{id}`

Remove uma ocorrência, incluindo a exclusão de suas fotos associadas no bucket S3.

**Autenticação:** Requer token válido

**Path Parameter:** `id` — ID da ocorrência

**Response `204 No Content`**

**Erros:**
| Código | Descrição |
|---|---|
| `404` | Ocorrência não encontrada |

---

### `GET /api/ocorrencias/verificar`

Verifica se já existe uma ocorrência ativa do mesmo tipo em uma rua e bairro informados. Usado pelo frontend como alternativa quando não há coordenadas GPS disponíveis. Documentado como público (ver ressalva de segurança acima).

**Query Parameters:** `tipo` (nome do enum `TipoOcorrencia`), `rua`, `bairro` — todos obrigatórios

**Response `200 OK`:** `OcorrenciaResponse` da ocorrência ativa encontrada

**Erros:**
| Código | Descrição |
|---|---|
| `404` | Nenhuma ocorrência ativa do tipo informado no endereço |

---

### `GET /api/ocorrencias/mapa`

Lista ocorrências para exibição no mapa (endpoint público).

**Response `200 OK`:** Array de `OcorrenciaResponse`

---

### `GET /api/ocorrencias/{id}`

Retorna detalhes de uma ocorrência pelo ID (endpoint público).

**Path Parameter:** `id` — ID da ocorrência

**Response `200 OK`:** `OcorrenciaResponse`

**Erros:**
| Código | Descrição |
|---|---|
| `404` | Ocorrência não encontrada |

---

## Upload de Fotos (AWS S3)

**Base URL:** `http://localhost:8081/api/fotos`

### `GET /api/fotos/presigned-url`

Gera uma URL pré-assinada do S3 (validade de 15 minutos) para que o cliente faça upload de uma foto diretamente ao bucket, sem que credenciais AWS transitem pelo backend ou pelo cliente.

**Perfil:** `ROLE_CIDADAO`

**Query Parameter:** `nomeArquivo` — nome original do arquivo (ex: `foto_buraco.jpg`)

**Response `200 OK`:**
```json
{
  "uploadUrl": "https://bucket.s3.amazonaws.com/fotos/uuid-foto_buraco.jpg?X-Amz-Signature=...",
  "key": "fotos/uuid-foto_buraco.jpg",
  "bucket": "fiscalizacao-fotos"
}
```

**Fluxo de uso:**
1. Chamar este endpoint com o nome do arquivo.
2. Fazer um `PUT` do arquivo diretamente para a `uploadUrl` retornada.
3. Enviar o `key` e o `bucket` retornados no campo `fotoOcorrencia` do `POST /api/ocorrencias`.

> As URLs de leitura retornadas em `OcorrenciaResponse.fotos[].url` são geradas dinamicamente (URLs pré-assinadas de leitura, validade de 60 minutos) a cada requisição — não é a URL literal armazenada no banco.

---

## Modelos de Dados

### OcorrenciaRequest

| Campo | Tipo | Descrição |
|---|---|---|
| `enderecoOcorrencia` | `EnderecoRequest` | Endereço da ocorrência |
| `tipoOcorrencia` | `Integer` | Código do tipo (ver enum) |
| `detalhes` | `String` | Descrição detalhada do problema |
| `fotoOcorrencia` | `List<FotoOcorrenciaRequest>` | Fotos anexadas (chaves obtidas via presigned URL) |
| `latitude` | `Double` | Latitude GPS |
| `longitude` | `Double` | Longitude GPS |

### OcorrenciaResponse

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` | ID da ocorrência |
| `data` | `LocalDateTime` | Data e hora do registro |
| `status` | `Integer` | Código do status atual |
| `tipo` | `Integer` | Código do tipo da ocorrência |
| `descricaoTipo` | `String` | Descrição textual do tipo |
| `detalhes` | `String` | Descrição detalhada do problema |
| `endereco` | `EnderecoResponse` | Endereço formatado |
| `fotos` | `List<FotoResponse>` | Lista de fotos (URLs pré-assinadas de leitura) |
| `nivelPrioridade` | `String` | Nível calculado pelo `ms-priorizacao` |
| `quantidadeDenuncias` | `Integer` | Total de apoios/denúncias acumulados na ocorrência |

### EnderecoRequest / EnderecoResponse

| Campo | Tipo |
|---|---|
| `rua` | `String` |
| `numero` | `Integer` |
| `bairro` | `String` |
| `cidade` | `String` |
| `estado` | `String` |
| `cep` | `Integer` |

### FotoOcorrenciaRequest

| Campo | Tipo | Descrição |
|---|---|---|
| `nomeArquivo` | `String` | Nome original do arquivo |
| `url` | `String` | Chave (key) do objeto no S3, retornada por `GET /api/fotos/presigned-url` |
| `bucket` | `String` | Nome do bucket (opcional) |

### PresignedUrlResponse

| Campo | Tipo | Descrição |
|---|---|---|
| `uploadUrl` | `String` | URL temporária para `PUT` do arquivo direto no S3 |
| `key` | `String` | Chave (path) do arquivo dentro do bucket |
| `bucket` | `String` | Nome do bucket S3 |

---

## Enumerações

### StatusOcorrencia

| Código | Nome | Descrição |
|---|---|---|
| `1` | `REGISTRADO` | Ocorrência registrada, aguardando ação |
| `2` | `CANCELADO` | Ocorrência cancelada |
| `3` | `RESOLVIDO` | Problema solucionado |
| `4` | `EM_PROCEDIMENTO` | Em atendimento pela prefeitura |

### TipoOcorrencia

| Código | Nome | Descrição |
|---|---|---|
| `1` | `BURACO_VIA` | Buraco em via pública |
| `2` | `PAVIMENTACAO_DANIFICADA` | Pavimentação danificada ou desgastada |
| `3` | `FALHA_DRENAGEM` | Falha na drenagem ou bueiro entupido |
| `4` | `CALCADA_IRREGULAR` | Calçada irregular ou quebrada |
| `5` | `FALHA_ILUMINACAO` | Falha na iluminação pública |
| `6` | `SINALIZACAO_DEFEITUOSA` | Sinalização defeituosa ou ausente |
| `7` | `ACUMULO_LIXO` | Acúmulo de lixo ou entulho |
| `99` | `OUTROS` | Outros problemas |

### NivelPrioridade

| Valor | Score mínimo |
|---|---|
| `CRITICA` | >= 80 |
| `ALTA` | >= 50 |
| `MEDIA` | >= 25 |
| `BAIXA` | < 25 |

---

## Integração com Outros Serviços

### ms-geo (`http://localhost:8084`)

| Método | Endpoint | Payload | Descrição |
|---|---|---|---|
| `POST` | `/api/geo/ocorrencias` | `OcorrenciaGeoDTO` | Registra ocorrência no mapa |
| `PATCH` | `/api/geo/ocorrencias/{id}/apoiar` | `{ "quantidadeDenuncias": Integer }` | Atualiza a contagem de apoios da ocorrência no mapa |

**OcorrenciaGeoDTO:**

| Campo | Tipo |
|---|---|
| `id` | `long` |
| `categoria` | `String` |
| `status` | `String` |
| `quantidadeDenuncias` | `Integer` |
| `dataCriacao` | `LocalDateTime` |
| `rua`, `bairro`, `cidade`, `estado`, `pais` | `String` |
| `latitude`, `longitude` | `Double` |

---

### ms-priorizacao (`http://localhost:8085`)

| Método | Endpoint | Payload | Resposta | Descrição |
|---|---|---|---|---|
| `POST` | `/api/priorizacao/calcular` | `OcorrenciaPriorizacaoDTO` | `PrioridadeResponseDTO` | Calcula prioridade inicial |
| `PUT` | `/api/priorizacao/recalcular/{ocorrenciaId}` | `OcorrenciaPriorizacaoDTO` | `PrioridadeResponseDTO` | Recalcula após mudança de status ou novo apoio |

**OcorrenciaPriorizacaoDTO:**

| Campo | Tipo |
|---|---|
| `id` | `Long` |
| `tipoOcorrencia` | `String` |
| `quantidadeDenuncias` | `Integer` |
| `dataCriacao` | `LocalDateTime` |
| `latitude`, `longitude` | `Double` |

**PrioridadeResponseDTO:**

| Campo | Tipo |
|---|---|
| `ocorrenciaId` | `Long` |
| `nivelPrioridade` | `NivelPrioridade` |
| `scoreCalculado` | `Integer` |
| `justificativa` | `String` |

---

### user-service (`http://localhost:8082`)

| Método | Endpoint | Payload | Header | Descrição |
|---|---|---|---|---|
| `POST` | `/api/usuarios/pontuar` | `PontuacaoRequestDTO` | `Authorization` | Concede pontos ao cidadão |

**PontuacaoRequestDTO:**

| Campo | Tipo |
|---|---|
| `usuarioId` | `Long` |
| `pontos` | `Integer` |
| `descricao` | `String` |

---

## Fluxos Principais

### Upload de Foto + Registro de Ocorrência

```
Cidadão → GET /api/fotos/presigned-url?nomeArquivo=foto.jpg
  ↓ S3Service gera chave "fotos/{uuid}_{nomeArquivo}"
  ← 200 PresignedUrlResponse { uploadUrl, key, bucket }
Cidadão → PUT <uploadUrl>  (upload direto ao S3, fora do report-service)
Cidadão → POST /api/ocorrencias
  ↓ SecurityFilter valida JWT e extrai usuarioId
  ↓ OcorrenciaService.registrar()
      ↓ Valida duplicidade (mesmo tipo + endereço com status pendente)
      ↓ Inicializa quantidadeDenuncias = 1
      ↓ Persiste Ocorrencia + Endereco + FotoOcorrencia no PostgreSQL
      ↓ GeoClient → POST ms-geo/api/geo/ocorrencias
      ↓ PriorizacaoClient → POST ms-priorizacao/api/priorizacao/calcular
      ↓ Atualiza nivelPrioridade na entidade
  ← 201 OcorrenciaResponse
```

### Apoiar Ocorrência

```
Cidadão → POST /api/ocorrencias/{id}/apoiar
  ↓ SecurityFilter valida JWT e extrai usuarioId
  ↓ OcorrenciaService.apoiar()
      ↓ Busca ocorrência por ID (404 se não encontrada)
      ↓ Verifica se usuarioId já está em apoiadores (409 se sim)
      ↓ Adiciona usuarioId a apoiadores e incrementa quantidadeDenuncias
      ↓ GeoClient → PATCH ms-geo/api/geo/ocorrencias/{id}/apoiar
      ↓ PriorizacaoClient → PUT ms-priorizacao/api/priorizacao/recalcular/{id}
  ← 200 OcorrenciaResponse
```

### Atualização de Status

```
Agente → PUT /api/ocorrencias/{id}/status
  ↓ SecurityFilter valida JWT (ROLE_AGENTE_PREFEITURA)
  ↓ OcorrenciaService.atualizarStatus()
      ↓ Atualiza status no banco
      ↓ PriorizacaoClient → PUT ms-priorizacao/api/priorizacao/recalcular/{id}
      ↓ Se status = RESOLVIDO:
          ↓ UserClient → POST user-service/api/usuarios/pontuar (50 pontos)
  ← 200 OcorrenciaResponse
```

### Remoção de Ocorrência

```
Cliente → DELETE /api/ocorrencias/{id}
  ↓ SecurityFilter valida JWT
  ↓ OcorrenciaService.deletar()
      ↓ Para cada foto associada: S3Service remove o objeto do bucket S3
      ↓ Remove a ocorrência do PostgreSQL
  ← 204 No Content
```
