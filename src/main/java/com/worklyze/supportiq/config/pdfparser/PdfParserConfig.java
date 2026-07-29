package com.worklyze.supportiq.config.pdfparser;

import org.apache.tika.parser.pdf.PDFParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PdfParserConfig {

    @Bean
    public PDFParser pdfParser() {
        return new PDFParser();
    }
}
