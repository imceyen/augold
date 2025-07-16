import pandas as pd
import mysql.connector
from prophet import Prophet
import json
import sys

def main(output_file_path):
    # DB 접속 및 데이터 로드
    db_config = { 'host': 'localhost', 'user': 'root', 'password': '1234', 'database': 'augold' }
    try:
        conn = mysql.connector.connect(**db_config)
        df = pd.read_sql_query("SELECT * FROM gold_price", conn)
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

    # Prophet 모델링
    df_prophet = df[['effective_date', 'price_per_gram']].copy()
    df_prophet.rename(columns={'effective_date': 'ds', 'price_per_gram': 'y'}, inplace=True)
    df_prophet['ds'] = pd.to_datetime(df_prophet['ds'])

    model = Prophet(weekly_seasonality=True, yearly_seasonality=False)
    model.fit(df_prophet)

    future = model.make_future_dataframe(periods=30)
    forecast = model.predict(future)

    # --- JSON으로 출력할 데이터 가공 ---

    # 1. 메인 예측 그래프 데이터
    forecast_data = forecast[['ds', 'yhat', 'yhat_lower', 'yhat_upper']].copy()
    full_data = pd.merge(forecast_data, df_prophet, on='ds', how='left')

    # 날짜를 문자열로 먼저 변환
    full_data['ds'] = full_data['ds'].dt.strftime('%Y-%m-%d')

    # <<< 여기가 가장 중요합니다 >>>
    # 데이터프레임의 모든 NaN 값을 None(JSON의 null)으로 변환합니다.
    # 이 한 줄이 모든 NaN 문제를 해결합니다.
    full_data = full_data.where(pd.notnull(full_data), None)

    # 2. 트렌드 데이터 추출
    trend_data = forecast[['ds', 'trend']].copy()
    trend_data.rename(columns={'trend': 'y'}, inplace=True)
    trend_data['ds'] = trend_data['ds'].dt.strftime('%Y-%m-%d')

    # 3. 주간 계절성 데이터 추출
    forecast['ds'] = pd.to_datetime(forecast['ds'])
    forecast['weekday'] = forecast['ds'].dt.day_name()
    weekly_avg = forecast.groupby('weekday')['weekly'].mean()
    ordered_days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    weekly_avg = weekly_avg.reindex(ordered_days)

    # 최종 JSON 구조화
    output_json = {
        "forecast": full_data.to_dict(orient='list'),
        "trend": trend_data.to_dict(orient='list'),
        "weekly": {
            "x": weekly_avg.index.tolist(),
            "y": weekly_avg.values.tolist()
        }
    }

    # 파일에 저장
    with open(output_file_path, 'w', encoding='utf-8') as f:
        json.dump(output_json, f, ensure_ascii=False, indent=4)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        output_path = sys.argv[1]
        main(output_path)
    else:
        print("Error: Output file path not provided.")