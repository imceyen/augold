import pandas as pd
import json
import os
import sys


def analyze_first_purchases(df):
    """
    고객별 첫 구매 상품을 분석하여 Top 3를 반환하는 함수
    """
    # 1. 고객별로 구매 날짜, 주문 ID 순으로 정렬하여 첫 구매를 정확히 식별
    #    'Customer_ID' 컬럼을 사용하여 고객을 그룹화합니다.
    df_sorted = df.sort_values(by=['Customer_ID', 'Order_Date', 'Order_ID'], ascending=[True, True, True])

    # 2. 고객별 첫 구매 행만 추출
    #    drop_duplicates를 사용하여 각 Customer_ID 그룹의 첫 번째 행만 남깁니다.
    first_purchases_df = df_sorted.drop_duplicates(subset=['Customer_ID'], keep='first')

    print(f"[INFO] 총 {len(df['Customer_ID'].unique())}명의 고객 중 첫 구매 데이터 {len(first_purchases_df)}건 분석 완료.")

    # 3. 첫 구매된 상품들의 이름을 집계 (value_counts)
    top_products = first_purchases_df['Product_Name'].value_counts()

    # 4. 상위 3개 상품만 선정
    top_three = top_products.head(3)

    # 5. 결과를 JSON 형식에 맞게 가공
    result_data = []
    for product, count in top_three.items():
        result_data.append({
            "product": product,
            "count": int(count)  # numpy.int64 타입을 표준 int로 변환
        })

    return result_data


def main():
    """
    메인 실행 함수
    """
    print("[START] 첫 구매 Top 3 상품 분석을 시작합니다.")

    script_dir = os.path.dirname(os.path.abspath(__file__))

    # 분석할 원본 데이터 CSV 파일 경로
    csv_path = os.path.join(script_dir, 'gold_sales_data_final_fixed.csv')

    try:
        df = pd.read_csv(csv_path, encoding='utf-8-sig')
        df['Order_Date'] = pd.to_datetime(df['Order_Date'])
        print(f"[INFO] CSV 파일 로드 성공: {csv_path}")
    except FileNotFoundError:
        print(f"[ERROR] CSV 파일을 찾을 수 없습니다: {csv_path}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"[ERROR] CSV 파일 로드 중 오류 발생: {e}", file=sys.stderr)
        sys.exit(1)

    # 분석 함수 호출
    top_three_data = analyze_first_purchases(df)

    # 결과 저장 디렉토리 생성
    output_dir = os.path.join(script_dir, 'output')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # 최종 결과를 JSON 파일로 저장
    output_path = os.path.join(output_dir, "top_three_first_purchases.json")

    try:
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(top_three_data, f, ensure_ascii=False, indent=4)
        print(f"[OK] 분석 결과 저장 완료: {output_path}")
        print("--- 결과 ---")
        print(json.dumps(top_three_data, ensure_ascii=False, indent=4))
        print("------------")

    except Exception as e:
        print(f"[ERROR] JSON 파일 저장 중 오류 발생: {e}", file=sys.stderr)
        sys.exit(1)

    print("[END] 분석 작업이 성공적으로 완료되었습니다.")


if __name__ == '__main__':
    main()