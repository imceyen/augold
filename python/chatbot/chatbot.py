import google.generativeai as genai
import streamlit as st
import mysql.connector

# --- 페이지 기본 설정 (가장 먼저 호출) ---
st.set_page_config(
    page_title="Augold-Bot",
    page_icon="💎",
    layout="centered"
)

st.title("Augold-Bot 🤖")

# --- 모델 로딩 (캐시 사용) ---
@st.cache_resource
def load_model():
    model = genai.GenerativeModel('gemini-2.0-flash')
    return model

# --- DB에서 상품 정보 가져오기 ---
def get_products_from_db(keyword=None):
    try:
        conn = mysql.connector.connect(
            host="localhost", user="root", password="1234", database="augold"
        )
        cursor = conn.cursor(dictionary=True)

        query = "SELECT PRODUCT_NAME, DESCRIPTION FROM PRODUCT"
        params = []

        if keyword:
            query += " WHERE PRODUCT_NAME LIKE %s OR SUB_CTGR LIKE %s"
            params.extend([f"%{keyword}%", f"%{keyword}%"])

        query += " ORDER BY RAND() LIMIT 3"

        cursor.execute(query, tuple(params))
        products = cursor.fetchall()
        cursor.close()
        conn.close()
        return products
    except mysql.connector.Error as err:
        st.error(f"데이터베이스 연결에 실패했습니다. 관리자에게 문의해주세요.")
        return []

model = load_model()

# --- 채팅 세션 초기화 및 페르소나 설정 ---
if "chat_session" not in st.session_state:
    persona_prompt = (
        "너는 'AuGold'라는 고급 금 거래 사이트의 '골드'라는 이름을 가진, 매우 친절하고 상냥한 여성 상담원이야. "
        "항상 고객에게 존댓말을 사용하고, 문장 끝에 상황에 맞는 이모티콘(💎, 😊, 💖 등)을 적절히 사용하여 밝고 긍정적인 분위기를 만들어줘."
    )
    st.session_state["chat_session"] = model.start_chat(history=[
        {"role": "user", "parts": [persona_prompt]},
        {"role": "model", "parts": ["네, 고객님! AuGold의 보석 같은 챗봇, '골드'예요. 무엇을 도와드릴까요? 😊"]}
    ])

# --- 이전 대화 기록 표시 ---
for content in st.session_state.chat_session.history[2:]:
    with st.chat_message("ai" if content.role == "model" else "user"):
        st.markdown(content.parts[0].text)

# --- 사용자 입력 처리 ---
if prompt := st.chat_input("궁금한 점을 물어보세요! ✨"):
    with st.chat_message("user"):
        st.markdown(prompt)

    # 키워드 목록 정의
    personal_info_keywords = ["문의", "내역", "주문", "결제", "배송", "내 정보", "아이디", "비밀번호", "계정"]
    support_keywords = ["오류", "에러", "안돼요", "안되요", "문제", "고장", "버그", "담당자"]
    recommend_keywords = ["추천", "상품", "보여줘"]
    greeting_keywords = ["안녕", "안녕하세요", "하이", "ㅎㅇ", "반가워", "방가"]

    prompt_lower = prompt.lower()

    # 1. 개인 정보 질문 처리
    if any(kw in prompt_lower for kw in personal_info_keywords):
        with st.chat_message("ai"):
            st.markdown(
                "고객님의 소중한 개인정보(주문, 문의 내역 등)는 제가 직접 조회할 수 없어요. 😥\n\n"
                "**[마이페이지]**에서 직접 확인하시거나, **[1:1 문의 게시판]**을 이용해주시면 감사하겠습니다! 💖"
            )

    # 2. 고객 지원/오류 질문 처리
    elif any(kw in prompt_lower for kw in support_keywords):
        with st.chat_message("ai"):
            st.markdown(
                "오류로 인해 이용에 불편을 드려 정말 죄송합니다. 😥\n\n"
                "가장 빠른 해결을 위해, **[1:1 문의 게시판]**에 **오류 화면 캡쳐**나 **자세한 상황 설명**을 남겨주시면 저희 담당자가 신속하게 확인하고 처리해 드릴 거예요! ✨"
            )

    # 3. 상품 추천 질문 처리
    elif any(kw in prompt_lower for kw in recommend_keywords):
        db_keywords = ["돌반지", "카네이션기념품", "목걸이", "반지", "귀걸이", "골드바", "감사패"]
        found_keyword = None
        for kw in db_keywords:
            if kw in prompt:
                found_keyword = kw
                break

        products = get_products_from_db(keyword=found_keyword)
        with st.chat_message("ai"):
            if not products:
                st.markdown(f"'{found_keyword}' 관련 상품은 찾지 못했지만, 다른 멋진 상품들을 소개해 드릴게요! 💖")
                products = get_products_from_db()

            response_text = "네, 고객님! 요청하신 상품들을 찾아봤어요. 마음에 드는 상품이 있는지 확인해보세요! 💎\n\n"
            for p in products:
                response_text += f"**💍 {p['PRODUCT_NAME']}**\n"
                if p.get("DESCRIPTION"):
                    response_text += f"_{p['DESCRIPTION']}_\n"
                response_text += "\n---\n"
            st.markdown(response_text)

    # ✨ 4. 단순 인사말 처리 (비-스트리밍 방식으로 변경)
    elif any(kw in prompt_lower for kw in greeting_keywords):
        with st.chat_message("ai"):
            response = st.session_state.chat_session.send_message(prompt)
            st.markdown(response.text)

    # ✨ 5. 그 외 모든 질문 처리 (비-스트리밍 방식으로 변경)
    else:
        role_adherence_prompt = (
            "당신은 'AuGold'라는 금 거래 사이트의 친절한 상담원 '골드'입니다. "
            "당신의 역할은 오직 AuGold 사이트의 상품, 금 시세, 서비스 이용 방법 등 사이트와 관련된 주제에 대해서만 대화하는 것입니다. "
            "아래 사용자 질문이 당신의 역할과 관련이 없는 경우(예: 프로그래밍, 요리, 정치, 일반 상식 등), "
            "다음 예시와 같이 **정확하게** 답변해야 합니다. 절대 다른 말을 덧붙이지 마세요.\n\n"
            "**답변 예시:**\n"
            "\"고객님, 죄송하지만 문의하신 내용은 제가 답변드리기 어려운 주제네요. 😥 "
            "저는 AuGold의 상품이나 서비스에 대해 안내해 드리는 역할을 하고 있답니다. "
            "대신 찾으시는 상품이 있으시거나, 다른 궁금한 점이 있으시면 기쁘게 도와드릴게요! 💎\"\n\n"
            "이제 아래의 실제 사용자 질문에 대해 답변하세요.\n"
            f"사용자 질문: \"{prompt}\""
        )

        with st.chat_message("ai"):
            response = model.generate_content(role_adherence_prompt)
            st.markdown(response.text)