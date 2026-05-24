package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import com.nanhua.spring_ai.service.IFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai/pdf")
public class PdfController {

    private final ChatClient pdfChatClient;
    private final IFileService fileService;
    private final ChatHistoryRepository chatHistoryRepository;

    @PostMapping("/upload/{chatId}")
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable String chatId,
            @RequestParam("file") MultipartFile file) {
        log.info("PDF上传 - chatId: {}, filename: {}", chatId, file.getOriginalFilename());
        boolean saved = fileService.save(chatId, file.getResource());
        if (saved) {
            chatHistoryRepository.save("pdf", chatId);
            return ResponseEntity.ok(Map.of("chatId", chatId, "success", true));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false));
    }

    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public reactor.core.publisher.Flux<String> chat(
            @RequestParam String prompt,
            @RequestParam(required = false) String chatId) {
        log.info("PDF问答 - prompt: {}, chatId: {}", prompt, chatId);
        return pdfChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> getFile(@PathVariable String chatId) {
        Resource file = fileService.getFile(chatId);
        if (file != null && file.exists()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file);
        }
        return ResponseEntity.notFound().build();
    }
}
