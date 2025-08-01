from flask import Flask
import os

app = Flask(__name__)

@app.route('/run-rpa')
def run_rpa():
    try:
        os.startfile(r"C:\ncsGlobal\FinalProject\augold\RPA\파이널프로젝트\파이널프로젝트.proj")
        return "RPA 파일 열기 성공"
    except Exception as e:
        return f"실패: {str(e)}", 500

if __name__ == "__main__":
    app.run(port=5001, host="0.0.0.0")
