# 검증 결과 (2026-09-30)

- 폰 / Wear APK assembleDebug: 성공.
- 폰 / Wear lintDebug: 오류 없음. Android 기본 View의 deprecated API와 표시용 리소스 경고가 있음.
- TimerStateTest: 24 assertions PASS.
  재부팅 복구(남은 시간 유지, 꺼져 있는 동안 끝난 회차 1회 완료, checkpoint, duration 상한)와
  종료 시각에 도착한 reset/boot/tick/end/skip 판정을 포함한다.
- Android 14 빈 에뮬레이터: android_smoke.py 8 checks PASS.
- WidgetProbe: RemoteViews 실제 inflation, 준비 시간 표시, 시작 PendingIntent,
  실행 중 시스템 Chronometer, 일시정지 PendingIntent 모두 PASS.
- 워치 APK: 같은 에뮬레이터에서 화면 384×384 / density 320 (192dp)로 배치 확인.
  Wear OS 시스템 및 실제 원형 기기의 진동·절전·제스처 확인은 아직 수행하지 않았음.
  원형 화면 여백(`isScreenRound`일 때 짧은 변의 14%)은 코드에 들어 있으나, 원형 에뮬레이터나 실기기 캡처는 아직 없다.

폰 테스트는 일시정지 시간 보존, 백그라운드 프로세스 재생성 후 종료 시각 보존,
백그라운드 시스템 알람, 실제 종료 알림 게시, 완료 기록의 중복 방지,
네 번째 집중 이후 긴 휴식 자동 시작을 포함합니다.

화면 캡처는 previews/에 저장했습니다. 위젯 이미지는 instrumentation으로 실제
RemoteViews를 Android Activity에 렌더링한 것으로 런처에 배치한 화면은 아닙니다.
