package com.example.gitlabscanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GitLabFileContentDTO {
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("file_path")
    private String filePath;
    private Long size;
    private String encoding;
    private String content;
    @JsonProperty("content_sha256")
    private String contentSha256;
}
