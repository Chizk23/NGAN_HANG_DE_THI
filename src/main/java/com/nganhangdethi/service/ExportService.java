package com.nganhangdethi.service;

import com.nganhangdethi.model.Exam;
import com.nganhangdethi.model.Question;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public ExportService() {
        // Constructor
    }

    // --- PHƯƠNG THỨC CHÍNH CHO TẠO BỘ ĐỀ XÁO TRỘN ---

    public List<Question> exportSingleExamVersionAndGetShuffled(Exam originalExam, String examFilePath, String format, boolean includeAnswersInExamFile) {
        if (originalExam == null || originalExam.getQuestions() == null || originalExam.getQuestions().isEmpty()) {
            logger.error("Đề gốc không hợp lệ hoặc không có câu hỏi để tạo phiên bản.");
            return null;
        }
        List<Question> questionsForThisVersion = new ArrayList<>(originalExam.getQuestions());
        Collections.shuffle(questionsForThisVersion);

        boolean success;
        if ("pdf".equalsIgnoreCase(format)) {
            success = exportExamToPdfInternal(originalExam, questionsForThisVersion, examFilePath, includeAnswersInExamFile);
        } else if ("docx".equalsIgnoreCase(format)) {
            success = exportExamToDocxInternal(originalExam, questionsForThisVersion, examFilePath, includeAnswersInExamFile);
        } else {
            logger.error("Định dạng file không được hỗ trợ khi tạo phiên bản đề: {}", format);
            return null;
        }

        if (success) {
            logger.info("Đã xuất thành công phiên bản đề thi tới: {}", examFilePath);
            return questionsForThisVersion;
        } else {
            logger.error("Lỗi khi xuất phiên bản đề thi tới: {}", examFilePath);
            return null;
        }
    }

    public boolean exportAnswerKeyForShuffledVersion(Exam examInfo, List<Question> shuffledQuestions, String answerKeyFilePath, String format, int versionNumber, boolean includeExplanation) {
        if (examInfo == null || shuffledQuestions == null || shuffledQuestions.isEmpty()) {
            logger.error("Dữ liệu không hợp lệ để xuất đáp án: examInfo hoặc shuffledQuestions là null/rỗng cho phiên bản {}.", versionNumber);
            return false;
        }
        logger.info("Bắt đầu tạo đáp án phiên bản {} cho đề '{}' tại: {}, IncludeExplanation: {}", (versionNumber == 0 ? "gốc" : versionNumber) , examInfo.getExamName(), answerKeyFilePath, includeExplanation);

        if ("pdf".equalsIgnoreCase(format)) {
            return exportAnswerKeyToPdfInternal(examInfo, shuffledQuestions, answerKeyFilePath, versionNumber, includeExplanation);
        } else if ("docx".equalsIgnoreCase(format)) {
            return exportAnswerKeyToDocxInternal(examInfo, shuffledQuestions, answerKeyFilePath, versionNumber, includeExplanation);
        } else {
            logger.error("Định dạng file đáp án không được hỗ trợ: {}", format);
            return false;
        }
    }


    // --- PHƯƠNG THỨC XUẤT ĐƠN LẺ (CÓ THỂ GỌI TỪ ExamManagementPanel) ---
    // Giờ đây nhận thêm includeAnswers
    public boolean exportExamToPdf(Exam exam, String filePath, boolean shuffle, boolean includeAnswers) {
        List<Question> questionsToUse = new ArrayList<>(exam.getQuestions());
        if (shuffle) {
            Collections.shuffle(questionsToUse);
        }
        return exportExamToPdfInternal(exam, questionsToUse, filePath, includeAnswers);
    }

    public boolean exportExamToDocx(Exam exam, String filePath, boolean shuffle, boolean includeAnswers) {
        List<Question> questionsToUse = new ArrayList<>(exam.getQuestions());
        if (shuffle) {
            Collections.shuffle(questionsToUse);
        }
        return exportExamToDocxInternal(exam, questionsToUse, filePath, includeAnswers);
    }

    // --- CÁC PHƯƠNG THỨC NỘI BỘ ĐỂ TẠO FILE ĐỀ THI ---
    private boolean exportExamToPdfInternal(Exam examInfo, List<Question> questionsToExport, String filePath, boolean includeAnswers) {
        logger.info("ExportService: Bắt đầu tạo PDF cho đề: '{}', file: {}, IncludeAnswersInFile: {}", examInfo.getExamName(), filePath, includeAnswers);
        try (PDDocument document = new PDDocument()) {
            PDType0Font fontForPdf;
            try {
                fontForPdf = UIUtils.getJapanesePdfFont(document);
            } catch (IOException e) {
                logger.error("Lỗi nghiêm trọng: Không thể load font PDF '{}'. Lỗi: {}", Constants.FONT_RESOURCE_PATH, e.getMessage());
                return false;
            }

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
            int pageCount = 1;

            float yPosition = currentPage.getMediaBox().getUpperRightY() - 40;
            final float margin = 40;
            final float bottomMargin = 60;
            final float contentWidth = currentPage.getMediaBox().getWidth() - 2 * margin;

            final float headerFontSize = 18f;
            final float subHeaderFontSize = 14f;
            final float infoFontSize = 11f;
            final float questionFontSize = 11f;
            final float answerKeyFontSize = 10f; // Font cho đáp án và giải thích khi in kèm
            final float leadingDefault = 1.5f * infoFontSize;
            final float leadingQuestion = 1.4f * questionFontSize;
            final float leadingAnswerKey = 1.3f * answerKeyFontSize;


            // --- HEADER ĐỀ THI ---
            yPosition = drawPdfExamHeader(contentStream, currentPage, examInfo, fontForPdf, yPosition, margin, contentWidth, headerFontSize, subHeaderFontSize, infoFontSize, leadingDefault);

            // --- DANH SÁCH CÂU HỎI ---
            int questionNumber = 1;
            for (Question q : questionsToExport) {
                String questionFullText = questionNumber + ". " + q.getQuestionText();
                List<String> questionLines = breakTextIntoLines(questionFullText, contentWidth, fontForPdf, questionFontSize);
                List<String> optALines = breakTextIntoLines("   A. " + (q.getOptionA() != null ? q.getOptionA() : ""), contentWidth - 20, fontForPdf, questionFontSize);
                List<String> optBLines = breakTextIntoLines("   B. " + (q.getOptionB() != null ? q.getOptionB() : ""), contentWidth - 20, fontForPdf, questionFontSize);
                List<String> optCLines = (q.getOptionC() != null && !q.getOptionC().isEmpty()) ? breakTextIntoLines("   C. " + q.getOptionC(), contentWidth - 20, fontForPdf, questionFontSize) : new ArrayList<>();
                List<String> optDLines = (q.getOptionD() != null && !q.getOptionD().isEmpty()) ? breakTextIntoLines("   D. " + q.getOptionD(), contentWidth - 20, fontForPdf, questionFontSize) : new ArrayList<>();
                
                List<String> answerKeyTextLines = new ArrayList<>();
                List<String> explanationTextLines = new ArrayList<>();

                if (includeAnswers) {
                    answerKeyTextLines = breakTextIntoLines("Đáp án đúng: " + q.getCorrectAnswer(), contentWidth - 30, fontForPdf, answerKeyFontSize);
                    if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                        explanationTextLines = breakTextIntoLines("Giải thích: " + q.getExplanation(), contentWidth - 40, fontForPdf, answerKeyFontSize);
                    }
                }

                float estimatedHeight = (questionLines.size() + optALines.size() + optBLines.size() + optCLines.size() + optDLines.size()) * leadingQuestion +
                                        (includeAnswers ? (answerKeyTextLines.size() * leadingAnswerKey) + (explanationTextLines.size() * leadingAnswerKey) + (leadingQuestion * 0.5f) : 0) +
                                        leadingQuestion;

                if (yPosition < bottomMargin + estimatedHeight) {
                    addPageNumber(contentStream, pageCount, currentPage, margin, fontForPdf, 8f);
                    contentStream.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = currentPage.getMediaBox().getUpperRightY() - margin;
                    pageCount++;
                }

                // In câu hỏi
                yPosition = drawPdfTextLines(contentStream, questionLines, margin, yPosition, fontForPdf, questionFontSize, leadingQuestion, Color.BLACK, currentPage, document, margin, bottomMargin);
                float optionIndent = margin + 20;
                yPosition = drawPdfTextLines(contentStream, optALines, optionIndent, yPosition, fontForPdf, questionFontSize, leadingQuestion, Color.BLACK, currentPage, document, margin, bottomMargin);
                yPosition = drawPdfTextLines(contentStream, optBLines, optionIndent, yPosition, fontForPdf, questionFontSize, leadingQuestion, Color.BLACK, currentPage, document, margin, bottomMargin);
                if (!optCLines.isEmpty()) yPosition = drawPdfTextLines(contentStream, optCLines, optionIndent, yPosition, fontForPdf, questionFontSize, leadingQuestion, Color.BLACK, currentPage, document, margin, bottomMargin);
                if (!optDLines.isEmpty()) yPosition = drawPdfTextLines(contentStream, optDLines, optionIndent, yPosition, fontForPdf, questionFontSize, leadingQuestion, Color.BLACK, currentPage, document, margin, bottomMargin);

                if (includeAnswers) {
                    yPosition -= leadingQuestion * 0.5f; // Khoảng cách nhỏ
                    // Kiểm tra sang trang trước khi vẽ đáp án
                    float ansHeight = (answerKeyTextLines.size() + explanationTextLines.size()) * leadingAnswerKey;
                    if (yPosition < bottomMargin + ansHeight) {
                        addPageNumber(contentStream, pageCount, currentPage, margin, fontForPdf, 8f); contentStream.close();
                        currentPage = new PDPage(PDRectangle.A4); document.addPage(currentPage);
                        contentStream = new PDPageContentStream(document, currentPage);
                        yPosition = currentPage.getMediaBox().getUpperRightY() - margin; pageCount++;
                    }
                    yPosition = drawPdfTextLines(contentStream, answerKeyTextLines, optionIndent, yPosition, fontForPdf, answerKeyFontSize, leadingAnswerKey, Color.BLUE.darker(), currentPage, document, margin, bottomMargin);
                    if (!explanationTextLines.isEmpty()) {
                        yPosition = drawPdfTextLines(contentStream, explanationTextLines, optionIndent + 10, yPosition, fontForPdf, answerKeyFontSize, leadingAnswerKey, Color.DARK_GRAY, currentPage, document, margin, bottomMargin);
                    }
                }
                yPosition -= leadingQuestion;
                questionNumber++;
            }

            addPageNumber(contentStream, pageCount, currentPage, margin, fontForPdf, 8f);
            contentStream.close();
            document.save(filePath);
            logger.info("Đã xuất PDF đề thi thành công: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi xuất PDF đề thi: {}. Lỗi: {}", filePath, e.getMessage(), e);
            return false;
        }
    }
    
    private float drawPdfExamHeader(PDPageContentStream contentStream, PDPage page, Exam examInfo, PDType0Font font, float yStart, float margin, float contentWidth, float headerFontSize, float subHeaderFontSize, float infoFontSize, float leading) throws IOException {
        // ... (Giữ nguyên logic vẽ header như phiên bản trước)
        float y = yStart;
        contentStream.beginText(); contentStream.setFont(font, infoFontSize);
        contentStream.newLineAtOffset(margin, y);
        contentStream.showText("TRƯỜNG: ............................................");
        contentStream.endText(); y -= leading * 1.5f;
        contentStream.beginText(); contentStream.setFont(font, infoFontSize);
        contentStream.newLineAtOffset(margin, y);
        contentStream.showText("Họ và tên: ..................................................................");
        contentStream.newLineAtOffset(contentWidth / 1.8f, 0);
        contentStream.showText("Lớp: .....................................");
        contentStream.endText(); y -= leading;
        contentStream.beginText(); contentStream.setFont(font, infoFontSize);
        contentStream.newLineAtOffset(margin, y);
        contentStream.showText("MSSV/SBD: .........................................................");
        contentStream.newLineAtOffset(contentWidth / 1.8f, 0);
        contentStream.showText("Ngày thi: ......./......./..............");
        contentStream.endText(); y -= leading * 2f;

        String examTitle = "ĐỀ THI: " + examInfo.getExamName().toUpperCase();
        float titleWidth = font.getStringWidth(examTitle) / 1000f * headerFontSize;
        contentStream.beginText(); contentStream.setFont(font, headerFontSize);
        contentStream.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2, y);
        contentStream.showText(examTitle);
        contentStream.endText(); y -= leading * 1.5f;

        String levelText = "Cấp độ: " + (examInfo.getLevelTarget() != null ? examInfo.getLevelTarget() : "N/A");
        float levelWidth = font.getStringWidth(levelText) / 1000f * subHeaderFontSize;
        contentStream.beginText(); contentStream.setFont(font, subHeaderFontSize);
        contentStream.newLineAtOffset((page.getMediaBox().getWidth() - levelWidth) / 2, y);
        contentStream.showText(levelText);
        contentStream.endText(); y -= leading * 1.8f;

        contentStream.moveTo(margin, y);
        contentStream.lineTo(margin + contentWidth, y);
        contentStream.stroke();
        y -= leading * 0.8f;
        return y;
    }

    private boolean exportExamToDocxInternal(Exam examInfo, List<Question> questionsToExport, String filePath, boolean includeAnswers) {
        logger.info("ExportService: Bắt đầu tạo DOCX cho đề: '{}', file: {}, IncludeAnswers: {}", examInfo.getExamName(), filePath, includeAnswers);
        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(filePath)) {
            // --- PHẦN THÔNG TIN THÍ SINH ---
            XWPFParagraph studentInfoPar = document.createParagraph();
            setParagraphSpacing(studentInfoPar, 0, 120);
            XWPFRun studentInfoRun = studentInfoPar.createRun();
            studentInfoRun.setFontSize(12);
            studentInfoRun.setText("TRƯỜNG: ......................................................................................................................");
            studentInfoRun.addBreak();
            studentInfoRun.setText("Họ và tên: ............................................................................");
            studentInfoRun.addTab(); studentInfoRun.addTab();
            studentInfoRun.setText("Lớp: .....................................");
            studentInfoRun.addBreak();
            studentInfoRun.setText("MSSV/SBD: ......................................................................");
            studentInfoRun.addTab(); studentInfoRun.addTab();
            studentInfoRun.setText("Ngày thi: ......./......./..............");

            // --- TIÊU ĐỀ ĐỀ THI ---
            XWPFParagraph titlePar = document.createParagraph();
            titlePar.setAlignment(ParagraphAlignment.CENTER);
            setParagraphSpacing(titlePar, 240, 120);
            XWPFRun titleRun = titlePar.createRun();
            titleRun.setText("ĐỀ THI: " + examInfo.getExamName().toUpperCase());
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            XWPFParagraph levelPar = document.createParagraph();
            levelPar.setAlignment(ParagraphAlignment.CENTER);
            setParagraphSpacing(levelPar, 0, 240);
            XWPFRun levelRun = levelPar.createRun();
            levelRun.setText("Cấp độ: " + (examInfo.getLevelTarget() != null ? examInfo.getLevelTarget() : "N/A"));
            levelRun.setFontSize(14);
            levelRun.setBold(true);

            if (examInfo.getDescription() != null && !examInfo.getDescription().isEmpty()) {
                XWPFParagraph descPar = document.createParagraph();
                descPar.setAlignment(ParagraphAlignment.LEFT);
                 setParagraphSpacing(descPar, 0, 180);
                XWPFRun descRun = descPar.createRun();
                descRun.setFontSize(12);
                descRun.setItalic(true);
                descRun.setText("Mô tả: " + examInfo.getDescription());
            }
            createEmptyParagraphWithSpacing(document, 240); // Dòng trống

            // Câu hỏi
            int questionNumber = 1;
            for (Question q : questionsToExport) {
                XWPFParagraph qPar = document.createParagraph();
                setParagraphSpacing(qPar, 120, 60);
                XWPFRun qRun = qPar.createRun();
                qRun.setFontSize(12);
                qRun.setText(questionNumber + ". " + q.getQuestionText());

                if (q.getOptionA() != null) addOptionToDocx(document, "A", q.getOptionA());
                if (q.getOptionB() != null) addOptionToDocx(document, "B", q.getOptionB());
                if (q.getOptionC() != null && !q.getOptionC().isEmpty()) addOptionToDocx(document, "C", q.getOptionC());
                if (q.getOptionD() != null && !q.getOptionD().isEmpty()) addOptionToDocx(document, "D", q.getOptionD());

                if (includeAnswers) {
                    XWPFParagraph ansPar = document.createParagraph();
                    ansPar.setIndentationLeft(360);
                    setParagraphSpacing(ansPar, 60, 0); // Không cần spacing after nếu có giải thích
                    XWPFRun ansRun = ansPar.createRun();
                    ansRun.setFontSize(11);
                    ansRun.setColor("0000FF"); // Blue
                    ansRun.setItalic(true);
                    ansRun.setText("Đáp án đúng: " + q.getCorrectAnswer());

                    if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                        // Tạo paragraph mới cho giải thích để có thể set spacing khác
                        XWPFParagraph expPar = document.createParagraph();
                        expPar.setIndentationLeft(360 + 180); // Thụt vào thêm
                        setParagraphSpacing(expPar, 0, 60);
                        XWPFRun expRun = expPar.createRun();
                        expRun.setFontSize(11);
                        expRun.setItalic(true);
                        expRun.setColor("555555"); // Dark Gray
                        expRun.setText("Giải thích: " + q.getExplanation());
                    }
                }
                if (questionNumber < questionsToExport.size() || !includeAnswers) { // Thêm dòng trống sau câu hỏi
                    createEmptyParagraphWithSpacing(document, 120);
                }
                questionNumber++;
            }

            document.write(out);
            logger.info("Đã xuất DOCX thành công: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi xuất DOCX nội bộ: {}. Lỗi: {}", filePath, e.getMessage(), e);
            return false;
        }
    }
    
    private void createEmptyParagraphWithSpacing(XWPFDocument document, int spacingAfter) {
        XWPFParagraph p = document.createParagraph();
        setParagraphSpacing(p, 0, spacingAfter);
    }


    private void addOptionToDocx(XWPFDocument document, String label, String text) {
        XWPFParagraph optPar = document.createParagraph();
        optPar.setIndentationLeft(360); // Thụt lề khoảng 0.25 inch (360 twips)
        setParagraphSpacing(optPar, 30, 30); // Khoảng cách nhỏ trên dưới
        XWPFRun optRun = optPar.createRun();
        optRun.setFontSize(12);
        optRun.setText(label + ". " + text);
    }

    private void setParagraphSpacing(XWPFParagraph paragraph, int spacingBefore, int spacingAfter) {
        CTPPr ppr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        if (spacingBefore >= 0) spacing.setBefore(BigInteger.valueOf(spacingBefore));
        if (spacingAfter >= 0) spacing.setAfter(BigInteger.valueOf(spacingAfter));
        spacing.setLineRule(STLineSpacingRule.AUTO); // Hoặc AT_LEAST, EXACT
        // spacing.setLine(BigInteger.valueOf(240)); // 240 twips = single line spacing
    }

    // --- CÁC PHƯƠNG THỨC NỘI BỘ ĐỂ TẠO FILE ĐÁP ÁN ---
    private boolean exportAnswerKeyToPdfInternal(Exam examInfo, List<Question> questions, String filePath, int versionNumber, boolean includeExplanation) {
        logger.info("ExportService: Bắt đầu tạo PDF đáp án PB {} cho đề: '{}', file: {}, IncludeExp: {}", versionNumber, examInfo.getExamName(), filePath, includeExplanation);
         try (PDDocument document = new PDDocument()) {
            PDType0Font fontForPdf;
            try {
                fontForPdf = UIUtils.getJapanesePdfFont(document);
            } catch (IOException e) {
                logger.error("Lỗi font khi xuất PDF đáp án: {}. Lỗi: {}", filePath, e.getMessage());
                return false;
            }

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
            int pageCount = 1;

            float yPosition = currentPage.getMediaBox().getUpperRightY() - 50;
            final float margin = 50;
            final float bottomMargin = 60;
            final float contentWidth = currentPage.getMediaBox().getWidth() - 2 * margin;
            final float titleFontSize = 14f;
            final float regularFontSize = 10f;
            final float leading = 1.4f * regularFontSize; // Giảm leading cho đáp án

            contentStream.beginText();
            contentStream.setFont(fontForPdf, titleFontSize);
            contentStream.newLineAtOffset(margin, yPosition);
            String title = "ĐÁP ÁN" + (includeExplanation ? " & GIẢI THÍCH" : "") + " - Đề: " + examInfo.getExamName() + (versionNumber > 0 ? String.format(" (Phiên Bản %02d)", versionNumber) : "");
            contentStream.showText(title);
            contentStream.endText();
            yPosition -= leading * 2.5f;

            int qNum = 1;
            for (Question q : questions) {
                String answerLine = String.format("%d. (ID: %d) Đáp án: %s", qNum, q.getId(), q.getCorrectAnswer());
                List<String> answerLines = List.of(answerLine); // breakTextIntoLines(answerLine, contentWidth, fontForPdf, regularFontSize);

                List<String> explanationLines = new ArrayList<>();
                if (includeExplanation && q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                    explanationLines = breakTextIntoLines("   Giải thích: " + q.getExplanation(), contentWidth - 10, fontForPdf, regularFontSize);
                }
                
                float estimatedHeight = (answerLines.size() + explanationLines.size()) * leading + (leading * 0.5f);

                if (yPosition < bottomMargin + estimatedHeight) {
                    addPageNumber(contentStream, pageCount, currentPage, margin, fontForPdf, 8f);
                    contentStream.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = currentPage.getMediaBox().getUpperRightY() - margin;
                    pageCount++;
                }
                
                yPosition = drawPdfTextLines(contentStream, answerLines, margin, yPosition, fontForPdf, regularFontSize, leading, Color.BLACK, currentPage, document, margin, bottomMargin);
                if (!explanationLines.isEmpty()) {
                    yPosition = drawPdfTextLines(contentStream, explanationLines, margin + 10, yPosition, fontForPdf, regularFontSize, leading, Color.DARK_GRAY, currentPage, document, margin, bottomMargin);
                }
                yPosition -= leading * 0.5f;
                qNum++;
            }
            addPageNumber(contentStream, pageCount, currentPage, margin, fontForPdf, 8f);
            contentStream.close();
            document.save(filePath);
            logger.info("Đã xuất PDF đáp án thành công: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi xuất PDF đáp án: {}. Lỗi: {}", filePath, e.getMessage(), e);
            return false;
        }
    }

    private boolean exportAnswerKeyToDocxInternal(Exam examInfo, List<Question> questions, String filePath, int versionNumber, boolean includeExplanation) {
        logger.info("ExportService: Bắt đầu tạo DOCX đáp án PB {} cho đề: '{}', file: {}, IncludeExp: {}", versionNumber, examInfo.getExamName(), filePath, includeExplanation);
        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(filePath)) {
            XWPFParagraph titlePar = document.createParagraph();
            titlePar.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePar.createRun();
            String title = "ĐÁP ÁN" + (includeExplanation ? " & GIẢI THÍCH" : "") + " - Đề: " + examInfo.getExamName() + (versionNumber > 0 ? String.format(" (Phiên Bản %02d)", versionNumber) : "");
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            // titleRun.setFontFamily("Times New Roman");
            createEmptyParagraphWithSpacing(document, 240);


            int qNum = 1;
            for (Question q : questions) {
                XWPFParagraph par = document.createParagraph();
                setParagraphSpacing(par, 0, 60);
                XWPFRun run = par.createRun();
                run.setFontSize(11);
                // run.setFontFamily("Times New Roman");
                run.setText(String.format("%d. (ID gốc: %d)", qNum, q.getId()));
                run.addTab(); // Thêm tab
                run.setText("Đáp án: " + q.getCorrectAnswer());

                if (includeExplanation && q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                    XWPFParagraph expPar = document.createParagraph(); // Đoạn mới cho giải thích
                    expPar.setIndentationLeft(360); // Thụt lề giải thích
                    setParagraphSpacing(expPar, 0, 60);
                    XWPFRun expRun = expPar.createRun();
                    expRun.setFontSize(10);
                    expRun.setItalic(true);
                    expRun.setColor("333333"); // Màu xám đậm
                    expRun.setText("Giải thích: " + q.getExplanation());
                }
                qNum++;
            }
            document.write(out);
            logger.info("Đã xuất DOCX đáp án thành công: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi xuất DOCX đáp án: {}. Lỗi: {}", filePath, e.getMessage(), e);
            return false;
        }
    }

    // --- PHƯƠNG THỨC HỖ TRỢ ---
    // Cập nhật drawPdfTextLines để nó có thể tự tạo trang mới nếu cần
    private float drawPdfTextLines(PDPageContentStream currentCS, List<String> lines, float x, float yStart,
                                   PDType0Font font, float fontSize, float leading, Color color,
                                   PDPage currentPageHolder, PDDocument document, float pageMargin, float bottomPageMargin) throws IOException {
        float y = yStart;
        if (lines == null || lines.isEmpty()) return y;

        PDPageContentStream cs = currentCS; // Sử dụng content stream hiện tại
        PDPage page = currentPageHolder;    // Sử dụng trang hiện tại

        cs.setNonStrokingColor(color);
        for (String line : lines) {
            if (y < bottomPageMargin + leading) { // Nếu không đủ chỗ cho dòng tiếp theo
                cs.close(); // Đóng stream của trang cũ
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                cs = new PDPageContentStream(document, page); // Mở stream cho trang mới
                y = page.getMediaBox().getUpperRightY() - pageMargin; // Reset y
                logger.debug("PDF: Đã tạo trang mới khi vẽ text lines.");
                // Vẽ lại header trên trang mới nếu cần (có thể truyền thêm thông tin ExamInfo)
                 // y = drawPdfExamHeader(cs, page, examInfo, font, y, pageMargin, page.getMediaBox().getWidth() - 2*pageMargin, ...);
                cs.setNonStrokingColor(color); // Set lại màu cho stream mới
            }
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(line);
            cs.endText();
            y -= leading;
        }
        // Không đóng contentStream ở đây, nó sẽ được đóng bởi hàm gọi bên ngoài sau khi tất cả nội dung của trang được vẽ
        return y;
    }


    private List<String> breakTextIntoLines(String text, float maxWidth, PDType0Font font, float fontSize) throws IOException {
        List<String> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty() || maxWidth <= 0) {
            if (text != null && !text.trim().isEmpty()) result.add(text.trim());
            else result.add(""); // Trả về dòng rỗng nếu text là null hoặc toàn khoảng trắng
            return result;
        }

        String[] paragraphs = text.split("\\r?\\n"); // Tách theo dòng mới trước
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty() && result.size() > 0 && !result.get(result.size()-1).trim().isEmpty()) {
                 result.add(""); // Giữ lại dòng trống nếu nó không phải là dòng trống liên tiếp ở đầu
                 continue;
            }

            String[] words = paragraph.split("(?<=\\s)|(?<=-)|(?=\\p{Punct})|(?<=\\p{Punct})(?=\\w)|(?<=\\p{Lo})(?=\\p{Lo})");
            StringBuilder line = new StringBuilder();
            float currentLineWidth = 0;

            for (String word : words) {
                if (word.isEmpty()) continue;

                float wordWidth = font.getStringWidth(word) / 1000f * fontSize;
                String wordWithPotentialSpace = (line.length() > 0 && !isPunctuation(word.charAt(0)) && !line.toString().endsWith(" ") ) ? " " + word : word;
                float testWidth = font.getStringWidth(line.toString() + wordWithPotentialSpace) / 1000f * fontSize;


                if (testWidth <= maxWidth) {
                    if (line.length() > 0 && !isPunctuation(word.charAt(0)) && !line.toString().endsWith(" ")) {
                        line.append(" ");
                    }
                    line.append(word);
                } else {
                    if (line.length() > 0) {
                        result.add(line.toString().trim());
                        line = new StringBuilder(word.trim());
                    } else {
                        // Từ đơn lẻ dài hơn cả dòng, cố gắng cắt từ
                        String remainingWord = word.trim();
                        while (remainingWord.length() > 0) {
                            int breakPoint = 0;
                            for (int k = 1; k <= remainingWord.length(); k++) {
                                if (font.getStringWidth(remainingWord.substring(0, k)) / 1000f * fontSize > maxWidth) {
                                    break;
                                }
                                breakPoint = k;
                            }
                            if (breakPoint == 0 && remainingWord.length() > 0) { // Không thể vừa ký tự nào
                                result.add(remainingWord.substring(0,1)); // Thêm 1 ký tự
                                remainingWord = remainingWord.substring(1);
                            } else if (breakPoint > 0) {
                                result.add(remainingWord.substring(0, breakPoint));
                                remainingWord = remainingWord.substring(breakPoint);
                            } else { // Should not happen
                                break;
                            }
                        }
                        line = new StringBuilder(); // Reset
                    }
                }
            }
            if (line.length() > 0) {
                result.add(line.toString().trim());
            }
        }
        if (result.isEmpty() && text != null && !text.isEmpty()) { // Trường hợp text rất ngắn, không có space
             result.add(text);
        }
        return result;
    }
    
    private boolean isPunctuation(char c) {
        return ".?!,:;\"'())".indexOf(c) != -1;
    }
    
    private int calculateApproxLines(String text, float maxWidth, PDType0Font font, float fontSize) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return breakTextIntoLines(text, maxWidth, font, fontSize).size();
        } catch (IOException e) {
            logger.warn("Không thể tính số dòng chính xác (IOException): {}", e.getMessage());
            float avgCharWidth = fontSize * 0.55f; // Ước lượng
            if (avgCharWidth <=0) return text.length();
            return (int) Math.ceil(text.length() / (maxWidth / avgCharWidth));
        }
    }

    private void addPageNumber(PDPageContentStream contentStream, int pageNum, PDPage page, float margin, PDType0Font font, float fontSize) {
        try {
            String text = String.format("- Trang %d -", pageNum);
            float textWidth = font.getStringWidth(text) / 1000f * fontSize;
            float x = (page.getMediaBox().getWidth() - textWidth) / 2;
            float y = margin / 2f - 5; // Dịch xuống một chút so với mép dưới

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        } catch (IOException e) {
            logger.error("Không thể vẽ số trang: {}", e.getMessage());
        }
    }
}