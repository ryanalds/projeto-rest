package edu.ifrn.apigateway.common.page;

import java.util.List;

public record PageResult<T>(List<T> items, int number, int size, long totalElements, int totalPages) {
}
