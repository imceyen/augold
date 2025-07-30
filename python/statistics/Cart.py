import pandas as pd
import json
import sys
import os
import numpy as np
from datetime import datetime, timedelta
import warnings

warnings.filterwarnings('ignore')

# --- 절대 경로로 디렉토리 설정 ---
PROJECT_ROOT = r"C:\ncsGlobal\FinalProject\augold"
STATISTICS_DIR = os.path.join(PROJECT_ROOT, "python", "statistics")
OUTPUT_DIR = os.path.join(STATISTICS_DIR, "output")

# 디렉토리 생성
os.makedirs(OUTPUT_DIR, exist_ok=True)


def get_output_path():
    filename = "cart_abandonment_analysis.json"
    return os.path.join(OUTPUT_DIR, filename)


class CartAnalysisJSON:
    def __init__(self, csv_file_path=None):
        """CSV 파일로 분석기 초기화"""
        # 기본 CSV 파일 경로 설정
        if csv_file_path is None:
            csv_file_path = 'cart_sample_data.csv'

        # STATISTICS_DIR 기준으로 절대 경로 생성
        self.csv_file = os.path.join(STATISTICS_DIR, csv_file_path)
        self.df = None
        self.analysis_base_time = None

    def load_data(self):
        """CSV 파일 로드 및 분석 기준 시점 설정"""
        try:
            self.df = pd.read_csv(self.csv_file, encoding='utf-8-sig')
            self.df['CART_DATE'] = pd.to_datetime(self.df['CART_DATE'])

            # 데이터 내 가장 최근 시점을 분석 기준으로 고정
            self.analysis_base_time = self.df['CART_DATE'].max()

            print(f"데이터 로드 완료: {len(self.df):,}건")
            print(f"분석 기준 시점: {self.analysis_base_time}")

            return True
        except Exception as e:
            print(f"파일 로드 실패: {e}")
            return False

    def analyze_abandonment(self, hours_threshold=1):
        """고정 시점 기준 이탈 분석"""
        if self.df is None:
            return None

        # 고정 시점 기준으로 이탈 판단
        threshold_time = self.analysis_base_time - timedelta(hours=hours_threshold)

        # 이탈/정상 분류
        self.df['IS_ABANDONED'] = self.df['CART_DATE'] < threshold_time
        self.df['HOURS_SINCE_ADDED'] = (self.analysis_base_time - self.df['CART_DATE']).dt.total_seconds() / 3600

        # 통계 계산
        total_items = len(self.df)
        abandoned_items = self.df['IS_ABANDONED'].sum()
        active_items = total_items - abandoned_items
        abandonment_rate = (abandoned_items / total_items) * 100

        return {
            'total': int(total_items),
            'abandoned': int(abandoned_items),
            'active': int(active_items),
            'rate': round(abandonment_rate, 1),
            'base_time': self.analysis_base_time.strftime('%Y-%m-%d %H:%M:%S'),
            'threshold_hours': hours_threshold
        }

    def generate_chart_data(self):
        """차트용 JSON 데이터 생성"""
        if self.df is None:
            return None

        abandoned_df = self.df[self.df['IS_ABANDONED'] == True]

        # 1. 이탈률 도넛차트 데이터
        abandon_counts = self.df['IS_ABANDONED'].value_counts()
        abandonment_chart = {
            'type': 'donut',
            'title': '전체 이탈률',
            'data': {
                'labels': ['이탈', '정상'],
                'values': [int(abandon_counts[True]), int(abandon_counts[False])],
                'colors': ['#FF6B6B', '#4ECDC4']
            }
        }

        # 2. 시간대별 라인차트 데이터
        self.df['HOUR'] = self.df['CART_DATE'].dt.hour
        hourly = self.df.groupby('HOUR')['CART_NUMBER'].count()
        hourly_chart = {
            'type': 'line',
            'title': '시간대별 장바구니 추가',
            'data': {
                'x': hourly.index.tolist(),
                'y': hourly.values.tolist(),
                'peak_hour': int(hourly.idxmax()),
                'peak_value': int(hourly.max())
            }
        }

        # 3. 순도별 바차트 데이터
        karat_abandon = abandoned_df['KARAT_CODE'].value_counts()
        karat_chart = {
            'type': 'bar',
            'title': '순도별 이탈 현황',
            'data': {
                'x': karat_abandon.index.tolist(),
                'y': karat_abandon.values.tolist(),
                'colors': ['#FFD93D', '#C0C0C0', '#CD7F32']
            }
        }

        # 4. 이탈 시간 분포 히스토그램
        hours_data = abandoned_df['HOURS_SINCE_ADDED']
        bins = [1, 6, 24, 72, 168, hours_data.max()]
        labels = ['1-6시간', '6-24시간', '1-3일', '3-7일', '7일이상']

        hist_data = pd.cut(hours_data, bins=bins, labels=labels, include_lowest=True)
        hist_counts = hist_data.value_counts().sort_index()

        total_abandoned = len(abandoned_df)
        time_distribution_chart = {
            'type': 'histogram',
            'title': '이탈 시간 분포',
            'data': {
                'x': labels,
                'y': hist_counts.values.tolist(),
                'percentages': [(count / total_abandoned) * 100 for count in hist_counts.values],
                'colors': ['#FF9999', '#FFB366', '#FFFF66', '#99FF99', '#99CCFF']
            }
        }

        return {
            'abandonment_donut': abandonment_chart,
            'hourly_line': hourly_chart,
            'karat_bar': karat_chart,
            'time_distribution': time_distribution_chart
        }

    def generate_insights(self):
        """비즈니스 인사이트 생성"""
        if self.df is None:
            return None

        abandoned_df = self.df[self.df['IS_ABANDONED'] == True]

        # 기본 통계
        total_abandoned = len(abandoned_df)
        avg_hours = abandoned_df['HOURS_SINCE_ADDED'].mean()

        # 순도별 인사이트
        top_karat = abandoned_df['KARAT_CODE'].value_counts().index[0]
        top_karat_count = abandoned_df['KARAT_CODE'].value_counts().iloc[0]

        # 시간대 인사이트
        peak_hour = self.df.groupby('HOUR')['CART_NUMBER'].count().idxmax()

        # 고객별 분석
        customer_abandon = abandoned_df.groupby('CSTM_NUMBER').size()
        avg_abandon_per_customer = customer_abandon.mean()
        max_abandon_customer = customer_abandon.max()

        return {
            'summary': {
                'total_abandoned': int(total_abandoned),
                'avg_hours': round(avg_hours, 1),
                'top_karat': top_karat,
                'top_karat_count': int(top_karat_count),
                'peak_hour': int(peak_hour),
                'avg_abandon_per_customer': round(avg_abandon_per_customer, 1),
                'max_abandon_customer': int(max_abandon_customer),
                'base_time': self.analysis_base_time.strftime('%Y-%m-%d %H:%M:%S')
            },
            'recommendations': [
                f"{peak_hour - 1}~{peak_hour + 1}시 집중 광고 집행",
                f"{top_karat} 상품 특별 할인 이벤트",
                "1-6시간 이탈 고객에게 즉시 리마인드",
                "6-24시간 이탈 고객에게 쿠폰 발송",
                "고객별 맞춤 추천 시스템 도입"
            ],
            'key_findings': [
                f"총 {total_abandoned:,}개 아이템이 평균 {avg_hours:.1f}시간 후 이탈",
                f"{top_karat} 순도가 {top_karat_count}건으로 가장 많이 이탈",
                f"{peak_hour}시에 장바구니 추가가 가장 활발",
                f"고객당 평균 {avg_abandon_per_customer:.1f}개 아이템 이탈"
            ]
        }

    def multi_threshold_analysis(self):
        """다양한 시간대별 이탈률 분석"""
        thresholds = [0.5, 1, 2, 6, 12, 24, 48, 72]
        results = []

        for hours in thresholds:
            threshold_time = self.analysis_base_time - timedelta(hours=hours)
            abandoned_count = (self.df['CART_DATE'] < threshold_time).sum()
            total_count = len(self.df)
            rate = (abandoned_count / total_count) * 100

            # 시간 표시 형식
            if hours < 1:
                time_label = f"{int(hours * 60)}분"
            elif hours < 24:
                time_label = f"{int(hours)}시간"
            else:
                time_label = f"{int(hours / 24)}일"

            results.append({
                'hours': hours,
                'time_label': time_label,
                'abandoned_count': int(abandoned_count),
                'rate': round(rate, 1)
            })

        return results


