package com.boonya.business.trip.mcp.server.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileSystemTool {

    private static final String BASE_DIR = "/tmp/mcp-files";

    @McpTool(
            name = "list-directory",
            description = "列出指定目录下的文件和子目录"
    )
    public String listDirectory(
            @McpToolParam(description = "目录路径，默认为根目录", required = false) String path
    ) {
        String targetPath = (path != null && !path.isEmpty()) ? path : BASE_DIR;

        log.info("列出目录: {}", targetPath);

        try {
            File directory = new File(targetPath);

            if (!directory.exists()) {
                return "目录不存在: " + targetPath;
            }

            if (!directory.isDirectory()) {
                return "路径不是目录: " + targetPath;
            }

            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                return "目录为空";
            }

            List<String> fileList = Arrays.stream(files)
                    .map(f -> (f.isDirectory() ? "[DIR]  " : "[FILE] ") + f.getName())
                    .collect(Collectors.toList());

            return String.join("\n", fileList);

        } catch (Exception e) {
            log.error("列出目录失败: {}", targetPath, e);
            return "操作失败: " + e.getMessage();
        }
    }

    @McpTool(
            name = "read-file",
            description = "读取指定文件的内容"
    )
    public String readFile(
            @McpToolParam(description = "文件完整路径") String filePath
    ) {
        log.info("读取文件: {}", filePath);

        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return "文件不存在: " + filePath;
            }

            if (!Files.isRegularFile(path)) {
                return "路径不是文件: " + filePath;
            }

            byte[] bytes = Files.readAllBytes(path);
            String content = new String(bytes);

            if (content.length() > 10000) {
                return content.substring(0, 10000) + "\n... (内容过长已截断)";
            }

            return content;

        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return "读取失败: " + e.getMessage();
        }
    }

    @McpTool(
            name = "write-file",
            description = "写入内容到指定文件"
    )
    public String writeFile(
            @McpToolParam(description = "文件完整路径") String filePath,
            @McpToolParam(description = "要写入的文件内容") String content
    ) {
        log.info("写入文件: {}", filePath);

        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();

            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.write(path, content.getBytes());
            return "文件写入成功: " + filePath;

        } catch (IOException e) {
            log.error("写入文件失败: {}", filePath, e);
            return "写入失败: " + e.getMessage();
        }
    }

    @McpTool(
            name = "get-file-info",
            description = "获取文件的元信息（大小、修改时间等）"
    )
    public String getFileInfo(
            @McpToolParam(description = "文件完整路径") String filePath
    ) {
        log.info("获取文件信息: {}", filePath);

        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return "文件不存在: " + filePath;
            }

            long size = Files.size(path);
            String lastModified = Files.getLastModifiedTime(path).toString();
            boolean isReadable = Files.isReadable(path);
            boolean isWritable = Files.isWritable(path);

            return String.format(
                    "文件: %s\n大小: %d bytes\n最后修改: %s\n可读: %s\n可写: %s",
                    filePath, size, lastModified, isReadable, isWritable
            );

        } catch (IOException e) {
            log.error("获取文件信息失败: {}", filePath, e);
            return "获取失败: " + e.getMessage();
        }
    }
}
