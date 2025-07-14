import pandas as pd
import mysql.connector
from matplotlib import pyplot as plt
from prophet import Prophet


# MySQL 접속 설정
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'global',
}

# DB에 접속 및 쿼리 실행
conn = mysql.connector.connect(**db_config)
cursor = conn.cursor()

sql = "SELECT * FROM gold_price"
cursor.execute(sql)

# 데이터 가져오기
table_data = cursor.fetchall()

# 컬럼 이름 가져오기
column_names = [desc[0] for desc in cursor.description]

# Pandas DataFrame으로 변환
df = pd.DataFrame(table_data, columns=column_names)

cursor.close()
conn.close()

# 결과 출력
print(df)

# 한글처리
plt.rcParams['font.family'] ='Malgun Gothic'
plt.rcParams['axes.unicode_minus'] =False

# Prophet이 요구하는 컬럼명으로 변경 + 필요한 열만 선택
df_prophet = df[['effective_date', 'price_per_gram']].copy()
df_prophet.rename(columns={
    'effective_date': 'ds',
    'price_per_gram': 'y'
}, inplace=True)

# Prophet 모델 생성 및 학습
model = Prophet()
model.fit(df_prophet)

# 미래 30일치 날짜 생성 및 예측
future = model.make_future_dataframe(periods=30)
forecast = model.predict(future)

# 예측 시각화
model.plot(forecast)
plt.title("향후 금 시세 예측")
plt.xlabel("날짜")
plt.ylabel("원/g")
plt.grid(True)
plt.tight_layout()
plt.show()

# 트렌드 및 구성요소 시각화
model.plot_components(forecast)
plt.tight_layout()
plt.show()