def main(hours_threshold=1):
    """메인 실행 함수"""
    # CSV 파일명만 전달 (CartAnalysisJSON에서 절대 경로로 변환)
    analyzer = CartAnalysisJSON('cart_sample_data.csv')

    if not analyzer.load_data():
        return False

    # 기본 분석 실행
    summary = analyzer.analyze_abandonment(hours_threshold)
    if summary is None:
        print("분석 실행 실패")
        return False

    # 차트 데이터 생성
    charts = analyzer.generate_chart_data()
    insights = analyzer.generate_insights()
    threshold_analysis = analyzer.multi_threshold_analysis()

    # 최종 결과 JSON 구성
    result = {
        'metadata': {
            'analysis_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'base_time': summary['base_time'],
            'threshold_hours': hours_threshold,
            'version': '1.0'
        },
        'summary': summary,
        'charts': charts,
        'insights': insights,
        'threshold_analysis': threshold_analysis
    }

    # JSON 파일 저장
    output_path = get_output_path()
    try:
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

        print(f"분석 완료! JSON 저장: {output_path}")
        print(f"총 {summary['total']:,}개 아이템 중 {summary['abandoned']:,}개({summary['rate']:.1f}%) 이탈")
        return True

    except Exception as e:
        print(f"JSON 저장 실패: {e}")
        return False


if __name__ == '__main__':
    # 명령행 인자로 이탈 기준 시간 받기 (기본 1시간)
    hours_threshold = int(sys.argv[1]) if len(sys.argv) > 1 else 1

    print("장바구니 이탈 분석 시작...")
    success = main(hours_threshold)

    if success:
        print("분석 성공!")
    else:
        print("분석 실패!")
        sys.exit(1)