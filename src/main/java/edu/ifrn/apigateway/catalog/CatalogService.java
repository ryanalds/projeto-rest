package edu.ifrn.apigateway.catalog;

import edu.ifrn.apigateway.common.page.PageResult;
import edu.ifrn.apigateway.provider.ProviderRegistry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final ProviderRegistry providers;

    public CatalogService(ProviderRegistry providers) { this.providers = providers; }

    @Cacheable(cacheNames = "catalog-items", key = "#source.name() + ':' + #type.name() + ':' + #id")
    public CatalogResource find(Source source, ResourceType type, int id) {
        return providers.get(source).find(type, id);
    }

    @Cacheable(cacheNames = "catalog-pages", key = "#source.name() + ':' + #type.name() + ':' + #page")
    public PageResult<CatalogResource> list(Source source, ResourceType type, int page) {
        return providers.get(source).list(type, page);
    }
}
