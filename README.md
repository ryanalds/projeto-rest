# Guia da API Gateway

Este documento descreve os domínios do projeto, como iniciar a aplicação para acesso pela rede e como consumir a API. A aplicação integra as fontes Rick and Morty e The Simpsons, converte os dados para um formato comum e protege os endpoints de catálogo com JWT.

## 1. Domínios e responsabilidades

Os domínios abaixo correspondem às áreas funcionais organizadas nos pacotes Java em `src/main/java/edu/ifrn/apigateway/`.

| Domínio | Responsabilidade |
|---|---|
| `auth` | Recebe as credenciais em `/auth/login`, autentica o usuário configurado e devolve um JWT. Também expõe a raiz protegida de navegação em `/api`. |
| `security` | Configura a aplicação como API stateless, valida o JWT enviado em cada requisição protegida e autentica o usuário local em memória. |
| `discovery` | Descobre as fontes e apresenta o menu HATEOAS de cada fonte, com links para coleções e recursos individuais. |
| `catalog` | Define o modelo comum (`CatalogResource`), os tipos de recurso e a lógica/rotas para listar páginas ou buscar um item por ID. Inclui os links de navegação da resposta. |
| `provider` | Integra cada API externa e converte suas respostas para o modelo comum do catálogo. Cada provider encapsula as diferenças da fonte correspondente. |
| `common` | Reúne tipos compartilhados, como paginação e tratamento padronizado de erros. |
| `config` | Configura cliente HTTP, cache, CORS e documentação OpenAPI/Swagger. |

### Modelo de catálogo

Os recursos devolvidos pelas duas fontes usam estes campos comuns:

| Campo | Conteúdo |
|---|---|
| `id` | Identificador do item na fonte. |
| `nome` | Nome apresentado pelo item. |
| `imagem` | URL da imagem, quando fornecida pela fonte. |
| `fonte` | Fonte de origem (`rickandmorty` ou `simpsons`). |
| `atributos` | Dados adicionais específicos do personagem, episódio ou local. |
| `relacoes` | Relações com outros itens, representadas por IDs. A resposta HATEOAS também pode expor links navegáveis dessas relações. |

## 2. Requisitos

- Java 21 ou superior
- Maven 3.9 ou superior
- Acesso de rede às APIs externas Rick and Morty e The Simpsons

## 3. Iniciar para acesso na rede

Execute no servidor onde a aplicação ficará rodando. O endereço `0.0.0.0` faz o Spring Boot escutar pelas interfaces de rede disponíveis; as outras pessoas acessam usando o IP ou hostname desse servidor.

```bash
cd /caminho/para/projeto-rest

export JWT_SECRET="$(openssl rand -base64 32)"
export APP_USER_USERNAME=ryan
export APP_USER_PASSWORD='admin123'

mvn spring-boot:run -Dspring-boot.run.arguments="--server.address=0.0.0.0"
```

Mantenha esse terminal aberto enquanto a API estiver em uso. Para acessar de outro computador, descubra o IP do servidor (por exemplo, com `hostname -I`) e use:

```text
http://IP_DO_SERVIDOR:8080/
http://IP_DO_SERVIDOR:8080/swagger-ui.html
```

O hostname do servidor também pode ser usado se os computadores da rede conseguirem resolvê-lo. Se a conexão não funcionar, verifique se o firewall do servidor permite conexões TCP na porta 8080. Não compartilhe o valor de `JWT_SECRET` nem a senha da conta.

### Variáveis opcionais

