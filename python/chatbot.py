import google.generativeai as genai
import streamlit as st

st.title("Augold-Bot")
python -m pip --version
@st.cache_resource
def load_model():
    model = genai.GenerativeModel('gemini-2.0-flash')
    print("model loaded...")
    return model

model = load_model()

# 시스템 메시지를 history로 전달 (system_instruction 대체)
if "chat_session" not in st.session_state:
    st.session_state["chat_session"] = model.start_chat(
        history=[
            {
                "role": "user",
                "parts": ["안녕하세요. Augold 금 거래 사이트 챗봇 부분입니다. 채팅을 입력하여 챗봇과 대화해보세요!"]
            }
        ]
    )

# 이전 대화 렌더링
for content in st.session_state.chat_session.history:
    with st.chat_message("ai" if content.role == "model" else "user"):
        st.markdown(content.parts[0].text)

# 사용자 입력 처리
if prompt := st.chat_input("메시지를 입력하세요."):
    with st.chat_message("user"):
        st.markdown(prompt)
    with st.chat_message("ai"):
        response = st.session_state.chat_session.send_message(prompt)
        st.markdown(response.text)
