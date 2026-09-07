package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitLabTreeItemDTO {
    private String id;
    private String name;
    private String type;
    private String path;
    @JsonProperty("mode")
    private String mode;
}
