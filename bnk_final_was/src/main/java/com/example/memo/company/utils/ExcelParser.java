package com.example.memo.company.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.company.dto.ContributionItemDto;
import com.example.memo.company.dto.SubscriberDto;

public class ExcelParser {
	
	/* 가입자 명부 등록 */
    public static List<SubscriberDto> parse(MultipartFile file) throws Exception {
        List<SubscriberDto> result = new ArrayList<>();

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            SubscriberDto dto = SubscriberDto.builder()
                .ssn(getCellValue(row.getCell(0)))
                .name(getCellValue(row.getCell(1)))
                .entryDate(getCellValue(row.getCell(2)))
                .baseDate(getCellValue(row.getCell(3)))
                .annualSalary(getCellValue(row.getCell(4)))
                .employmentType(getCellValue(row.getCell(5)))
                .email(getCellValue(row.getCell(6)))
                .phone(getCellValue(row.getCell(7)))
                .status(getCellValue(row.getCell(8)))
                .joinDate(getCellValue(row.getCell(9)))
                .build();

            result.add(dto);
        }

        return result;
    }
    
    /**
     * DC 부담금 납입용 엑셀 파일 파싱
     * @param file 사용자가 업로드한 엑셀 파일
     * @return 엑셀 데이터가 담긴 DTO 목록
     */
    public static List<ContributionItemDto> parseContribution(MultipartFile file) throws Exception {
        List<ContributionItemDto> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 3행부터
                Row row = sheet.getRow(i);
                if (row == null) continue;
                ContributionItemDto dto = ContributionItemDto.builder()
                        .name(getCellValue(row.getCell(0)))
                        .ssn(getCellValue(row.getCell(1)))
                        .amount(normalizeNum(getCellValue(row.getCell(2))))
                        .performanceBonus(normalizeNum(getCellValue(row.getCell(3))))
                        .annualSalary(normalizeNum(getCellValue(row.getCell(4))))
                        .build();
                result.add(dto);
            }
        }
        return result;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            } else {
                return String.valueOf((long) cell.getNumericCellValue());
            }
        }
        return cell.getStringCellValue().trim();
    }

    private static String normalizeNum(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[,\\s]", "");
    }
}
