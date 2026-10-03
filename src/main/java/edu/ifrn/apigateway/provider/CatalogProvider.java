package edu.ifrn.apigateway.provider;

import edu.ifrn.apigateway.catalog.CatalogResource;
import edu.ifrn.apigateway.catalog.ResourceType;
import edu.ifrn.apigateway.catalog.Source;
import edu.ifrn.apigateway.common.page.PageResult;

public interface CatalogProvider {
    Source source();
    CatalogResource find(ResourceType type, int id);
    PageResult<CatalogResource> list(ResourceType type, int page);
}
