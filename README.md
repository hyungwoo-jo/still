# Still

[GitHub](https://github.com/hyungwoo-jo/still) · [최신 APK](https://github.com/hyungwoo-jo/still/releases/latest)

폰: [다운로드](https://github.com/hyungwoo-jo/still/releases/latest/download/still-phone.apk) · 워치: [다운로드](https://github.com/hyungwoo-jo/still/releases/latest/download/still-wear.apk)

Flow의 차분한 타이머 화면을 참고해 만든 개인용 Android / Wear OS 뽀모도로 앱.
작업명은 Still이며, 화면과 아이콘은 이 프로젝트에서 직접 구성했습니다.

## 첫 버전

- 집중 / 짧은 휴식 / 긴 휴식, 1–180분 설정, 긴 휴식 주기 2–8회.
- 시작·일시정지·재개·현재 회차 초기화·건너뛰기.
- 집중과 휴식의 자동 시작을 각각 선택. 기본은 수동 시작.
- 시스템 알람으로 종료 처리. 앱 프로세스가 사라져도 타이머 유지.
- 종료 소리 / 진동을 각각 선택. Android 시스템 알림 설정이 최종 우선합니다.
- 알림 패널에서 남은 시간 확인·일시정지·건너뛰기.
- 폰 홈 위젯: 시스템 Chronometer 카운트다운과 시작·일시정지.
- 오늘 및 최근 7일의 완료 회차 / 집중 시간. 건너뛴 집중은 기록하지 않음.
- 폰: 시스템 / 라이트 / 다크 테마. 워치: 검정 배경과 작은 화면 구성.
- Wear OS 독립 실행 앱. 폰과 워치의 타이머·설정·기록은 각각 저장됩니다.
- 광고, 구매, 계정, 인터넷 권한, 클라우드 없음.

## 설치 및 빌드

폰 Android 8 이상, 워치 Wear OS의 Android 11 이상.

```sh
./gradlew :phone:assembleDebug :wear:assembleDebug
```

- 폰 APK: `phone/build/outputs/apk/debug/phone-debug.apk`
- 워치 APK: `wear/build/outputs/apk/debug/wear-debug.apk`

개발용 debug APK와 배포용 release APK를 구분합니다. 설치는 사용자가 직접 진행합니다.
GitHub Releases에는 같은 앱 전용 키로 서명한 release APK를 올립니다.
Android SDK 34 / Java 17 이상 / Gradle 8.2.1 / AGP 8.2.2.
`local.properties`의 SDK 경로는 각 PC에 맞춰 설정합니다.

```sh
adb install -r phone/build/outputs/apk/debug/phone-debug.apk
adb -s <watch-serial> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

위젯은 홈 화면의 위젯 선택 목록에서 Still을 추가합니다.
처음 타이머를 시작할 때 알림 권한을 허용해야 종료 알림을 받습니다.
Android 12에서는 정확한 알람 권한이 필요할 수 있으며 앱에서 설정을 안내합니다.

## 검증

```sh
mkdir -p build/core-tests
javac -d build/core-tests shared/src/main/java/com/hwserve/still/TimerState.java tests/TimerStateTest.java
java -cp build/core-tests TimerStateTest
./gradlew :phone:lintDebug :wear:lintDebug
```

타이머 상태 머신 24개 검증: 일시정지 시간 제외, 정확한 종료 경계,
완료만 기록, 수동 건너뛰기, 4회 후 긴 휴식, 사용자 주기, 자동 시작, 초기화,
재부팅 뒤 남은 시간, 종료 시각의 초기화는 완료로 세지 않음.

## 동작상 한계

- 기기를 재부팅하면 진행 중인 타이머는 일시정지합니다. 아직 끝나지 않았으면 벽시계 기준으로 남은 시간을 유지하고, 꺼져 있는 동안 끝났으면 그 회차만 완료한 뒤 다음 회차를 일시정지합니다. 벽시계를 크게 바꾸면 이 남은 시간이 달라질 수 있습니다.
- Android 강제 종료는 시스템 알람도 취소하므로 앱을 다시 열어야 합니다.
- 정확한 알람 권한이 없으면 시스템이 종료 알림을 늦출 수 있습니다.
- 알림 소리·진동은 무음·방해금지·알림 채널 설정에 영향을 받습니다.
- 워치 타일·워치페이스 컴플리케이션과 폰↔워치 연결은 현재 구현 범위에 없습니다.
- Play Store 배포용 target SDK 상향·release 서명은 별도 작업입니다.
- Wear OS 실물 기기의 진동·원형 화면·저전력 동작은 추가 확인이 필요합니다.

리서치와 설계 근거는 [docs/design.md](docs/design.md), 실제 실행 화면은 `previews/`에 둡니다.

추가 확인: Android 14 에뮬레이터에서 통합 검사 8개 통과.
위젯 RemoteViews 실제 렌더링 및 시작·일시정지 PendingIntent 통과.
`previews/watch-layout.png`는 폰 에뮬레이터를 192dp 크기로 바꿔 Wear APK를 실행한 화면이며 실제 Wear OS 캡처는 아닙니다.

에뮬레이터 통합 검사(테스트용 데이터로 앱 상태를 덮어쓰므로 일회용 에뮬레이터에서만 실행):

```sh
ANDROID_SERIAL=emulator-5582 python3 tests/android_smoke.py
./gradlew :phone:assembleDebugAndroidTest
adb -s emulator-5582 install -r phone/build/outputs/apk/androidTest/debug/phone-debug-androidTest.apk
adb -s emulator-5582 shell am instrument -w com.hwserve.still.test/com.hwserve.still.WidgetProbe
```

## GitHub 빌드 및 업데이트

- main 푸시 / PR: 타이머 테스트, 두 앱 debug 빌드, lint, 테스트 APK artifact 생성.
- 버전 태그 푸시 / Actions 수동 실행: 검증 이후 앱 전용 키로 release APK를 서명하고 GitHub Release 생성.
- `version.properties`의 versionName과 versionCode를 함께 올립니다. 태그는 `v<versionName>`이어야 합니다.
- 배포 파일 이름은 `still-phone.apk`, `still-wear.apk`로 유지하므로 위의 최신 다운로드 링크를 계속 쓸 수 있습니다.
- APK를 열어 설치하면 기존 release 설치 위에 업데이트되어 로컬 기록이 유지됩니다. 자동 설치는 하지 않습니다.
- 이전 개발용 debug APK를 이미 설치했다면 release APK와 서명이 달라 첫 전환 때 제거 후 설치가 필요합니다.

```sh
# 예: versionName=0.1.2, versionCode=3으로 변경한 후
git add version.properties
git commit -m "Release 0.1.2"
git tag v0.1.2
git push origin main v0.1.2
```

서명 키는 로컬의 ignored `.signing/`와 GitHub Actions Secrets에만 보관합니다.
Secrets: `STILL_KEYSTORE_BASE64`, `STILL_KEYSTORE_PASSWORD`. 서명 키를 교체하지 않습니다.
로컬 release 빌드는 `STILL_KEYSTORE` 및 `STILL_KEYSTORE_PASSWORD` 환경 변수를 사용합니다.

단일 GitHub CI는 키를 복원해 폰과 워치 APK를 서명합니다. 일반 PR 검사에는 키가 제공되지 않습니다.
