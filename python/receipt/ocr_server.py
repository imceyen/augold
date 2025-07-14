from PIL import Image
import pytesseract
import re

# Tesseract 설치 경로
pytesseract.pytesseract.tesseract_cmd = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

# 이미지 경로
image_path = r"C:\ncsGlobal\FinalProject\augold\receipt_file\expense_20250714_163339.png"

# OCR 함수
def ocr_image_to_text(path):
    img = Image.open(path)
    text = pytesseract.image_to_string(img, lang='kor+eng')
    return text

def parse_receipt(text):
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    result = {
        "store": None,
        "items": [],
        "total": None,
        "vat": None,
        "supply_price": None,
        "card_number": None,
        "transaction_time": None,
        "approval_number": None
    }

    for i, line in enumerate(lines):
        # 가맹점 (띄어쓰기 무시하고 찾기)
        m_store = re.search(r"가\s*맹\s*점\s*:\s*(.+)", line)
        if m_store:
            # 띄어쓰기 모두 제거해서 가맹점명 저장
            store = m_store.group(1).replace(" ", "")
            result["store"] = store
            continue

        # 카드번호
        m_card = re.search(r"카\s*드\s*번\s*호\s*:\s*(.+)", line)
        if m_card:
            result["card_number"] = m_card.group(1).strip()
            continue

        # 승인번호
        m_approval = re.search(r"승\s*인\s*번\s*호\s*:\s*(\d+)", line)
        if m_approval:
            result["approval_number"] = m_approval.group(1)
            continue

        # 거래일시
        m_time = re.search(r"거\s*래\s*일\s*시\s*:\s*([\d/: ]+)", line)
        if m_time:
            result["transaction_time"] = m_time.group(1).strip()
            continue

        # 금액 관련 (총액, 부가세, 합계)
        m_supply = re.search(r"금\s*액\s*:\s*([\d,]+)원", line)
        if m_supply:
            result["supply_price"] = m_supply.group(1).replace(",", "")
            continue

        m_vat = re.search(r"부\s*가\s*세\s*:\s*([\d,]+)원", line)
        if m_vat:
            result["vat"] = m_vat.group(1).replace(",", "")
            continue

        m_total = re.search(r"합\s*계\s*:\s*([\d,]+)원", line)
        if m_total:
            result["total"] = m_total.group(1).replace(",", "")
            continue

        # 상품명, 단가, 수량, 금액 추출
        # 숫자+원 + 수량 + 숫자+원 으로 된 라인 찾기
        if re.search(r"\d{1,3}(,\d{3})*\s*원\s+\d+\s+\d{1,3}(,\d{3})*\s*원", line):
            # 단가, 수량, 금액 정규식 추출
            price_match = re.findall(r"(\d{1,3}(?:,\d{3})*)\s*원", line)
            qty_match = re.findall(r"\s(\d+)\s", line)
            # 상품명은 단가 앞부분 텍스트로 분리하고 공백 제거
            name_part = line.split(price_match[0])[0].replace(" ", "") if price_match else line.strip()

            unit_price = price_match[0].replace(",", "") if price_match else ""
            qty = qty_match[0] if qty_match else ""
            amount = price_match[1].replace(",", "") if len(price_match) > 1 else ""

            result["items"].append({
                "name": name_part,
                "unit_price": unit_price,
                "qty": qty,
                "amount": amount
            })

    return result

if __name__ == "__main__":
    text = ocr_image_to_text(image_path)
    print("OCR 결과 원문:\n" + "-"*40)
    print(text)
    print("-"*40)

    parsed = parse_receipt(text)
    print("🧾 파싱 결과:\n" + "-"*40)
    for k, v in parsed.items():
        if k == "items":
            print(f"{k}:")
            for item in v:
                print(f"  - {item}")
        else:
            print(f"{k}: {v}")
