import pandas as pd
import json
import os
import sys


def get_age_group(age):
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


def main():
    print("🚀 성별/연령대별 매출 분석 시작")

    # CSV 파일 찾기
    csv_path = find_csv_file()
    if not csv_path:
        print("❌ CSV 파일을 찾을 수 없습니다.")
        print("📂 gold_sales_data_final_final.csv 파일을 현재 디렉토리나 상위 디렉토리에 준비해주세요.")
        return

    try:
        # 데이터 로드 및 처리
        df = pd.read_csv(csv_path)
        print("✅ 원본 데이터 수:", len(df))
        print("📊 Birth_Date null:", df['Birth_Date'].isna().sum())
        print("👥 Gender 값 분포:", df['Gender'].value_counts().to_dict())
        print("💰 총 매출:", f"{df['Total_Price'].sum():,}원")

        # 데이터 전처리
        df['Birth_Date'] = pd.to_datetime(df['Birth_Date'], errors='coerce')
        df['Age'] = 2025 - df['Birth_Date'].dt.year

        # 필터링
        df = df[(df['Age'] >= 10) & (df['Age'] <= 100)]
        df = df[df['Gender'].isin(['남', '여'])]
        df = df[(df['Total_Price'] > 0) & (df['Quantity'] > 0)]

        print(f"✅ 전처리 후 데이터 수: {len(df)}")

        # 연령대 그룹핑
        df['Age_Group'] = df['Age'].apply(get_age_group)

        # 성별/연령대/카테고리별 매출 집계
        grouped = df.groupby(['Gender', 'Age_Group', 'Category'])['Total_Price'].sum().reset_index()
        male = grouped[grouped['Gender'] == '남']
        female = grouped[grouped['Gender'] == '여']

        print(f"📊 남성 그룹 수: {len(male)}")
        print(f"📊 여성 그룹 수: {len(female)}")

        # Top/Bottom 제품 분석
        grouped_items = df.groupby(['Gender', 'Age_Group', 'Product_Name'])['Total_Price'].sum().reset_index()

        top_bottom_result = {
            "top3": [],
            "bottom3": []
        }

        for (gender, age_group), group_df in grouped_items.groupby(['Gender', 'Age_Group']):
            sorted_df = group_df.sort_values(by='Total_Price', ascending=False)

            def to_dict_with_group(row):
                d = row.to_dict()
                d['Gender'] = gender
                d['Age_Group'] = age_group
                return d

            top3 = [to_dict_with_group(row) for _, row in sorted_df.head(3).iterrows()]
            bottom3 = [to_dict_with_group(row) for _, row in sorted_df.tail(3).sort_values(by='Total_Price').iterrows()]

            top_bottom_result['top3'].append({
                "Gender": gender,
                "Age_Group": age_group,
                "Products": top3
            })
            top_bottom_result['bottom3'].append({
                "Gender": gender,
                "Age_Group": age_group,
                "Products": bottom3
            })

        # JSON 결과 생성
        result = {
            "male": male.to_dict(orient='records'),
            "female": female.to_dict(orient='records'),
            "top_bottom": top_bottom_result
        }

        # output 폴더 생성 및 JSON 파일 저장
        output_dir = "output"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            print(f"📁 output 폴더 생성: {output_dir}")

        json_output_path = os.path.join(output_dir, "demographics_dashboard.json")
        with open(json_output_path, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

        print(f"✅ JSON 파일 저장 완료: {json_output_path}")

        # 결과 요약 출력
        total_male_sales = male['Total_Price'].sum()
        total_female_sales = female['Total_Price'].sum()
        print("\n📈 분석 결과 요약:")
        print(f"   남성 총 매출: {total_male_sales:,.0f}원")
        print(f"   여성 총 매출: {total_female_sales:,.0f}원")
        print(f"   총 매출: {total_male_sales + total_female_sales:,.0f}원")
        print(f"   성별 차이: {abs(total_male_sales - total_female_sales):,.0f}원")

        return True

    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("👥 성별/연령대별 매출 분석 도구")
    print("=" * 60)

    success = main()

    if success:
        print("\n🎉 분석이 완료되었습니다!")
        print("📁 생성된 파일: output/demographics_dashboard.json")
        print("🔧 이 파일을 Spring Boot의 static 폴더에 복사하세요.")
    else:
        print("\n❌ 분석에 실패했습니다.")

    print("\n" + "=" * 60)