package edu.ifrn.apigateway.catalog;

import java.util.List;
import java.util.Map;

public record CatalogResource(
        int id,
        String nome,
        String imagem,
        String fonte,
        Map<String, Object> atributos,
        Map<String, List<Integer>> relacoes) {
}
