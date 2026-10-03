package edu.ifrn.apigateway.provider;

import edu.ifrn.apigateway.catalog.CatalogResource;
import edu.ifrn.apigateway.catalog.ResourceType;
import edu.ifrn.apigateway.catalog.Source;
import edu.ifrn.apigateway.common.error.BackendTimeoutException;
import edu.ifrn.apigateway.common.error.BackendUnavailableException;
import edu.ifrn.apigateway.common.error.BadGatewayException;
import edu.ifrn.apigateway.common.error.ResourceNotFoundException;
import edu.ifrn.apigateway.common.page.PageResult;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRestCatalogProvider implements CatalogProvider {
    protected final RestClient client;

    protected AbstractRestCatalogProvider(RestClient client) {
        this.client = client;
    }

    protected abstract Source providerSource();
    protected abstract CatalogResource map(ResourceType type, JsonNode json);
    protected abstract PageResult<CatalogResource> mapPage(ResourceType type, int requestedPage, JsonNode json);

    @Override
    public Source source() { return providerSource(); }

    @Override
    public CatalogResource find(ResourceType type, int id) {
        if (id < 1) throw new IllegalArgumentException("O id deve ser maior que zero.");
        JsonNode json = get(type.externalPath(source()) + "/" + id,
                "Recurso " + type.path() + "/" + id + " não encontrado na fonte " + source().path());
        return map(type, json);
    }

    @Override
    public PageResult<CatalogResource> list(ResourceType type, int page) {
        if (page < 1) throw new IllegalArgumentException("A página deve ser maior que zero.");
        JsonNode json = get(type.externalPath(source()) + "?page=" + page,
                "Página " + page + " não encontrada para " + type.path() + " na fonte " + source().path());
        return mapPage(type, page, json);
    }

    private JsonNode get(String path, String notFoundMessage) {
        try {
            JsonNode result = client.get().uri("/" + path).retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> { throw new ResourceNotFoundException(notFoundMessage); })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> { throw new BackendUnavailableException(
                                    "A fonte " + source().path() + " está indisponível.", null); })
                    .body(JsonNode.class);
            if (result == null || result.isNull()) throw new BadGatewayException("Resposta vazia da fonte " + source().path() + ".");
            return result;
        } catch (ResourceNotFoundException | BackendUnavailableException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new BackendTimeoutException("Tempo limite excedido ao consultar " + source().path() + ".", exception);
            }
            throw new BackendUnavailableException("Não foi possível acessar a fonte " + source().path() + ".", exception);
        } catch (RestClientResponseException exception) {
            throw new BackendUnavailableException("A fonte " + source().path()
                    + " respondeu com HTTP " + exception.getStatusCode().value() + ".", exception);
        } catch (RestClientException | IllegalStateException exception) {
            throw new BadGatewayException("Resposta inválida da fonte " + source().path() + ".", exception);
        }
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) return true;
        }
        return false;
    }

    protected String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    protected int integer(JsonNode node, String field, int fallback) {
        return node.path(field).asInt(fallback);
    }

    protected List<Integer> idsFromUrls(JsonNode array) {
        List<Integer> ids = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(value -> {
                String raw = value.isTextual() ? value.asString() : null;
                Integer id = trailingId(raw);
                if (id != null) ids.add(id);
            });
        }
        return ids;
    }

    protected Integer idFromUrl(String url) { return trailingId(url); }

    private Integer trailingId(String value) {
        if (value == null) return null;
        try {
            String clean = value.replaceAll("/+$", "");
            return Integer.valueOf(clean.substring(clean.lastIndexOf('/') + 1));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    protected List<Integer> idsFromObjects(JsonNode array, String field) {
        List<Integer> ids = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(value -> {
                int id = value.path(field).asInt(0);
                if (id > 0) ids.add(id);
            });
        }
        return ids;
    }
}
