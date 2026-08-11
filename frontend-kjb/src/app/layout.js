import "./globals.css";
import { AuthInitializer } from "@/components/AuthInitializer";

export const metadata = {
  title: "도시 귀농 프로젝트",
  description: "도심 속 텃밭 커뮤니티, 시티팜",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <head>
        <link
          rel="stylesheet"
          crossOrigin="anonymous"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css"
        />
      </head>
      <body className="flex min-h-svh justify-center font-[Pretendard,system-ui,sans-serif]">
        <AuthInitializer />
        {children}
      </body>
    </html>
  );
}
