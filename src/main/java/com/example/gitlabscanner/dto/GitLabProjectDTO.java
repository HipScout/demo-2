package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitLabProjectDTO {
    private Long id;
    private String name;
    @JsonProperty("web_url")
    private String webUrl;
    private String description;
    @JsonProperty("public")
    private Boolean isPublic;
    private String visibility;
}
