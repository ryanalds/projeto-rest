package edu.ifrn.apigateway.provider;

import edu.ifrn.apigateway.catalog.Source;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderRegistry {
    private final Map<Source, CatalogProvider> providers = new EnumMap<>(Source.class);

    public ProviderRegistry(List<CatalogProvider> providers) {
        providers.forEach(provider -> this.providers.put(provider.source(), provider));
    }

    public CatalogProvider get(Source source) {
        CatalogProvider provider = providers.get(source);
        if (provider == null) throw new IllegalArgumentException("Fonte não configurada: " + source.path());
        return provider;
    }
}
