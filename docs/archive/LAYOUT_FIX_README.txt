DaySpark v5.2 — 화면 깨짐 수정 패치

원인: Thymeleaf 템플릿에서 <i .../> 를 HTML에 사용했습니다.
HTML에서 i는 빈 태그가 아니므로 <i ...></i> 로 닫아야 합니다.
브라우저가 잘못된 HTML을 복구하면서 카드 내부 배치와 서식이 깨질 수 있었습니다.

패치 적용
1. DaySpark 서버 창과 크롬 DaySpark 탭을 닫습니다.
2. 이 ZIP의 src 폴더를 현재 DaySpark_v5_1_FULL_Quiet_Chrome_Reports 폴더에
   그대로 덮어씁니다. 기존 .tools, db, reports, Maven 파일은 그대로 둡니다.
3. 기존 00_START_DAYSPARK_CHROME.bat을 더블클릭합니다.
4. 화면이 캐시되었다면 크롬에서 Ctrl+F5를 눌러 새 CSS를 받습니다.

수정 내용: index, challenge-detail, calendar, stats의 잘못된 i 빈 태그 수정,
기타 템플릿의 CSS 캐시 버전 갱신, 반응형 도전 카드 CSS 보강.

이 패치는 HTML/CSS만 바꿉니다. Java/DB/Maven을 재설치하지 않습니다.
