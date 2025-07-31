import pandas as pd
from prophet import Prophet
import json
import sys
import os
import numpy as np

# --- 기본 경로 및 설정 ---
script_dir = os.path.dirname(os.path.abspath(__file__))
output_dir = os.path.join(script_dir, 'output')
os.makedirs(output_dir, exist_ok=True)
RESAMPLE_RULE_MAP = {'Y': 'YE', 'M': 'ME', 'W': 'W', 'D': 'D'}
FUTURE_PERIODS = {'Y': 2, 'M': 2, 'W': 2, 'D': 5}

def get_output_path(freq):
    filename = f"profit_analysis_{freq.upper()}.json"
    return os.path.join(output_dir, filename)

def main(freq='M', include_predict=True):
    """지정된 주기에 대한 순이익 분석을 실행하고 JSON 파일로 저장합니다."""
    print(f"===== 순이익 분석 시작 (단위: {freq}, 예측: {include_predict}) =====")

    csv_path = os.path.join(script_dir, 'gold_sales_data_final_fixed.csv')
    df_raw = pd.read_csv(csv_path)
    df_raw['Order_Date'] = pd.to_datetime(df_raw['Order_Date'])
    df_raw['Net_Profit'] = df_raw['Total_Price'] - (df_raw['Purchase_Price'] * df_raw['Quantity'])
    df_raw = df_raw.sort_values('Order_Date').set_index('Order_Date')

    freq_upper = freq.upper()
    resample_rule = RESAMPLE_RULE_MAP[freq_upper]

    df = df_raw['Net_Profit'].resample(resample_rule).sum().reset_index()
    df.rename(columns={'Order_Date': 'ds', 'Net_Profit': 'y'}, inplace=True)

    if freq_upper == 'M':
        # 월별 분석 시 추가 비용 처리
        receipt_csv_path = os.path.join(script_dir, '..', 'receipt', 'receipt', '2025-07 영수증 처리 내역.csv')
        if os.path.exists(receipt_csv_path):
            try:
                df_receipt = pd.read_csv(receipt_csv_path, skiprows=2, encoding='utf-8-sig')
                total_row = df_receipt[df_receipt['행 레이블'] == '총합계']
                if not total_row.empty:
                    additional_cost = pd.to_numeric(total_row['합계 : 총금액'].iloc[0])
                    target_month_date = pd.to_datetime('2025-07-31')
                    target_index = df.index[df['ds'] == target_month_date].tolist()
                    if target_index:
                        df.loc[target_index[0], 'y'] -= additional_cost
                    else:
                        new_row = pd.DataFrame([{'ds': target_month_date, 'y': -additional_cost}])
                        df = pd.concat([df, new_row], ignore_index=True).sort_values(by='ds').reset_index(drop=True)
                    print(f"    - 2025년 7월 추가 비용 ({additional_cost:,.0f}원) 반영됨")
            except Exception as e:
                print(f"    - [경고] 추가 비용 파일 처리 중 오류 발생: {e}")

    # Prophet 모델 생성
    if freq_upper == 'Y': model = Prophet(yearly_seasonality=False, weekly_seasonality=False)
    elif freq_upper == 'D': model = Prophet(yearly_seasonality=True, weekly_seasonality=True, daily_seasonality=True, changepoint_prior_scale=0.05)
    else: model = Prophet(yearly_seasonality=True, weekly_seasonality=True)
    model.fit(df)

    # 미래 예측
    if include_predict:
        future_periods = FUTURE_PERIODS[freq_upper]
        future = model.make_future_dataframe(periods=future_periods, freq=resample_rule)
    else:
        future = df[['ds']].copy()

    forecast = model.predict(future)

    # 데이터 결합 및 최종 정리
    full_data = pd.merge(forecast, df, on='ds', how='left')
    full_data = full_data[['ds', 'y', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']]
    full_data['ds'] = full_data['ds'].dt.strftime('%Y-%m-%d')
    full_data = full_data.where(pd.notnull(full_data), None).replace({np.nan: None})

    # JSON 파일로 저장
    output_path = get_output_path(freq)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(full_data.to_dict(orient='records'), f, ensure_ascii=False, indent=4)
    print(f"-> 순이익 분석 완료: {output_path}")

if __name__ == '__main__':
    # Java에서 넘겨주는 인자를 받아서 처리하도록 수정
    freq_arg = sys.argv[1] if len(sys.argv) > 1 else 'M'
    predict_arg = sys.argv[2].lower() == 'true' if len(sys.argv) > 2 else False
    main(freq_arg, predict_arg)