import pandas as pd
import json
from statsmodels.tsa.holtwinters import ExponentialSmoothing
from datetime import timedelta
import platform
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
import os

# 한글 폰트 설정 (Windows)
if platform.system() == 'Windows':
    font_path = 'C:/Windows/Fonts/malgun.ttf'  # 맑은 고딕
    font_name = fm.FontProperties(fname=font_path).get_name()
    plt.rc('font', family=font_name)

# 데이터 로드
file_path = "/mnt/data/gold_sales_data.csv"
df = pd.read_csv(file_path)
df['Order_Date'] = pd.to_datetime(df['Order_Date'])

# 계산 필드 추가
df['Revenue'] = df['Total_Price']
df['Cost'] = df['Purchase_Price'] * df['Quantity']
df['Profit'] = (df['Sale_Price'] - df['Purchase_Price']) * df['Quantity']

# 리샘플 및 예측 함수
def aggregate_and_forecast(df, freq, label):
    freq_map = {'M': 'ME', 'Y': 'YE'}
    freq_for_resample = freq_map.get(freq, freq)
    agg_df = df.resample(freq_for_resample, on='Order_Date')[['Revenue', 'Cost', 'Profit']].sum()

    try:
        model = ExponentialSmoothing(agg_df['Revenue'], trend='add', seasonal=None, damped_trend=True)
        model_fit = model.fit()
        forecast = model_fit.forecast(1)
        forecast_value = forecast.values[0]
    except Exception:
        forecast_value = None

    if freq == 'D':
        next_date = agg_df.index[-1] + timedelta(days=1)
    elif freq == 'W':
        next_date = agg_df.index[-1] + timedelta(weeks=1)
    elif freq == 'M':
        next_date = agg_df.index[-1] + pd.offsets.MonthEnd()
    elif freq == 'Y':
        next_date = agg_df.index[-1] + pd.offsets.YearEnd()
    else:
        next_date = None

    return {
        'label': label,
        'agg': agg_df,
        'forecast_value': forecast_value,
        'next_date': next_date
    }

# 일/주/월/연 데이터 생성
results = {
    'D': aggregate_and_forecast(df, 'D', '일별'),
    'W': aggregate_and_forecast(df, 'W', '주별'),
    'M': aggregate_and_forecast(df, 'M', '월별'),
    'Y': aggregate_and_forecast(df, 'Y', '연별'),
}

# JSON 저장 함수
def save_json(period='D', filename=None):
    data = results[period]
    df_agg = data['agg']
    forecast = data['forecast_value']
    next_date = data['next_date']

    records = []
    for idx, row in df_agg.iterrows():
        records.append({
            "date": idx.strftime('%Y-%m-%d'),
            "revenue": round(row['Revenue']),
            "cost": round(row['Cost']),
            "profit": round(row['Profit'])
        })

    output = {
        "period": period,
        "data": records,
        "forecast": {
            "date": next_date.strftime('%Y-%m-%d') if next_date else None,
            "revenue": round(forecast) if forecast else None
        }
    }

    if filename is None:
        filename = f'C:/ncsGlobal/FinalProject/augold/python/data/chart_data_{period}.json'

    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

# 모든 주기별 JSON 저장
for p in ['D', 'W', 'M', 'Y']:
    save_json(p)