| Variável | Padrão | Uso |
|---|---|---|
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do JWT em segundos. |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação. |
| `RICKANDMORTY_API_URL` | `https://rickandmortyapi.com/api` | Endereço base da API Rick and Morty. |
| `SIMPSONS_API_URL` | `https://thesimpsonsapi.com/api` | Endereço base da API dos Simpsons. |
| `PROVIDER_CONNECT_TIMEOUT_MS` | `2000` | Timeout para estabelecer conexão com uma fonte externa. |
| `PROVIDER_READ_TIMEOUT_MS` | `5000` | Timeout de leitura de uma fonte externa. |
| `CACHE_TTL_MINUTES` | `10` | Tempo de validade dos dados em cache. |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:8080` | Origem permitida para chamadas feitas por navegador. Configure a origem do frontend se ele estiver em outro host/porta. |

O arquivo `.env.example` lista as configurações. O Spring Boot não carrega `.env` automaticamente: exporte as variáveis no terminal ou configure-as no ambiente que inicia a aplicação.

## 4. Autenticação e uso da API

As rotas `/api/**` exigem JWT. Primeiro faça login com o usuário e a senha configurados no servidor:

```bash
BASE_URL=http://IP_DO_SERVIDOR:8080

curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"ryan","password":"defina-uma-senha-forte"}'
```

A resposta contém `token`, `tipo` (`Bearer`) e `expiraEmSegundos`. Copie o valor de `token` sem as aspas JSON e envie-o no cabeçalho `Authorization`. Não envie o token como parâmetro da URL.

Com `jq` instalado, é possível guardar o token na variável do terminal sem copiá-lo manualmente:

```bash
TOKEN=$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"ryan","password":"defina-uma-senha-forte"}' | jq -r '.token')
```

Uma chamada autenticada fica assim:

```bash
curl -i "$BASE_URL/api" \
  -H "Authorization: Bearer $TOKEN"
```

O token não deve ser colocado entre aspas dentro do valor do cabeçalho. As aspas acima são apenas sintaxe do shell para delimitar a string.

### Rotas

| Método | Caminho | Descrição | Autenticação |
|---|---|---|---|
| `POST` | `/auth/login` | Valida as credenciais e emite JWT. | Não |
| `GET` | `/api` | Raiz HATEOAS com links para as fontes disponíveis. | Sim |
| `GET` | `/api/{fonte}` | Menu HATEOAS da fonte, com links para as coleções e itens. | Sim |
| `GET` | `/api/{fonte}/{recurso}?page=1` | Lista uma página de recursos. A numeração começa em 1. | Sim |
| `GET` | `/api/{fonte}/{recurso}/{id}` | Busca um item pelo ID. | Sim |

Valores aceitos:

- `fonte`: `rickandmorty` ou `simpsons`
- `recurso`: `characters`, `episodes` ou `locations`
- `page`: inteiro maior ou igual a 1; se omitido, vale `1`
- `id`: inteiro maior ou igual a 1

### Exemplos

Descobrir as fontes:

```bash
curl -sS "$BASE_URL/api" -H "Authorization: Bearer $TOKEN"
```

Ver o menu de personagens, episódios e locais de Rick and Morty:

```bash
curl -sS "$BASE_URL/api/rickandmorty" \
  -H "Authorization: Bearer $TOKEN"
```

Listar personagens dos Simpsons na página 2:

```bash
curl -sS "$BASE_URL/api/simpsons/characters?page=2" \
  -H "Authorization: Bearer $TOKEN"
```

Buscar o personagem de ID 1 em Rick and Morty:

```bash
curl -sS "$BASE_URL/api/rickandmorty/characters/1" \
  -H "Authorization: Bearer $TOKEN"
```

### Navegação HATEOAS

As respostas de descoberta e catálogo incluem links `_links`. Use esses links para seguir para a fonte, coleção, página seguinte/anterior e recursos relacionados. As listas incluem `_embedded` com a coleção de itens e `page` com metadados de paginação. As relações retornadas variam conforme os dados oferecidos por cada fonte.

### Swagger e interface web

- Swagger UI: `http://IP_DO_SERVIDOR:8080/swagger-ui.html`. Use **Authorize** e informe somente o JWT, sem aspas e sem o prefixo `Bearer`; o Swagger acrescenta o esquema de autenticação.
- Interface web: `http://IP_DO_SERVIDOR:8080/`. Entre com o mesmo usuário e senha e navegue pelos links disponibilizados pela API.
- Saúde: `http://IP_DO_SERVIDOR:8080/actuator/health`.

### Respostas de erro

- `401 Unauthorized`: credenciais incorretas no login ou token ausente, inválido ou expirado.
- `404 Not Found`: fonte, recurso ou item inexistente.
- `400 Bad Request`: parâmetros inválidos, como página ou ID menor que 1.
- `502 Bad Gateway`, `503 Service Unavailable` e `504 Gateway Timeout`: erro, indisponibilidade ou timeout ao consultar uma API externa.

Os erros da API são devolvidos no formato Problem Details (`application/problem+json`). Faça login novamente para obter outro token quando ele expirar.
