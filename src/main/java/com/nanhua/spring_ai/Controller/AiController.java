package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.agent.SuperManus;
import com.nanhua.spring_ai.constant.FileConstant;
import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@Slf4j
@RequestMapping("/ai")
public class AiController {

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ChatModel deepSeekChatModel;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @GetMapping("/manus/chat/sync")
    public String doChatWithManusSync(String message, @RequestParam(required = false) String chatId) {
        log.info("同步请求 - message: {}, chatId: {}", message, chatId);
        chatHistoryRepository.save("manus", chatId);
        SuperManus superManus = new SuperManus(allTools, deepSeekChatModel, chatMemory);
        superManus.setChatId(chatId);
        return superManus.run(message);
    }

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, @RequestParam(required = false) String chatId) {
        log.info("SSE 流式请求 - message: {}, chatId: {}", message, chatId);
        chatHistoryRepository.save("manus", chatId);
        SuperManus superManus = new SuperManus(allTools, deepSeekChatModel, chatMemory);
        superManus.setChatId(chatId);
        return superManus.runStream(message);
    }

    /**
     * 文件下载接口：将 Agent 生成的文件提供给前端下载。
     * path 参数为相对于 tmp 目录的路径，如 "file/report.md" 或 "download/image.png"
     */
    @GetMapping("/manus/file/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@RequestParam String path) {
        Path baseDir = Paths.get(FileConstant.FILE_SAVE_DIR).normalize();
        Path filePath = baseDir.resolve(path).normalize();

        // 安全检查：确保文件在 tmp 目录内
        if (!filePath.startsWith(baseDir)) {
            log.warn("非法文件访问尝试: {}", path);
            return ResponseEntity.badRequest().build();
        }

        org.springframework.core.io.Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            log.warn("文件不存在: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        String filename = filePath.getFileName().toString();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
