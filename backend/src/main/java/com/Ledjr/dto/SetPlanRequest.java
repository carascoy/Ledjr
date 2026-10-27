package com.Ledjr.dto;

import java.time.LocalDate;

public record SetPlanRequest(
    LocalDate payoffTarget  // "2027-10-01", null = remove the plan
) {}
