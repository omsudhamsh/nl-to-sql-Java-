package com.nlsql.model;

import jakarta.validation.constraints.NotBlank;

public record QuestionRequest(@NotBlank String question) {}