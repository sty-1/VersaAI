package com.nanhua.spring_ai.service.impl;

import com.nanhua.spring_ai.constant.FileConstant;
import com.nanhua.spring_ai.service.IFileService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final VectorStore vectorStore;

    // 会话id 与 文件名的对应关系，方便查询会话历史时重新加载文件
    private final Properties chatFiles = new Properties();

    // PDF 文件存储目录
    private final Path pdfDir = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf");

    // 元数据文件路径（统一放到 pdfDir 下）
    private final Path metadataFile = pdfDir.resolve("chat-pdf.properties");
    private final Path vectorStoreFile = pdfDir.resolve("chat-pdf.json");

    @Override
    public boolean save(String chatId, Resource resource) {
        // 1.确保目录存在
        try {
            Files.createDirectories(pdfDir);
        } catch (IOException e) {
            log.error("无法创建PDF存储目录", e);
            return false;
        }

        // 2.保存到本地磁盘（tmp/pdf/ 目录下）
        String filename = resource.getFilename();
        Path targetPath = pdfDir.resolve(Objects.requireNonNull(filename));
        if (Files.notExists(targetPath)) {
            try {
                Files.copy(resource.getInputStream(), targetPath);
            } catch (IOException e) {
                log.error("Failed to save PDF resource.", e);
                return false;
            }
        }

        // 3.保存映射关系（只存文件名，读取时拼上 pdfDir）
        chatFiles.put(chatId, filename);

        // 4.写入向量库
        writeToVectorStore(resource, chatId);
        return true;
    }

    @Override
    public Resource getFile(String chatId) {
        String filename = chatFiles.getProperty(chatId);
        if (filename == null) {
            return null;
        }
        return new FileSystemResource(pdfDir.resolve(filename));
    }

    @PostConstruct
    private void init() {
        if (Files.exists(metadataFile)) {
            try (BufferedReader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
                chatFiles.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("加载 chat-pdf.properties 失败", e);
            }
        }
        if (Files.exists(vectorStoreFile)) {
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
            simpleVectorStore.load(vectorStoreFile.toFile());
        }
    }

    @PreDestroy
    private void persistent() {
        try {
            Files.createDirectories(pdfDir);
            chatFiles.store(Files.newBufferedWriter(metadataFile, StandardCharsets.UTF_8),
                    LocalDateTime.now().toString());
            if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                simpleVectorStore.save(vectorStoreFile.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException("持久化失败", e);
        }
    }

    private void writeToVectorStore(Resource resource, String chatId) {
        // 1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 文件源
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );
        // 2.读取PDF文档，拆分为Document
        List<Document> documents = reader.read();
        documents.forEach(document -> document.getMetadata().put("chat_id", chatId));
        // 3.写入向量库
        vectorStore.add(documents);
    }
}