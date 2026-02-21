package com.utem.utem_core.dto;

import java.util.List;

public record FailureClusterDTO(
        int clusterId,
        String representativeError,  // normalized canonical error message
        int occurrences,
        List<String> affectedTests,  // distinct test names in this cluster
        List<String> runIds          // distinct run IDs where cluster appears
) {}
