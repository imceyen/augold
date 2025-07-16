package com.global.augold.goldPrice.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.global.augold.goldPrice.dto.GoldPriceDTO;

import java.io.FileInputStream;
import java.util.*;

public class ExcelReader {
    public static List<GoldPriceDTO> readGoldPriceExcel(String filePath) {
        List<GoldPriceDTO> list = new ArrayList<>();
        System.out.println("엑셀에서 읽은 시세 수량: " + list.size());

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) { // 첫 줄은 헤더니까 1부터 시작
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String date = row.getCell(0).getStringCellValue(); // 날짜
                double price = row.getCell(1).getNumericCellValue(); // 금시세 (원/g)

                GoldPriceDTO dto = new GoldPriceDTO();
                dto.setEffectiveDate(date);
                dto.setPricePerGram(price);

                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;

    }

}
