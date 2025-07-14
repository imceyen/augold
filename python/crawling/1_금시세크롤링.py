# pip install cloudscraper
import cloudscraper
import pandas as pd

# generate.cmd 요청 주소
otp_url = 'http://data.krx.co.kr/comm/fileDn/GenerateOTP/generate.cmd'

# 2023~2024 데이터 추출
# form data 생성
otp_form_data = {
    'locale': 'ko_KR',
    "share": '1',
    "csvxls_isNo": 'false',
    "name": 'fileDown',
    "url": 'dbms/MDC/STAT/standard/MDCSTAT15001',
    'strtDd': "20230102",  # 다운로드 받고싶은 날짜
    'endDd': "20241230",
    'adjStkPrc': 2,  # 수정 주가
    'adjStkPrc_check': 'Y',
    'isuCd': "KRD040200002"
}

# scraper 객체 
scraper = cloudscraper.create_scraper()

# form data와 함께 요청 
response = scraper.post(otp_url, params=otp_form_data)

# response의 text 에 담겨있는 otp 코드 가져오기  
otp = response.text

# 결과 확인 
print("result: ", response)
print("otp: ", otp)

#csv 다운로드
csv_url = 'http://data.krx.co.kr/comm/fileDn/download_csv/download.cmd'
csv_form_data = scraper.post(csv_url, params={'code': otp})
csv_form_data.encoding = 'EUC-KR'

# 정상적으로 가져왔는지 확인(됨)
#print(csv_form_data.text)

lst_row = []
for row in csv_form_data.text.split('\n'):
    lst_row.append(row.split(','))

df = pd.DataFrame(lst_row[1:], columns=lst_row[0])

# 데이터 확인
#print(df)

# 2025년 데이터 크롤링
# form data
otp_form_data2 = {
    'locale': 'ko_KR',
    "share": '1',
    "csvxls_isNo": 'false',
    "name": 'fileDown',
    "url": 'dbms/MDC/STAT/standard/MDCSTAT15001',
    'strtDd': "20250102",  # 다운로드 받고싶은 날짜
    'endDd': "20250704",
    'adjStkPrc': 2,  # 수정 주가
    'adjStkPrc_check': 'Y',
    'isuCd': "KRD040200002"
}

response2 = scraper.post(otp_url, params=otp_form_data2)
# response2의 text 에 담겨있는 otp 코드 가져오기
otp2 = response2.text

#csv 다운로드
csv_form_data2 = scraper.post(csv_url, params={'code': otp2})
csv_form_data2.encoding = 'EUC-KR'

# 정상적으로 가져왔는지 확인(됨)
#print(csv_form_data.text)

lst_row = []
for row in csv_form_data2.text.split('\n'):
    lst_row.append(row.split(','))

df2 = pd.DataFrame(lst_row[1:], columns=lst_row[0])

#df.to_csv('data/2023-2024.csv')
#df2.to_csv('data/2025.csv')

df_all = pd.concat([df, df2], ignore_index=True)

# 일자와 종가만 필터링 후 복사
filter_df = df_all[['일자', '종가']].copy()

# 문자열의 " 제거
filter_df['일자'] = filter_df['일자'].str.strip().str.replace('"', '')
filter_df['종가'] = filter_df['종가'].str.strip().str.replace('"', '')

# 2. 날짜 문자열을 datetime으로 변환
filter_df['effective_date'] = pd.to_datetime(filter_df['일자'], format='%Y/%m/%d', errors='coerce')

# 3. 종가를 숫자로 변환
filter_df['price_per_gram'] = pd.to_numeric(filter_df['종가'].str.replace(',', ''), errors='coerce')

# print(filter_df)

# 테이블에 등록할 파일만 추출
result_df = filter_df[['effective_date', 'price_per_gram']]

# 일에 따라 오름차순 정리
result_df = result_df.sort_values('effective_date')

# 출력
# print(result_df)

result_df.to_csv('data/금시세.csv', index=False)