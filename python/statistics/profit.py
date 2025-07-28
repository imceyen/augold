# profit.py - 카테고리별 판매 및 수익 분석 + 예측 결과 JSON 저장

import pandas as pd
import numpy as np
import json
import os
import sys
from sklearn.linear_model import LinearRegression

def main(output_path):
    # ✅ 1. 데이터 로드
    csv_path = os.path.join(os.path.dirname(__file__), "gold_sales_data_final_fixed.csv")
    df = pd.read_csv(csv_path)

    # ✅ 2. 전처리
    df = df.rename(columns={'Order_Date': 'Date'})
    df = df.dropna(subset=['Date', 'Total_Price', 'Purchase_Price', 'Quantity'])
    df['Date'] = pd.to_datetime(df['Date'], errors='coerce')
    df = df.dropna(subset=['Date'])
    df = df[(df['Total_Price'] > 0) & (df['Purchase_Price'] > 0) & (df['Quantity'] > 0)]

    # ✅ 3. 분석 기간 설정 (2025년 1월 ~ 6월)
    df['Month'] = df['Date'].dt.to_period('M').dt.to_timestamp()
    df = df[(df['Month'] >= '2025-01-01') & (df['Month'] <= '2025-06-30')]

    # ✅ 4. 이익률 및 순이익 계산
    df['Profit_Margin'] = (df['Total_Price'] - (df['Purchase_Price'] * df['Quantity'])) / (df['Purchase_Price'] * df['Quantity'])
    df = df[(df['Profit_Margin'] > -5) & (df['Profit_Margin'] < 5)]
    df['Net_Profit'] = df['Total_Price'] - (df['Purchase_Price'] * df['Quantity'])

    # ✅ 5. 카테고리별 총 판매금액
    category_sales = df.groupby('Category')['Total_Price'].sum().reset_index()
    category_sales = category_sales.rename(columns={'Total_Price': 'total_price'})

    # ✅ 6. 카테고리별 평균 이익률
    category_margin = df.groupby('Category')['Profit_Margin'].mean().reset_index()
    category_margin = category_margin.rename(columns={'Profit_Margin': 'avg_profit_margin'})

    # ✅ 7. 카테고리별 월별 시계열 데이터 생성
    ts = df.groupby(['Month', 'Category']).agg({
        'Profit_Margin': 'mean',
        'Total_Price': 'sum',
        'Net_Profit': 'sum'
    }).reset_index()
    ts['Month'] = ts['Month'].dt.strftime('%Y-%m')
    ts['Month_Num'] = ts['Month'].astype('category').cat.codes + 1

    # ✅ 8. 예측 (이익률 + 순이익)
    forecast_margin = []
    forecast_netprofit = []
    for cat in ts['Category'].unique():
        sub = ts[ts['Category'] == cat]
        X = sub[['Month_Num']]
        y_margin = sub['Profit_Margin']
        y_netprofit = sub['Net_Profit']

        model_margin = LinearRegression().fit(X, y_margin)
        model_netprofit = LinearRegression().fit(X, y_netprofit)

        next_month_num = X['Month_Num'].max() + 1
        pred_margin = model_margin.predict([[next_month_num]])[0]
        pred_netprofit = model_netprofit.predict([[next_month_num]])[0]

        forecast_margin.append({
            "Month": "2025-07",
            "Category": cat,
            "Predicted_Margin": round(pred_margin, 6)
        })

        forecast_netprofit.append({
            "Month": "2025-07",
            "Category": cat,
            "Predicted_Net_Profit": round(pred_netprofit)
        })

    # ✅ 9. JSON 결과 구성
    result = {
        "category_sales": category_sales.to_dict(orient='records'),
        "category_margin": category_margin.to_dict(orient='records'),
        "category_timeseries": ts[['Month', 'Category', 'Profit_Margin']].to_dict(orient='records'),
        "category_forecast": forecast_margin,
        "category_netprofit_timeseries": ts[['Month', 'Category', 'Net_Profit']].round(0).astype({'Net_Profit': int}).to_dict(orient='records'),
        "category_netprofit_forecast": forecast_netprofit
    }

    # ✅ 10. JSON 파일 저장
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f" JSON 저장 완료: {output_path}")

# ✅ 11. 명령줄 인자로 저장 경로 받기
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("❌ 출력 파일 경로가 누락되었습니다.")
        sys.exit(1)
    output_path = sys.argv[1]
    main(output_path)
