# pip install mysql-connector-python pandas
import pandas as pd
import mysql.connector
from datetime import datetime

# CSV 파일 읽기
df = pd.read_csv("data/금시세.csv")

# MySQL 접속 설정
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'global',
}

# DB에 insert
conn = mysql.connector.connect(**db_config)
cursor = conn.cursor()

# ✅ 중복 시 price_per_gram 값 업데이트
insert_sql = """
INSERT INTO gold_price (price_per_gram, effective_date)
VALUES (%s, %s)
ON DUPLICATE KEY UPDATE
    price_per_gram = VALUES(price_per_gram)
"""

for _, row in df.iterrows():
    effective_date = pd.to_datetime(row['effective_date']).date()
    price_per_gram = float(row['price_per_gram'])
    cursor.execute(insert_sql, (price_per_gram, effective_date))

conn.commit()
cursor.close()
conn.close()

# MySQL에 데이터 insert 완료
print("데이터 삽입 완료!")
