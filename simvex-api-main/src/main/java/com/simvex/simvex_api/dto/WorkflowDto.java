package com.simvex.simvex_api.dto;

import java.util.List;

public class WorkflowDto {
    
    public static class NodeResponse {
        public Long id;
        public String title;
        public String content;
        public Double x;
        public Double y;
        public List<FileResponse> files;
    }

    public static class ConnectionResponse {
        public Long id;
        public Long from; // fromNodeId
        public Long to;   // toNodeId
        public String fromAnchor;
        public String toAnchor;
    }

    public static class FileResponse {
        public Long id;
        public String fileName;
        public String url;
    }
    
    // 생성/수정 요청용 DTO
    public static class NodeRequest {
        public String title;
        public String content;
        public Double x;
        public Double y;
    }

    public static class ConnectionRequest {
        public Long from;
        public Long to;
        public String fromAnchor;
        public String toAnchor;
    }
}