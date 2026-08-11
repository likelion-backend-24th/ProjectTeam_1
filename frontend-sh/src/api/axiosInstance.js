import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080", // 백엔드 서버 주소에 맞게 수정
  withCredentials: true,
});

export default api;
