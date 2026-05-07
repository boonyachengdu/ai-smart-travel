package com.boonya.business.trip.dialog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;

import java.util.List;

@Slf4j
@Import({ChatMemory.class})
@SpringBootTest
public class DocumentTest {


    public void test(){
//        // 支持 PDF/Word/Markdown 自动解析
//        PdfDocumentReader pdfReader = new PdfDocumentReader(
//                new FileSystemResource("policies/travel-policy.pdf")
//        );
//        List<Document> docs = pdfReader.get();
//
//// 自动切片并向量化
//        documentWriter.write(docs);

    }
}
