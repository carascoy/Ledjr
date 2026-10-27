package com.Ledjr.dto;

import java.time.LocalDate;

public record SetPlanRequest(
    Long userId,            // temp until sessions (phase 5)
    LocalDate payoffTarget  // "2027-10-01", null = remove the plan
) {}
