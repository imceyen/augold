import pandas as pd
from prophet import Prophet
import json
<<<<<<< Updated upstream
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
=======
from statsmodels.tsa.holtwinters import ExponentialSmoothing
from datetime import timedelta
import os

file_path = r"C:\ncsGlobal\FinalProject\augold\python\data\gold_sales_data.csv"
df = pd.read_csv(file_path, encoding='utf-8-sig')

df['Order_Date'] = pd.to_datetime(df['Order_Date'])
>>>>>>> Stashed changes

def main(freq='M', include_predict=True):
    csv_path = os.path.join(script_dir, 'gold_sales_data_final_fixed.csv')

<<<<<<< Updated upstream
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
=======
def aggregate_and_forecast(df, freq, label):
    # pandas 2.2 이상 호환: M → ME, Y → YE
    freq_map = {'M': 'ME', 'Y': 'YE'}
    freq_fixed = freq_map.get(freq, freq)

    agg_df = df.resample(freq_fixed, on='Order_Date')[['Revenue', 'Cost', 'Profit']].sum()

    try:
        model = ExponentialSmoothing(agg_df['Revenue'], trend='add', seasonal=None, damped_trend=True)
        model_fit = model.fit()
        forecast = model_fit.forecast(1).values[0]
    except Exception as e:
        print(f"Forecasting failed for freq={freq}: {e}")
        forecast = None
<<<<<<< Updated upstream

    next_date = None
    if not agg_df.empty:
        if freq == 'D':
            next_date = agg_df.index[-1] + timedelta(days=1)
        elif freq == 'W':
            next_date = agg_df.index[-1] + timedelta(weeks=1)
        elif freq == 'M':
            next_date = agg_df.index[-1] + pd.offsets.MonthEnd(1)
        elif freq == 'Y':
            next_date = agg_df.index[-1] + pd.offsets.YearEnd(1)

    return {'label': label, 'agg': agg_df, 'forecast': forecast, 'next_date': next_date}


>>>>>>> Stashed changes

    periods = {'Y': 3, 'M': 12, 'W': 4, 'D': 14}
    future_periods = periods.get(freq_upper, 6)

<<<<<<< Updated upstream
    if include_predict:
        future = model.make_future_dataframe(periods=future_periods, freq=resample_rule)
    else:
        future = df[['ds']].copy()

    forecast = model.predict(future)
=======

    next_date = None
    if not agg_df.empty:
        if freq == 'D':
            next_date = agg_df.index[-1] + timedelta(days=1)
        elif freq == 'W':
            next_date = agg_df.index[-1] + timedelta(weeks=1)
        elif freq == 'M':
            next_date = agg_df.index[-1] + pd.offsets.MonthEnd(1)
        elif freq == 'Y':
            next_date = agg_df.index[-1] + pd.offsets.YearEnd(1)

    return {'label': label, 'agg': agg_df, 'forecast': forecast, 'next_date': next_date}



results = {
    'D': aggregate_and_forecast(df, 'D', '일별'),
    'W': aggregate_and_forecast(df, 'W', '주별'),
    'M': aggregate_and_forecast(df, 'M', '월별'),
    'Y': aggregate_and_forecast(df, 'Y', '연별'),
}
>>>>>>> Stashed changes

    # 역변환
    forecast['yhat'] = np.expm1(forecast['yhat'])
    forecast['yhat_lower'] = np.expm1(forecast['yhat_lower'])
    forecast['yhat_upper'] = np.expm1(forecast['yhat_upper'])
=======
def save_json(period='D', filename=None):
    data = results[period]
    df_agg = data['agg']
    forecast = data['forecast']
    next_date = data['next_date']

    records = [
        {
            "date": idx.strftime('%Y-%m-%d'),
            "revenue": round(row['Revenue']),
            "cost": round(row['Cost']),
            "profit": round(row['Profit'])
        }
        for idx, row in df_agg.iterrows()
    ]

    output = {
        "period": period,
        "data": records,
        "forecast": {
            "date": next_date.strftime('%Y-%m-%d') if next_date else None,
            "revenue": round(forecast) if forecast is not None else None
        }
    }
>>>>>>> Stashed changes

    df['y'] = np.expm1(df['y'])

    full_data = pd.merge(forecast, df, on='ds', how='left')
    full_data = full_data[['ds', 'y', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']]
    full_data['ds'] = full_data['ds'].dt.strftime('%Y-%m-%d')

<<<<<<< Updated upstream
<<<<<<< Updated upstream
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
=======
for period in ['D', 'W', 'M', 'Y']:
    save_json(period)
>>>>>>> Stashed changes
=======
for period in ['D', 'W', 'M', 'Y']:
    save_json(period)
>>>>>>> Stashed changes
