import pandas as pd
import mysql.connector
from matplotlib import pyplot as plt
from prophet import Prophet
import matplotlib.dates as mdates

# 1. MySQL 접속 및 데이터 로딩
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'global',
}
conn = mysql.connector.connect(**db_config)
cursor = conn.cursor()

sql = "SELECT * FROM gold_price"
cursor.execute(sql)
table_data = cursor.fetchall()
column_names = [desc[0] for desc in cursor.description]
df = pd.DataFrame(table_data, columns=column_names)

cursor.close()
conn.close()

# 2. 한글처리 및 시각화 옵션
plt.rcParams['font.family'] ='Malgun Gothic'
plt.rcParams['axes.unicode_minus'] =False

# 3. Prophet 준비
df_prophet = df[['effective_date', 'price_per_gram']].copy()
df_prophet.rename(columns={'effective_date': 'ds', 'price_per_gram': 'y'}, inplace=True)

model = Prophet()
model.fit(df_prophet)

# 4. 예측
future = model.make_future_dataframe(periods=30)
forecast = model.predict(future)

# 5. 예측 그래프 그리기
fig1 = model.plot(forecast, figsize=(12, 6))

# ✅ 그래프 커스터마이징
ax = fig1.gca()
ax.set_title("향후 금 시세 예측", fontsize=16, fontweight='bold')
ax.set_xlabel("날짜", fontsize=12)
ax.set_ylabel("금 시세 (원/g)", fontsize=12)

# 날짜 포맷 깔끔하게
ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
fig1.autofmt_xdate()

# 눈금, 그리드
ax.grid(True, linestyle='--', alpha=0.6)

plt.tight_layout()
plt.show()

# 6. 트렌드 구성요소 시각화
fig2 = model.plot_components(forecast)
fig2.suptitle("금 시세 구성요소 (추세, 주기)", fontsize=14, fontweight='bold')
plt.tight_layout()
plt.show()
