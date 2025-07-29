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

def main(output_path):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(current_dir, "gold_sales_data_final_fixed.csv")

    df = pd.read_csv(csv_path)
    print("원본 데이터 수:", len(df))
    print("Birth_Date null:", df['Birth_Date'].isna().sum())
    print("Gender 값 분포:", df['Gender'].value_counts())
    print("총 매출:", df['Total_Price'].sum())

    df['Birth_Date'] = pd.to_datetime(df['Birth_Date'], errors='coerce')
    df['Age'] = 2025 - df['Birth_Date'].dt.year

    df = df[(df['Age'] >= 10) & (df['Age'] <= 100)]
    df = df[df['Gender'].isin(['남', '여'])]
    df = df[(df['Total_Price'] > 0) & (df['Quantity'] > 0)]

    df['Age_Group'] = df['Age'].apply(get_age_group)

    grouped = df.groupby(['Gender', 'Age_Group', 'Category'])['Total_Price'].sum().reset_index()
    male = grouped[grouped['Gender'] == '남']
    female = grouped[grouped['Gender'] == '여']

    #  Top/Bottom 분석 포함
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

    #  최종 JSON 결과에 포함
    result = {
        "male": male.to_dict(orient='records'),
        "female": female.to_dict(orient='records'),
        "top_bottom": top_bottom_result  # 여기 포함됨
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(f" 통합 분석 결과 저장 완료: {output_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(" 출력 파일 경로가 누락되었습니다.")
        sys.exit(1)

    output_path = sys.argv[1]
    main(output_path)
