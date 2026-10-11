package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PdfTextExtractorSupport {

    private static final int MAX_CARACTERES_TEXTO = 400_000;

    public String extrairTexto(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BadRequestException("O arquivo de edital não pode ser vazio.");
        }

        try (PDDocument document = Loader.loadPDF(arquivo.getBytes())) {
            if (document.isEncrypted()) {
                throw new BadRequestException("O PDF enviado está protegido por senha e não pode ser processado.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String textoBruto = stripper.getText(document);

            if (textoBruto == null || textoBruto.isBlank()) {
                throw new BadRequestException("Não foi possível extrair texto do PDF. Certifique-se de que o documento não é uma imagem digitalizada sem OCR.");
            }

            return sanitizarTexto(textoBruto);
        } catch (IOException e) {
            throw new BadRequestException("Falha ao processar o arquivo PDF. O arquivo pode estar corrompido ou em formato inválido.");
        }
    }

    private String sanitizarTexto(String texto) {
        String limpo = texto.replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (limpo.length() > MAX_CARACTERES_TEXTO) {
            return limpo.substring(0, MAX_CARACTERES_TEXTO);
        }
        return limpo;
    }
}
