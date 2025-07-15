import google.generativeai as genai
import streamlit as st
import mysql.connector

st.title("Augold-Bot")

@st.cache_resource
def load_model():
    model = genai.GenerativeModel('gemini-2.0-flash')
    return model

def get_products_from_db():
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="1234",
        database="augold"
    )
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT PRODUCT_NAME, DESCRIPTION
        FROM PRODUCT 
        ORDER BY RAND() 
        LIMIT 3
    """)
    products = cursor.fetchall()
    cursor.close()
    conn.close()
    return products

model = load_model()

if "chat_session" not in st.session_state:
    st.session_state["chat_session"] = model.start_chat(
        history=[{
            "role": "user",
            "parts": ["안녕하세요. Augold 금 거래 사이트 챗봇입니다. 채팅을 입력하여 챗봇과 대화해보세요!"]
        }]
    )

for content in st.session_state.chat_session.history:
    with st.chat_message("ai" if content.role == "model" else "user"):
        st.markdown(content.parts[0].text)

if prompt := st.chat_input("메시지를 입력하세요."):
    with st.chat_message("user"):
        st.markdown(prompt)

    if "추천" in prompt or "상품" in prompt:
        products = get_products_from_db()
        product_recommendations = ""
        for p in products:
            product_recommendations += f"📦 **{p['PRODUCT_NAME']}**\n"
            if p["DESCRIPTION"]:
                product_recommendations += f"{p['DESCRIPTION']}\n"
            product_recommendations += "\n---\n"

        with st.chat_message("ai"):
            st.markdown(f"다음은 추천 상품입니다:\n\n{product_recommendations}")
    else:
        with st.chat_message("ai"):
            response = st.session_state.chat_session.send_message(prompt)
            st.markdown(response.text)
