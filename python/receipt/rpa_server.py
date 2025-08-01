from flask import Flask
import subprocess

app = Flask(__name__)

@app.route('/run-rpa')
def run_rpa():
    try:
        # Brity RPA 실행 파일 경로
        brity_path = r"C:\Users\4Class_13\AppData\Roaming\Brity RPA Designer\BrityRPA_Designer.exe"

        # 프로젝트 파일 경로
        proj_path = r"C:\ncsGlobal\FinalProject\augold\RPA\파이널프로젝트\파이널프로젝트.proj"

        # RPA 실행
        subprocess.Popen([brity_path, proj_path], shell=True)

        return "RPA 실행 성공"
    except Exception as e:
        return f"실패: {str(e)}", 500

if __name__ == "__main__":
    app.run(port=5001, host="0.0.0.0")
