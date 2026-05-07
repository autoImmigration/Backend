package com.yongsik.immigrationops.review.domain;

import java.util.List;

public record ReviewTask(
        ReviewStatus status,
        List<String> reasons
) {
}

