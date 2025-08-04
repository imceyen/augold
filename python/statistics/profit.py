import pandas as pd
import numpy as np
import json
import os
import sys
from sklearn.linear_model import LinearRegression


def find_csv_file():
    """CSV 파일을 찾는 함수"""
    filename = "gold_sales_data_final_final.csv"

    search_paths = [
        ".",  # 현재 디렉토리
        "..",  # 상위 디렉토리
        "../..",  # 상위의 상위 디렉토리
    ]

    for path in search_paths:
        full_path = os.path.join(path, filename)
        if os.path.exists(full_path):
            print(f"✅ CSV 파일 발견: {full_path}")
            return full_path

    return None


def preprocess_data(df):
    """데이터 전처리 함수"""
    print(f"📊 원본 데이터: {len(df)}개 레코드")

    # 컬럼명 정규화
    if 'Order_Date' in df.columns:
        df = df.rename(columns={'Order_Date': 'Date'})

    # 필수 컬럼 확인
    required_columns = ['Date', 'Total_Price', 'Purchase_Price', 'Quantity', 'Category']
    missing_columns = [col for col in required_columns if col not in df.columns]

    if missing_columns:
        print(f"❌ 필수 컬럼이 없습니다: {missing_columns}")
        print(f"📋 사용 가능한 컬럼: {list(df.columns)}")
        return None

    # 결측값 제거
    df = df.dropna(subset=['Date', 'Total_Price', 'Purchase_Price', 'Quantity'])
    print(f"📊 결측값 제거 후: {len(df)}개 레코드")

    # 날짜 변환
    df['Date'] = pd.to_datetime(df['Date'], errors='coerce')
    df = df.dropna(subset=['Date'])
    print(f"📊 날짜 변환 후: {len(df)}개 레코드")

    # 양수값만 유지
    df = df[(df['Total_Price'] > 0) & (df['Purchase_Price'] > 0) & (df['Quantity'] > 0)]
    print(f"📊 양수값 필터링 후: {len(df)}개 레코드")

    return df


def main():
    print("🚀 카테고리별 이익률 분석 시작")

    # CSV 파일 찾기
    csv_path = find_csv_file()
    if not csv_path:
        print("❌ CSV 파일을 찾을 수 없습니다.")
        print("📂 gold_sales_data_final_final.csv 파일을 현재 디렉토리나 상위 디렉토리에 준비해주세요.")
        return False

    try:
        # ✅ 1. 데이터 로드
        df = pd.read_csv(csv_path)
        print(f"✅ 데이터 로드 완료: {len(df)}개 레코드")

        # ✅ 2. 전처리
        df = preprocess_data(df)
        if df is None:
            return False

        # ✅ 3. 분석 기간 설정 (최근 6개월 또는 2025년 1월 ~ 6월)
        df['Month'] = df['Date'].dt.to_period('M').dt.to_timestamp()

        # 날짜 범위 확인
        date_range = df['Date'].agg(['min', 'max'])
        print(f"📅 데이터 기간: {date_range['min']} ~ {date_range['max']}")

        # 2025년 데이터가 있으면 2025년 1-6월, 없으면 최근 6개월
        if df['Date'].dt.year.max() >= 2025:
            df = df[(df['Month'] >= '2025-01-01') & (df['Month'] <= '2025-06-30')]
            print(f"📅 2025년 1-6월 데이터 선택: {len(df)}개 레코드")
        else:
            # 최근 6개월 데이터 선택
            latest_months = df['Month'].sort_values().tail(6).unique()
            df = df[df['Month'].isin(latest_months)]
            print(f"📅 최근 6개월 데이터 선택: {len(df)}개 레코드")

        if len(df) == 0:
            print("❌ 분석할 데이터가 없습니다.")
            return False

        # ✅ 4. 이익률 및 순이익 계산
        df['Profit_Margin'] = (df['Total_Price'] - (df['Purchase_Price'] * df['Quantity'])) / (
                    df['Purchase_Price'] * df['Quantity'])

        # 이상치 제거 (이익률이 -500% ~ +500% 범위를 벗어나는 데이터)
        df = df[(df['Profit_Margin'] > -5) & (df['Profit_Margin'] < 5)]
        df['Net_Profit'] = df['Total_Price'] - (df['Purchase_Price'] * df['Quantity'])

        print(f"📊 이익률 계산 완료: {len(df)}개 레코드")
        print(f"📊 평균 이익률: {df['Profit_Margin'].mean() * 100:.1f}%")

        # ✅ 5. 카테고리별 총 판매금액
        category_sales = df.groupby('Category')['Total_Price'].sum().reset_index()
        category_sales = category_sales.rename(columns={'Total_Price': 'total_price'})
        print(f"📊 카테고리별 판매금액 계산 완료: {len(category_sales)}개 카테고리")

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
        print("🔮 예측 모델 생성 중...")
        forecast_margin = []
        forecast_netprofit = []

        for cat in ts['Category'].unique():
            sub = ts[ts['Category'] == cat]
            if len(sub) < 2:  # 최소 2개 데이터 포인트 필요
                print(f"⚠️  {cat} 카테고리는 데이터가 부족하여 예측을 건너뜁니다.")
                continue

            X = sub[['Month_Num']]
            y_margin = sub['Profit_Margin']
            y_netprofit = sub['Net_Profit']

            try:
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
            except Exception as e:
                print(f"⚠️  {cat} 카테고리 예측 실패: {e}")
                continue

        # ✅ 9. JSON 결과 구성
        result = {
            "category_sales": category_sales.to_dict(orient='records'),
            "category_margin": category_margin.to_dict(orient='records'),
            "category_timeseries": ts[['Month', 'Category', 'Profit_Margin']].to_dict(orient='records'),
            "category_forecast": forecast_margin,
            "category_netprofit_timeseries": ts[['Month', 'Category', 'Net_Profit']].round(0).astype(
                {'Net_Profit': int}).to_dict(orient='records'),
            "category_netprofit_forecast": forecast_netprofit
        }

        # ✅ 10. output 폴더 생성 및 JSON 파일 저장
        output_dir = "output"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            print(f"📁 output 폴더 생성: {output_dir}")

        json_output_path = os.path.join(output_dir, "category_profit_dashboard.json")
        with open(json_output_path, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

        print(f"✅ JSON 파일 저장 완료: {json_output_path}")

        # 결과 요약 출력
        total_sales = category_sales['total_price'].sum()
        avg_margin = category_margin['avg_profit_margin'].mean()
        forecast_total = sum(item['Predicted_Net_Profit'] for item in forecast_netprofit)

        print(f"\n📈 분석 결과 요약:")
        print(f"   총 매출액: {total_sales:,.0f}원")
        print(f"   평균 이익률: {avg_margin * 100:.1f}%")
        print(f"   분석 카테고리 수: {len(category_sales)}")
        if forecast_netprofit:
            print(f"   7월 예측 순이익: {forecast_total:,.0f}원")

        return True

    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == '__main__':
    print("=" * 60)
    print("💰 카테고리별 이익률 분석 도구")
    print("=" * 60)

    success = main()

    if success:
        print("\n🎉 분석이 완료되었습니다!")
        print("📁 생성된 파일: output/category_profit_dashboard.json")
        print("🔧 이 파일을 Spring Boot의 static 폴더에 복사하세요.")
    else:
        print("\n❌ 분석에 실패했습니다.")

    print("\n" + "=" * 60)