from flask import Flask, jsonify
import pandas as pd
from datetime import datetime
import os
import traceback

app = Flask(__name__)

@app.route('/')
def index():
    try:
        # 오늘 날짜 (예: 20250711)
        today = datetime.now().strftime('%Y%m%d')
        file_name = f"{today}_금시세.csv"
        base_dir = os.path.dirname(__file__)
        csv_path = os.path.join(base_dir, 'data', file_name)

        if not os.path.exists(csv_path):
            return f"""
                ❌ 오늘 날짜 파일이 없습니다.<br>
                찾은 경로: {csv_path}<br>
                기대한 파일명: {file_name}
            """

        df = pd.read_csv(csv_path, encoding='utf-8')
        latest = df.iloc[-1]
        effective_date = str(latest['effective_date'])
        price_per_gram = float(latest['price_per_gram'])

        return f"""
            ✅ Flask 서버 정상 작동 중!<br><br>
            📅 금 시세 기준일: <b>{effective_date}</b><br>
            💰 1g당 가격: <b>{price_per_gram:,.0f}원</b><br>
            📂 사용된 파일: <code>{file_name}</code>
        """

    except Exception as e:
        traceback.print_exc()
        return f"""
            ❗ 에러 발생: {str(e)}
        """


@app.route('/api/gold-price')
def gold_price():
    try:
        today = datetime.now().strftime('%Y%m%d')
        file_name = f"{today}_금시세.csv"
        base_dir = os.path.dirname(__file__)
        csv_path = os.path.join(base_dir, 'data', file_name)

        if not os.path.exists(csv_path):
            return jsonify({"error": f"파일 없음: {file_name}"}), 404

        df = pd.read_csv(csv_path, encoding='utf-8')
        latest = df.iloc[-1]

        return jsonify({
            "effective_date": str(latest['effective_date']),
            "price_per_gram": float(latest['price_per_gram'])
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


@app.route('/api/goldprice/history')
def gold_price_history():
    try:
        # 오늘 날짜 파일 기준으로 과거 기록도 모아보려면 금시세.csv 사용
        base_dir = os.path.dirname(__file__)
        csv_path = os.path.join(base_dir, 'data', '금시세.csv')

        if not os.path.exists(csv_path):
            return jsonify({"error": "금시세.csv 파일이 존재하지 않습니다."}), 404

        df = pd.read_csv(csv_path, encoding='utf-8')
        records = df.to_dict(orient='records')
        return jsonify(records)

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("🚀 Flask 서버 실행 중...")
    app.run(debug=True)
