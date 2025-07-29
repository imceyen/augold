from PIL import Image
import pytesseract
import os
import re
import pandas as pd

# Tesseract 설치 경로
pytesseract.pytesseract.tesseract_cmd = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

# 영수증 이미지 폴더 경로
image_folder = r"C:\ncsGlobal\FinalProject\augold\receipt_file"
output_excel_path = r"C:\ncsGlobal\FinalProject\augold\receipt_result.xlsx"

# 가맹점 이름 기반 지출유형 자동 지정
def get_category_from_store(store_name):
    if not store_name:
        return ""
    store_name = store_name.replace(" ", "")
    if any(keyword in store_name for keyword in ["스타벅스", "투썸", "이디야", "커피"]):
        return "접대비"
    elif any(keyword in store_name for keyword in ["다이소", "문구", "생활용품", "마트"]):
        return "소모품비"
    elif any(keyword in store_name for keyword in ["식당", "한식", "음식", "레스토랑"]):
        return "복리후생비"
    else:
        return "비품비"

# OCR 텍스트에서 품명/단가/수량/총금액 추출
def parse_item_info(text):
    pattern = re.compile(r'([가-힣 ]+?)\s*(\d{1,3}(?:,\d{3})*|\d+)\s*원\s*(\d+)\s*(\d{1,3}(?:,\d{3})*|\d+)\s*원')
    match = pattern.search(text)
    if match:
        item_name = re.sub(r'\s+', '', match.group(1))
        unit_price = int(match.group(2).replace(",", ""))
        quantity = int(match.group(3))
        total_price = int(match.group(4).replace(",", ""))
        return item_name, unit_price, quantity, total_price
    return "", 0, 0, 0

# OCR 함수
def ocr_image_to_text(path):
    img = Image.open(path)
    text = pytesseract.image_to_string(img, lang='kor+eng')
    return text


# 전체 이미지 처리 + 엑셀 저장
def process_all_receipts(folder_path):
    data = []
    print("▷ 영수증 인식 중 ... ")
    for filename in os.listdir(folder_path):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
            filepath = os.path.join(folder_path, filename)
            try:
                ocr_text = ocr_image_to_text(filepath)

                store_match = re.search(r'가 맹 점\s*:\s*([가-힣 ]+)', ocr_text)
                category_match = re.search(r'(복리후생비|소모품비|비품비|접대비)', ocr_text)
                date_match = re.search(r'\d{4}/\d{2}/\d{2}|\d{4}-\d{2}-\d{2}', ocr_text)
                item_name, unit_price, quantity, total_price = parse_item_info(ocr_text)

                store = store_match.group(1).strip().replace(" ", "") if store_match else ""
                category = category_match.group(1) if category_match else get_category_from_store(store)
                date = date_match.group(0).replace("/", "-") if date_match else ""

                data.append([
                    store,
                    category,
                    date,
                    item_name,
                    unit_price,
                    quantity,
                    total_price
                ])
            except Exception as e:
                print(f"⚠ 오류 발생: {e}")

    df = pd.DataFrame(data, columns=["가맹점", "지출유형", "날짜", "품명", "단가", "수량", "총금액"])
    df.to_excel(output_excel_path, index=False)

    print("▷ 영수증 인식 성공!")
    print(f"▷ 엑셀 저장 완료 → {output_excel_path}")

# 실행
if __name__ == "__main__":
    process_all_receipts(image_folder)
