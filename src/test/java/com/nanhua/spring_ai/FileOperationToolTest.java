package com.nanhua.spring_ai;

import com.nanhua.spring_ai.tools.FileOperationTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class FileOperationToolTest {

    @Test
    public void testReadFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "";
        String result = tool.readFile(fileName);
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void testWriteFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "";
        String content = "";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
    }
}

