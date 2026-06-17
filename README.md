# report-service

Microsserviço responsável pelo registro, gerenciamento e acompanhamento de ocorrências de infraestrutura urbana. Faz parte de um ecossistema de microsserviços que inclui `user-service`, `ms-geo` e `ms-priorizacao`.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Configuração e Execução](#configuração-e-execução)
- [Autenticação e Segurança](#autenticação-e-segurança)
- [API REST](#api-rest)
- [Modelos de Dados](#modelos-de-dados)
- [Enumerações](#enumerações)
- [Integração com Outros Serviços](#integração-com-outros-serviços)
- [Fluxos Principais](#fluxos-principais)

---

## Visão Geral

O `report-service` permite que cidadãos registrem problemas de infraestrutura urbana (buracos, falhas de iluminação, acúmulo de lixo etc.) e que agentes da prefeitura acompanhem e atualizem o status dessas ocorrências.

Ao registrar uma ocorrência, o serviço automaticamente:
- Envia os dados geográficos para o `ms-geo` (exibição no mapa);
- Solicita o cálculo de prioridade ao `ms-priorizacao`;
- Concede pontos ao cidadão via `user-service` quando a ocorrência é resolvida.

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
| Lombok | — |

---

## Arquitetura

```
report-service
├── controller      # Endpoints REST
├── service         # Regras de negócio
├── repository      # Acesso ao banco (Spring Data JPA)
├── entity          # Entidades JPA (Ocorrencia, Endereco, FotoOcorrencia)
├── dto             # Objetos de transferência de dados
├── client          # Feign clients para ms-geo, ms-priorizacao, user-service
├── config          # SecurityConfig, habilitação do Feign
├── enums           # StatusOcorrencia, TipoOcorrencia, NivelPrioridade
└── exception       # Handler global de exceções
```

---

## Configuração e Execução

### Pré-requisitos

- JDK 17+
- PostgreSQL rodando em `localhost:5432`
- Banco de dados `infra_urbana` criado

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
```

> `ms-geo` está configurado em `http://localhost:8084` e `user-service` em `http://localhost:8082` diretamente nas anotações `@FeignClient`.

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
| `GET /api/ocorrencias/minhas` | `ROLE_CIDADAO` |
| `GET /api/ocorrencias` | `ROLE_AGENTE_PREFEITURA` |
| `PUT /api/ocorrencias/{id}/status` | `ROLE_AGENTE_PREFEITURA` |
| `DELETE /api/ocorrencias/{id}` | Autenticado |
| `GET /api/ocorrencias/mapa` | Público |
| `GET /api/ocorrencias/{id}` | Público |

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
  "fotoOcorrencia": [
    {
      "nomeArquivo": "foto1.jpg",
      "url": "https://bucket.s3.amazonaws.com/foto1.jpg",
      "bucket": "nome-do-bucket"
    }
  ],
  "latitude": -23.5505,
  "longitude": -46.6333
}
```

**Response `201 Created`:**
```json
{
  "id": 42,
  "data": "2026-06-17T10:30:00",
  "status": 1,
  "endereco": {
    "rua": "Rua das Flores",
    "numero": 123,
    "bairro": "Centro",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": 01310100
  },
  "fotos": [
    { "id": 1, "url": "https://bucket.s3.amazonaws.com/foto1.jpg" }
  ],
  "nivelPrioridade": "MEDIA"
}
```

**Erros:**
| Código | Descrição |
|---|---|
| `400` | Já existe ocorrência pendente do mesmo tipo no mesmo local |

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

Remove uma ocorrência.

**Autenticação:** Requer token válido

**Path Parameter:** `id` — ID da ocorrência

**Response `204 No Content`**

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

## Modelos de Dados

### OcorrenciaRequest

| Campo | Tipo | Descrição |
|---|---|---|
| `enderecoOcorrencia` | `EnderecoRequest` | Endereço da ocorrência |
| `tipoOcorrencia` | `Integer` | Código do tipo (ver enum) |
| `fotoOcorrencia` | `List<FotoOcorrenciaRequest>` | Fotos anexadas |
| `latitude` | `Double` | Latitude GPS |
| `longitude` | `Double` | Longitude GPS |

### OcorrenciaResponse

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` | ID da ocorrência |
| `data` | `LocalDateTime` | Data e hora do registro |
| `status` | `Integer` | Código do status atual |
| `endereco` | `EnderecoResponse` | Endereço formatado |
| `fotos` | `List<FotoResponse>` | Lista de fotos |
| `nivelPrioridade` | `String` | Nível calculado pelo `ms-priorizacao` |

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
| `nomeArquivo` | `String` | Nome do arquivo |
| `url` | `String` | URL completa ou chave S3 |
| `bucket` | `String` | Nome do bucket (opcional) |

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
| `PUT` | `/api/priorizacao/recalcular/{ocorrenciaId}` | `OcorrenciaPriorizacaoDTO` | `PrioridadeResponseDTO` | Recalcula após mudança de status |

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

### Registro de Ocorrência

```
Cidadão → POST /api/ocorrencias
  ↓ SecurityFilter valida JWT e extrai usuarioId
  ↓ OcorrenciaService.registrar()
      ↓ Valida duplicidade (mesmo tipo + endereço com status pendente)
      ↓ Persiste Ocorrencia + Endereco + FotoOcorrencia no PostgreSQL
      ↓ GeoClient → POST ms-geo/api/geo/ocorrencias
      ↓ PriorizacaoClient → POST ms-priorizacao/api/priorizacao/calcular
      ↓ Atualiza nivelPrioridade na entidade
  ← 201 OcorrenciaResponse
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