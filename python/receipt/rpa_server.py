from flask import Flask
import subprocess
import os

app = Flask(__name__)

@app.route('/run-rpa')
def run_rpa():
    try:
        # 현재 파일과 같은 디렉토리에 있는 run_rpa.bat 파일 경로
        bat_path = os.path.join(os.path.dirname(__file__), 'run_rpa.bat')

        # .bat 파일 실행
        subprocess.Popen([bat_path], shell=True)

        return "RPA 실행 성공"
    except Exception as e:
        return f"실패: {str(e)}", 500

if __name__ == "__main__":
    app.run(port=5001, host="0.0.0.0")
