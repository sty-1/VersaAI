package com.nanhua.spring_ai.tools;

import com.nanhua.spring_ai.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component

public class FileOperationTool {

    private final Path fileDir;

    public FileOperationTool() {
        this.fileDir = Paths.get(FileConstant.FILE_SAVE_DIR, "file");
    }

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        try {
            Path filePath = fileDir.resolve(fileName);
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            Path filePath = fileDir.resolve(fileName);
            Files.createDirectories(fileDir);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return "File written successfully to: " + filePath;
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}