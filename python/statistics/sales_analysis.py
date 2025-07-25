import pandas as pd
from prophet import Prophet
import json
import sys
import os
import numpy as np

# --- output 디렉토리 설정 ---
script_dir = os.path.dirname(os.path.abspath(__file__))
output_dir = os.path.join(script_dir, 'output')
os.makedirs(output_dir, exist_ok=True)

def get_output_path(freq):
    filename = f"sales_analysis_{freq.upper()}.json"
    return os.path.join(output_dir, filename)

def main(freq='M', include_predict=True):
    csv_path = os.path.join(script_dir, 'gold_sales_data_final_fixed.csv')

    df_raw = pd.read_csv(csv_path)
    df_raw['Order_Date'] = pd.to_datetime(df_raw['Order_Date'])
    df_raw = df_raw.sort_values('Order_Date')
    df_raw.set_index('Order_Date', inplace=True)

    resample_rule = {
        'Y': 'Y',
        'M': 'M',
        'W': 'W',
        'D': 'D'
    }.get(freq.upper(), 'M')

    df = df_raw['Total_Price'].resample(resample_rule).sum().reset_index()
    df.rename(columns={'Order_Date': 'ds', 'Total_Price': 'y'}, inplace=True)

    # 로그 변환
    df['y'] = np.log1p(df['y'])

    # Prophet 모델 생성
    freq_upper = freq.upper()
    if freq_upper == 'Y':
        model = Prophet(yearly_seasonality=False, weekly_seasonality=False)
    elif freq_upper == 'D':
        model = Prophet(
            yearly_seasonality=True,
            weekly_seasonality=True,
            daily_seasonality=True,
            changepoint_prior_scale=0.05
        )
    else:
        model = Prophet(yearly_seasonality=True, weekly_seasonality=True)

    model.fit(df)

    periods = {'Y': 3, 'M': 12, 'W': 4, 'D': 14}
    future_periods = periods.get(freq_upper, 6)

    if include_predict:
        future = model.make_future_dataframe(periods=future_periods, freq=resample_rule)
    else:
        future = df[['ds']].copy()

    forecast = model.predict(future)

    # 역변환
    forecast['yhat'] = np.expm1(forecast['yhat'])
    forecast['yhat_lower'] = np.expm1(forecast['yhat_lower'])
    forecast['yhat_upper'] = np.expm1(forecast['yhat_upper'])

    df['y'] = np.expm1(df['y'])

    full_data = pd.merge(forecast, df, on='ds', how='left')
    full_data = full_data[['ds', 'y', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']]
    full_data['ds'] = full_data['ds'].dt.strftime('%Y-%m-%d')

    # NaN 처리
    full_data = full_data.where(pd.notnull(full_data), None)
    full_data = full_data.replace({np.nan: None})

    output_path = get_output_path(freq)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(full_data.to_dict(orient='records'), f, ensure_ascii=False, indent=4)

    print(f"[OK] {freq_upper} 데이터 저장 완료: {output_path}")

if __name__ == '__main__':
    freq = sys.argv[1] if len(sys.argv) > 1 else 'M'
    predict = sys.argv[2].lower() == 'true' if len(sys.argv) > 2 else True
    main(freq, predict)
