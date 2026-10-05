# Guia do projeto: HATEOAS, JWT e providers

Este documento indica os principais arquivos e trechos que implementam HATEOAS, autenticação JWT e a conversão dos dados das APIs de Simpsons e Rick and Morty.

## Visão geral

O projeto funciona como um gateway: consulta as APIs externas, converte personagens, episódios e locais para um modelo comum e devolve respostas navegáveis com links HATEOAS. As rotas `/api/**` exigem JWT.

## HATEOAS

- `src/main/java/edu/ifrn/apigateway/auth/RootController.java` — método `root()` (`@GetMapping`, linha 16): monta a raiz `/api`, com link para si própria e links para as fontes Simpsons e Rick and Morty.
- `src/main/java/edu/ifrn/apigateway/discovery/SourceController.java` — método `menu()` (linha 21): monta `/api/{fonte}` e acrescenta links para coleções e recursos individuais.
- `src/main/java/edu/ifrn/apigateway/discovery/SourceMenu.java` — estende `RepresentationModel<SourceMenu>` (linha 5), permitindo que o menu carregue links HATEOAS.
- `src/main/java/edu/ifrn/apigateway/catalog/CatalogController.java` — método `list()` (linha 32): cria a listagem paginada e adiciona links `self`, `first`, `prev`, `next`, `last` e `source` (linhas 47–52).
- `src/main/java/edu/ifrn/apigateway/catalog/CatalogController.java` — método `item()` (linha 67): envolve o recurso em `EntityModel`, adiciona links para o próprio item, coleção e fonte, e converte suas relações em links navegáveis (linhas 73–77).
- `src/main/java/edu/ifrn/apigateway/catalog/CatalogPageModel.java` — estende `RepresentationModel` e expõe os itens em `_embedded`, além dos metadados de página.
- `src/main/java/edu/ifrn/apigateway/auth/AuthController.java` — login também devolve um link `root` junto com o token (linhas 23–29).

Conceitos usados no código: `RepresentationModel` é a base para respostas com links; `EntityModel` envolve um recurso individual; `WebMvcLinkBuilder` cria links a partir dos métodos dos controllers.

## JWT

O fluxo é dividido em emissão, validação e configuração de acesso:

1. `src/main/java/edu/ifrn/apigateway/auth/LoginRequest.java` define o corpo do login: `username` e `password`, ambos obrigatórios.
2. `src/main/java/edu/ifrn/apigateway/auth/AuthController.java` recebe `POST /auth/login`, chama o serviço e devolve a resposta com link HATEOAS.
3. `src/main/java/edu/ifrn/apigateway/auth/AuthService.java`, método `login()` (linha 18), valida as credenciais pelo `AuthenticationManager` e solicita a emissão do JWT.
4. `src/main/java/edu/ifrn/apigateway/security/JwtService.java`, método `issue()` (linha 22), cria o token com `subject`, horário de emissão e expiração e assina-o. O método `subject()` (linha 32) verifica a assinatura e lê o usuário.
5. `src/main/java/edu/ifrn/apigateway/security/JwtAuthenticationFilter.java`, método `doFilterInternal()` (linha 25), lê `Authorization: Bearer ...`, valida o token e configura a autenticação da requisição.
6. `src/main/java/edu/ifrn/apigateway/security/SecurityConfig.java`, método `securityFilterChain()` (linha 28), configura sessões stateless, deixa login e documentação públicos, exige autenticação nas demais rotas e instala o filtro JWT.
7. `src/main/java/edu/ifrn/apigateway/auth/TokenResponse.java` define a resposta: `token`, `tipo` (`Bearer`) e `expiraEmSegundos`.
8. `src/main/resources/application.properties` configura segredo, expiração e credenciais usando variáveis de ambiente (`JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `APP_USER_USERNAME`, `APP_USER_PASSWORD`).

Para consumir uma rota protegida, envie o token no cabeçalho `Authorization: Bearer <token>`. O token não deve ser enviado como parâmetro da URL.

## Modelo comum de catálogo

`src/main/java/edu/ifrn/apigateway/catalog/CatalogResource.java` define o DTO retornado pelo gateway:

| Campo | Conteúdo |
|---|---|
| `id` | Identificador do recurso na fonte. |
| `nome` | Nome do personagem, episódio ou local. |
| `imagem` | URL da imagem, quando disponível. |
| `fonte` | `rickandmorty` ou `simpsons`. |
| `atributos` | Dados adicionais específicos da fonte e do tipo de recurso. |
| `relacoes` | Relações com outros itens, representadas por listas de IDs. |

A paginação interna é representada por `src/main/java/edu/ifrn/apigateway/common/page/PageResult.java`, que contém itens, número e tamanho da página, total de elementos e total de páginas. O controller usa esses dados para construir os links HATEOAS.

`src/main/java/edu/ifrn/apigateway/catalog/ResourceType.java` associa os tipos comuns `characters`, `episodes` e `locations` aos caminhos de cada API externa.

## Dados de Rick and Morty

`src/main/java/edu/ifrn/apigateway/provider/rickandmorty/RickAndMortyProvider.java`, método `map()` (linha 26), converte cada tipo para `CatalogResource`:

- **Personagem:** atributos `status`, `species`, `type`, `gender`, `origem` e `localizacao`; relações com origem, localização e episódios. A imagem vem do campo `image`.
- **Episódio:** atributos `air_date` e `episode`; relação com personagens.
- **Local:** atributos `type` e `dimension`; relação com personagens residentes.

A API externa representa algumas relações como URLs. O provider extrai os IDs dessas URLs para preencher `relacoes`. O método `mapPage()` (linha 54) lê os itens de `results` e os totais do objeto `info`.

## Dados dos Simpsons

`src/main/java/edu/ifrn/apigateway/provider/simpsons/SimpsonsProvider.java`, método `map()` (linha 28), converte cada tipo para o mesmo `CatalogResource`:

- **Personagem:** atributos `age`, `birthdate`, `gender`, `occupation`, `status`, `description` e `phrases`; relações com o episódio e o local da primeira aparição. A imagem vem de `portrait_path`.
- **Episódio:** atributos `airdate`, `episode_number`, `season`, `description` e `synopsis`; relação com personagem. A imagem vem de `image_path`.
- **Local:** atributos `description`, `town` e `use`; relação com episódio. A imagem vem de `image_path`.

O método `imageUrl()` (linha 66) mantém URLs completas e completa caminhos relativos usando a base do CDN definida em `CDN_BASE`.

## Fluxo da consulta ao provider

1. `CatalogController` resolve a fonte e o tipo do recurso a partir da URL.
2. `CatalogService` seleciona o provider correspondente por meio do `ProviderRegistry`.
3. `AbstractRestCatalogProvider` faz a requisição HTTP à fonte externa e trata erros, como recurso não encontrado, indisponibilidade e timeout.
4. `RickAndMortyProvider` ou `SimpsonsProvider` converte o JSON externo para `CatalogResource`.
5. `CatalogController` monta a resposta HATEOAS e seus links.

A configuração dos `RestClient` está em `src/main/java/edu/ifrn/apigateway/config/RestClientConfig.java`; as URLs base das APIs estão em `src/main/resources/application.properties`.
