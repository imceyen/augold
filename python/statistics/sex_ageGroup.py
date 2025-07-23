import pandas as pd
import json
import os
import sys

def get_age_group(age):
    """나이를 연령대 그룹으로 변환"""
    if age < 20:
        return '10대'
    elif age < 30:
        return '20대'
    elif age < 40:
        return '30대'
    elif age < 50:
        return '40대'
    elif age < 60:
        return '50대'
    else:
        return '60대 이상'

def main(output_path):
    # 1. 현재 파일 기준 CSV 경로 구성
    current_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(current_dir, "gold_sales_data_final_fixed.csv")

    # 2. 데이터 불러오기
    df = pd.read_csv(csv_path)

    # 3. 데이터 전처리
    df['Birth_Date'] = pd.to_datetime(df['Birth_Date'], errors='coerce')  # 날짜형으로 변환
    df['Age'] = 2025 - df['Birth_Date'].dt.year                           # 나이 계산

    # 유효한 나이, 성별, 매출만 필터링
    df = df[(df['Age'] >= 10) & (df['Age'] <= 100)]
    df = df[df['Gender'].isin(['남', '여'])]
    df = df[(df['Total_Price'] > 0) & (df['Quantity'] > 0)]

    # 4. 연령대 컬럼 추가
    df['Age_Group'] = df['Age'].apply(get_age_group)

    # 5. 성별 × 연령대 × 카테고리별 매출 집계
    grouped = df.groupby(['Gender', 'Age_Group', 'Category'])['Total_Price'].sum().reset_index()

    # 6. 성별로 구분
    male = grouped[grouped['Gender'] == '남']
    female = grouped[grouped['Gender'] == '여']

    # 7. JSON 구조로 변환
    result = {
        "male": male.to_dict(orient='records'),
        "female": female.to_dict(orient='records')
    }

    # 8. 결과를 JSON 파일로 저장
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False)
    print(f"✅ 분석 결과가 저장되었습니다: {output_path}")

    # 9. Spring Boot static 폴더 경로로도 복사
    try:
        spring_static_path = os.path.abspath(
            os.path.join(current_dir, '..', '..', '..', 'src', 'main', 'resources', 'static', 'temp_output.json')
        )
        os.makedirs(os.path.dirname(spring_static_path), exist_ok=True)
        with open(spring_static_path, "w", encoding="utf-8") as sf:
            json.dump(result, sf, ensure_ascii=False)
        print(f"📁 Spring Boot static 폴더에 복사됨: {spring_static_path}")
    except Exception as e:
        print(f"⚠️ static 폴더 복사 실패: {e}")


if __name__ == "__main__":
    # 출력 파일 경로가 없으면 종료
    if len(sys.argv) < 2:
        print("❌ 출력 파일 경로가 누락되었습니다.")
        sys.exit(1)

    output_path = sys.argv[1]
    main(output_path)
