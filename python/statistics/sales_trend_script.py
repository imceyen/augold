import pandas as pd
from prophet import Prophet
import json
import sys
from datetime import datetime
import os

script_dir = os.path.dirname(os.path.abspath(__file__))
csv_path = os.path.join(script_dir, 'gold_sales_data_final_fixed.csv')

def main(output_file_path):
    # --- 1. 데이터 읽어오기 및 전처리 ---
    df_raw = pd.read_csv(csv_path)
    df_raw['Order_Date'] = pd.to_datetime(df_raw['Order_Date'])

    # 중요: Prophet 모델링을 위해 일별 매출 합계 계산
    df = df_raw.groupby('Order_Date')['Total_Price'].sum().reset_index()

    # --- 2. Prophet 모델링 ---
    # Prophet 입력 형식에 맞게 컬럼명 변경
    df.rename(columns={'Order_Date': 'ds', 'Total_Price': 'y'}, inplace=True)

    # seasonality 설정
    model = Prophet(yearly_seasonality=True, weekly_seasonality=True)
    model.fit(df)

    # 90일 미래 예측
    future = model.make_future_dataframe(periods=90)
    forecast = model.predict(future)

    # --- 3. JSON으로 출력할 데이터 가공 ---

    # 3-1. 전체 예측 데이터 (첫 번째 그래프) - 수정된 부분
    # forecast와 원본 df를 'ds'(datetime 객체) 기준으로 바로 합칩니다.
    # 이렇게 하면 날짜를 기준으로 과거 데이터(y)와 예측 데이터(yhat)가 올바르게 결합됩니다.
    full_data = pd.merge(forecast, df, on='ds', how='left')

    # JSON에 포함할 최종 컬럼들을 선택합니다.
    full_data = full_data[['ds', 'y', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']]

    # 날짜를 JSON 호환을 위해 문자열로 변환합니다.
    full_data['ds'] = full_data['ds'].dt.strftime('%Y-%m-%d')

    # JavaScript에서 null로 처리하기 쉽도록 NaN을 None으로 변경합니다.
    full_data = full_data.where(pd.notnull(full_data), None)

    # 3-2. 요일별 Seasonality 데이터 (두 번째 그래프)
    seasonal_components = model.predict_seasonal_components(forecast)
    weekly_df = forecast[['ds']].copy()
    weekly_df['weekly_effect'] = seasonal_components['weekly']
    weekly_df['weekday'] = weekly_df['ds'].dt.weekday  # 0=월, 1=화, ...

    # 요일별 평균 효과 계산
    weekly_agg = weekly_df.groupby('weekday')['weekly_effect'].mean().reset_index()
    day_korean = ['월', '화', '수', '목', '금', '토', '일']
    weekly_agg['day_name'] = weekly_agg['weekday'].map(lambda x: day_korean[x])
    weekly_data = weekly_agg[['day_name', 'weekly_effect']]

    # 3-3. 월별 Seasonality 데이터 (세 번째 그래프)
    yearly_df = forecast[['ds']].copy()
    yearly_df['yearly_effect'] = seasonal_components['yearly']
    yearly_df['month'] = yearly_df['ds'].dt.month

    # 월별 평균 효과 계산
    yearly_agg = yearly_df.groupby('month')['yearly_effect'].mean().reset_index()
    month_kor = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
    yearly_agg['month_name'] = yearly_agg['month'].map(lambda x: month_kor[x - 1])
    # 월 순서대로 정렬
    yearly_agg['month_name'] = pd.Categorical(yearly_agg['month_name'], categories=month_kor, ordered=True)
    yearly_agg.sort_values('month_name', inplace=True)
    yearly_data = yearly_agg[['month_name', 'yearly_effect']]

    # --- 4. 최종 JSON 구조 생성 ---
    # 각 데이터를 딕셔너리 형태로 변환하여 최종 JSON 객체 구성
    output_json = {
        "main_forecast": full_data.to_dict(orient='records'),
        "weekly_seasonality": weekly_data.to_dict(orient='records'),
        "yearly_seasonality": yearly_data.to_dict(orient='records')
    }

    # 결과를 지정된 파일 경로에 JSON으로 저장
    with open(output_file_path, 'w', encoding='utf-8') as f:
        json.dump(output_json, f, ensure_ascii=False, indent=4)

    print(f"성공적으로 예측 데이터를 {output_file_path} 파일에 저장했습니다.")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("오류: 출력 파일 경로를 인자로 제공해야 합니다.")
        print("기본 경로 'prophet_output.json'을 사용합니다.")
        output_path = 'prophet_output.json'
    else:
        output_path = sys.argv[1]

    main(output_path)