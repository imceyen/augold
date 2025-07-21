from PIL import Image, ImageDraw, ImageFont
import os
from datetime import datetime, timedelta
import random

# 저장 폴더
output_dir = r"C:\ncsGlobal\FinalProject\augold\receipt_file"
os.makedirs(output_dir, exist_ok=True)

# 폰트 경로
font_path = "C:/Windows/Fonts/malgun.ttf"
font_path_bold = "C:/Windows/Fonts/malgunbd.ttf"

# 점선 함수
def draw_dotted_line(draw, y, width):
    for x in range(20, width - 20, 15):
        draw.line([(x, y), (x + 5, y)], fill=(0, 0, 0), width=1)

# 항목 정의
categories = {
    "서울식당": ("고등어정식", "복리후생비"),
    "김밥천국": ("김밥", "복리후생비"),
    "스타벅스": ("아메리카노", "접대비"),
    "이마트": ("복사용지", "소모품비"),
    "쿠팡": ("마우스", "비품비"),
    "배달의민족": ("야식", "복리후생비"),
    "동네문구": ("볼펜", "소모품비"),
    "롯데마트": ("청소용품", "소모품비")
}

# 항목별 단가 범위
realistic_prices = {
    "고등어정식": (9000, 13000),
    "김밥": (5000, 7000),
    "아메리카노": (4000, 6000),
    "복사용지": (3000, 5000),
    "마우스": (10000, 20000),
    "야식": (7000, 12000),
    "볼펜": (1000, 3000),
    "청소용품": (5000, 8000)
}

card_numbers = [
    "1234-56**-****-7890",
    "4321-11**-****-0009",
    "9876-00**-****-1234",
    "5555-12**-****-3456",
    "1111-99**-****-6789"
]

# 랜덤 값 생성기
def generate_approval_number():
    return ''.join(str(random.randint(0, 9)) for _ in range(8))

def random_july_datetime():
    date = datetime(2025, 7, random.randint(1, 31))
    hour = random.randint(9, 20)  # 오전 9시 ~ 오후 8시
    minute = random.randint(0, 59)
    second = random.randint(0, 59)
    return datetime(date.year, date.month, date.day, hour, minute, second)

def random_terminal_number():
    return f"{random.randint(1000, 9999)}-{random.randint(1000, 9999)}-{random.randint(1000, 9999)}"

def random_representative():
    return random.choice(["홍길동", "김철수", "이영희", "박민수", "임현우", "정가은"])

def random_address():
    return random.choice([
        "서울시 강남구 역삼로 22",
        "서울시 강서구 공항대로 78",
        "서울시 성동구 성수동 112",
        "서울시 종로구 인사동길 55",
        "서울시 마포구 와우산로 48",
        "서울시 동작구 흑석로 19"
    ])

def random_business_number():
    return f"{random.randint(100, 999)}-{random.randint(10, 99)}-{random.randint(10000, 99999)}"

def random_tel():
    return f"02-{random.randint(200, 999)}-{random.randint(1000,9999)}"

# 영수증 생성
def draw_expense_receipt(store, item, unit_price, qty, expense_type, date_time, card_number, approval_num):
    terminal = random_terminal_number()
    representative = random_representative()
    address = random_address()
    business = random_business_number()
    tel = random_tel()

    img_w, img_h = 500, 850
    bg_color = (255, 255, 255)
    text_color = (0, 0, 0)

    img = Image.new("RGB", (img_w, img_h), bg_color)
    draw = ImageDraw.Draw(img)

    font_bold = ImageFont.truetype(font_path_bold, 22)
    font_regular = ImageFont.truetype(font_path, 20)

    y = 30
    draw.text((30, y), "IC신용승인 (고객용)", font=font_bold, fill=text_color)
    y += 50

    # 상단 매장 정보
    for line in [
        f"단말기 : {terminal}",
        f"가맹점 : {store}",
        f"주소 : {address}",
        f"대표자 : {representative}",
        f"사업자 : {business}   TEL : {tel}"
    ]:
        draw.text((30, y), line, font=font_regular, fill=text_color)
        y += 35

    draw_dotted_line(draw, y, img_w)
    y += 30

    # 상품 정보 헤더
    start_x = [30, 210, 320, 400]
    draw.text((start_x[0], y), "상품명", font=font_bold, fill=text_color)
    draw.text((start_x[1], y), "단가", font=font_bold, fill=text_color)
    draw.text((start_x[2], y), "수량", font=font_bold, fill=text_color)
    draw.text((start_x[3], y), "금액", font=font_bold, fill=text_color)
    y += 50

    total = unit_price * qty
    draw.text((start_x[0], y), item, font=font_regular, fill=text_color)
    draw.text((start_x[1], y), f"{unit_price:,}원", font=font_regular, fill=text_color)
    draw.text((start_x[2], y), str(qty), font=font_regular, fill=text_color)
    draw.text((start_x[3], y), f"{total:,}원", font=font_regular, fill=text_color)
    y += 40

    draw_dotted_line(draw, y, img_w)
    y += 40

    vat = round(total / 11)
    supply_price = total - vat

    for label, value in [("금액", supply_price), ("부가세", vat), ("합계", total)]:
        draw.text((30, y), f"{label} :", font=font_regular, fill=text_color)
        draw.text((150, y), f"{value:,}원", font=font_regular if label != "합계" else font_bold, fill=text_color)
        y += 35

    draw_dotted_line(draw, y, img_w)
    y += 30

    draw.text((30, y), f"카드번호 : {card_number}", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), f"거래일시 : {date_time.strftime('%Y/%m/%d %H:%M:%S')}", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), f"승인번호 : {approval_num}", font=font_regular, fill=text_color)
    y += 35
    draw.text((30, y), "문의 : TEL)1544-0000", font=font_regular, fill=text_color)
    y += 40

    draw_dotted_line(draw, y, img_w)
    y += 40
    draw.text((img_w // 2, y), "* 감사합니다 *", font=font_bold, fill=text_color, anchor="mm")

    filename = f"expense_{date_time.strftime('%Y%m%d_%H%M%S')}_{store}_{expense_type}.png"
    img.save(os.path.join(output_dir, filename), dpi=(300, 300))
    print(f"{filename} 저장 완료")

# ▶ 영수증 30개 생성
for i in range(30):
    store = random.choice(list(categories.keys()))
    item, expense_type = categories[store]
    min_price, max_price = realistic_prices[item]
    price = random.randint(min_price // 100, max_price // 100) * 100
    qty = random.randint(1, 4)
    dt = random_july_datetime()
    card_number = card_numbers[i % len(card_numbers)]
    approval_num = generate_approval_number()

    draw_expense_receipt(store, item, price, qty, expense_type, dt, card_number, approval_num)
