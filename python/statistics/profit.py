# profit_script.py - 카테고리별 이익률 분석 및 예측

import pandas as pd
import numpy as np
import sys
import json
from sklearn.linear_model import LinearRegression
import os


# 현재 파일의 위치에서 CSV 파일 절대경로 만들기
current_dir = os.path.dirname(os.path.abspath(__file__))
csv_path = os.path.join(current_dir, "gold_sales_data_final_fixed.csv")

print(">>> CSV PATH:", csv_path)  # 디버깅용

df = pd.read_csv(csv_path)

df = df.rename(columns={'Order_Date': 'Date'})

# 🔹 필수 열이 결측치인 행 제거
df = df.dropna(subset=['Date', 'Sale_Price', 'Purchase_Price'])

# 🔹 날짜 형식으로 변환 후 다시 결측 제거 (파싱 실패한 경우 등)
df['Date'] = pd.to_datetime(df['Date'], errors='coerce')
df = df.dropna(subset=['Date'])

# 🔹 월 단위로 변환
df['Month'] = df['Date'].dt.to_period('M').dt.to_timestamp()

# 🔹 이상치 제거 및 수익률 계산
df = df[(df['Sale_Price'] > 0) & (df['Purchase_Price'] > 0)]
df['Profit_Margin'] = (df['Sale_Price'] - df['Purchase_Price']) / df['Purchase_Price']

# 🔹 수익률이 너무 크거나 작은 이상치 필터링
df = df[(df['Profit_Margin'] > -5) & (df['Profit_Margin'] < 5)]

# 🔹 분석 기간 제한 (예: 2025년 1월 ~ 6월)
df = df[(df['Month'] >= '2025-01-01') & (df['Month'] <= '2025-06-30')]

# 🔹 월별 평균 이익률 계산
monthly_avg = df.groupby('Month')['Profit_Margin'].mean().reset_index()

# 🔹 날짜 포맷을 문자열로 변환 (Plotly 등에서 문자열 필요)
monthly_avg['Month'] = monthly_avg['Month'].dt.strftime("%Y-%m-%d")

# 🔹 회귀분석을 위한 x축 숫자화
monthly_avg['Month_Num'] = np.arange(1, len(monthly_avg) + 1)

# 🔹 선형 회귀 모델 훈련 및 예측
X = monthly_avg['Month_Num'].values.reshape(-1, 1)
y = monthly_avg['Profit_Margin'].values
model = LinearRegression().fit(X, y)
predicted_margin = model.predict(np.array([[7]]))[0]  # 7월 예측값

# 🔹 CSV 경로 출력 (디버깅용)
print(">>> CSV PATH:", csv_path)

# 🔹 최종 결과 JSON 구조 생성
result = {
    "monthly": monthly_avg.to_dict(orient="records"),  # 과거 데이터
    "forecast": {
        "month": "2025-07-01",
        "predicted_margin": predicted_margin           # 예측값
    }
}

# 🔹 출력 파일 경로 받아오기 (Spring에서 전달한 인자)
if len(sys.argv) < 2:
    print("❌ 출력 파일 경로가 누락되었습니다.")
    sys.exit(1)

output_path = sys.argv[1]

# 🔹 JSON 파일로 저장
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False)
