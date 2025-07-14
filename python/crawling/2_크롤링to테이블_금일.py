#pip install mysql-connector-python pandas
import pandas as pd
import mysql.connector
from datetime import datetime

# 오늘에 해당하는 날짜 값 만들기
today_str = datetime.today().strftime('%Y%m%d')

# CSV 파일 읽기
df = pd.read_csv(f"data/{today_str}_금시세.csv")

# MySQL 접속 설정
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'augold',
}

# DB에 insert
conn = mysql.connector.connect(**db_config)
cursor = conn.cursor()

for _, row in df.iterrows():
    effective_date = pd.to_datetime(row['effective_date']).date()
    price_per_gram = float(row['price_per_gram'])

    # 먼저 UPDATE 시도
    update_sql = """
    UPDATE gold_price
    SET price_per_gram = %s,
        created_at = CURRENT_TIMESTAMP
    WHERE effective_date = %s
    """
    cursor.execute(update_sql, (price_per_gram, effective_date))
    print('데이터 갱신 완료!')

    # 업데이트된 행이 없으면 INSERT
    if cursor.rowcount == 0:
        insert_sql = """
        INSERT INTO gold_price (price_per_gram, effective_date)
        VALUES (%s, %s)
        """
        cursor.execute(insert_sql, (price_per_gram, effective_date))
        print('데이터 삽입 완료!')
        
conn.commit()
print('commit 완료')