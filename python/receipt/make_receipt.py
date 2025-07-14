from PIL import Image, ImageDraw, ImageFont
import os
from datetime import datetime

output_dir = r"C:\ncsGlobal\FinalProject\augold\receipt_file"
os.makedirs(output_dir, exist_ok=True)

font_path = "C:/Windows/Fonts/malgun.ttf"
font_path_bold = "C:/Windows/Fonts/malgunbd.ttf"

def draw_dotted_line(draw, y, width):
    # 점선 간격을 15픽셀로 늘림, 점 길이 5 유지
    for x in range(20, width - 20, 15):
        draw.line([(x, y), (x + 5, y)], fill=(0, 0, 0), width=1)

def draw_expense_receipt():
    img_w, img_h = 500, 850  # 높이 넉넉히 늘림
    bg_color = (255, 255, 255)
    text_color = (0, 0, 0)

    img = Image.new("RGB", (img_w, img_h), bg_color)
    draw = ImageDraw.Draw(img)

    font_bold = ImageFont.truetype(font_path_bold, 22)  # 폰트 크기 약간 키움
    font_regular = ImageFont.truetype(font_path, 20)

    y = 30
    draw.text((30, y), "IC신용승인 (고객용)", font=font_bold, fill=text_color)
    y += 50  # 줄 간격 넉넉히

    # 매장 정보
    for text in [
        "단말기 : 9876-5432-1098",
        "가맹점 : 서울식당",
        "주소 : 서울시 종로구 인사동 12-3",
        "대표자 : 김철수",
        "사업자 : 111-22-33333   TEL : 02-123-4567"
    ]:
        draw.text((30, y), text, font=font_regular, fill=text_color)
        y += 35  # 줄 간격 증가

    draw_dotted_line(draw, y, img_w)
    y += 30

    start_x = [30, 210, 320, 400]
    draw.text((start_x[0], y), "상품명", font=font_bold, fill=text_color)
    draw.text((start_x[1], y), "단가", font=font_bold, fill=text_color)
    draw.text((start_x[2], y), "수량", font=font_bold, fill=text_color)
    draw.text((start_x[3], y), "금액", font=font_bold, fill=text_color)
    y += 50

    items = [("고등어 정식", 15000, 4)]
    total = 0
    for name, unit_price, qty in items:
        amount = unit_price * qty
        total += amount
        draw.text((start_x[0], y), name, font=font_regular, fill=text_color)
        draw.text((start_x[1], y), f"{unit_price:,}원", font=font_regular, fill=text_color)
        draw.text((start_x[2], y), str(qty), font=font_regular, fill=text_color)
        draw.text((start_x[3], y), f"{amount:,}원", font=font_regular, fill=text_color)
        y += 40

    draw_dotted_line(draw, y, img_w)
    y += 40

    vat = round(total / 11)
    supply_price = total - vat

    draw.text((30, y), "금액 :", font=font_regular, fill=text_color)
    draw.text((150, y), f"{supply_price:,}원", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), "부가세 :", font=font_regular, fill=text_color)
    draw.text((150, y), f"{vat:,}원", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), "합계 :", font=font_bold, fill=text_color)
    draw.text((150, y), f"{total:,}원", font=font_bold, fill=text_color)
    y += 40

    draw_dotted_line(draw, y, img_w)
    y += 30

    now = datetime.now()

    draw.text((30, y), "카드번호 : 1234-56**-****-7890", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), f"거래일시 : {now.strftime('%Y/%m/%d %H:%M:%S')}", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), "승인번호 : 98765432", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), "문의 : TEL)1544-0000", font=font_regular, fill=text_color)
    y += 40

    draw_dotted_line(draw, y, img_w)
    y += 40

    draw.text((img_w//2, y), "* 감사합니다 *", font=font_bold, fill=text_color, anchor="mm")

    filename = f"expense_{now.strftime('%Y%m%d_%H%M%S')}.png"
    img.save(os.path.join(output_dir, filename), dpi=(300, 300))  # DPI 추가 저장
    print(f"{filename} 저장 완료")

draw_expense_receipt()
