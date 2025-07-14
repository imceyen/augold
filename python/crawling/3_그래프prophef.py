# pip install prophet
import pandas as pd
import matplotlib.pyplot as plt
from prophet import Prophet
from datetime import datetime

# 한글처리
plt.rcParams['font.family'] ='Malgun Gothic'
plt.rcParams['axes.unicode_minus'] =False

filename = f'data/금시세.csv'

# CSV 불러오기
df = pd.read_csv(filename)

# 문자열 공백 제거
for col in df.select_dtypes(include='object').columns:
    df[col] = df[col].str.strip()

# 날짜 타입으로 변환
df['effective_date'] = pd.to_datetime(df['effective_date'])

# Prophet이 요구하는 컬럼명으로 변경
df_prophet = df.rename(columns={
    'effective_date': 'ds',
    'price_per_gram': 'y'
})

# Prophet 모델 생성 및 학습
model = Prophet()
model.fit(df_prophet)

# 미래 30일치 날짜 생성
future = model.make_future_dataframe(periods=30)

# 예측 수행
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
