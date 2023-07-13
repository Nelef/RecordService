# 버퍼 사용하여 녹취 중 임시 파일 내보내는 안드로이드 녹취 서비스 모듈
![스크린샷 2023-07-13 오전 9 08 17](https://github.com/Nelef/RecordService/assets/40861980/c46a9362-9f78-4ca1-b98f-e235d7643c90)

### 미리보기
![Screen_Recording_20230713_110943_RecordModule](https://github.com/Nelef/RecordService/assets/40861980/b700cd88-2a4f-446f-a9f4-8c531f006d5d)

### 다운로드
RecordModule.apk - [OneDrive에서 다운로드](https://kumohackr-my.sharepoint.com/:u:/g/personal/imoneleft_kumoh_ac_kr/EYzrEoepnydNrDfUh6H766UBSAE4RT2jt1CpahcpC2FMdA?e=BpQbE0) / [Synology에서 다운로드](http://gofile.me/76Cd2/8Gi5ZfS0r)

### 사용한 기술
- Android kotlin, compose
- 기본 라이브러리 : AudioRecord, MediaMuxer

### 최소 Android 버전
- 안드로이드 6.0 이상 (Android 6 ~ 13 테스트 완료)

### Interface
```kotlin
interface Record {
    fun recordStart(
        context: Context,
        filePath: String,
        fileTempPath: String
    ): Boolean
    fun recordResume(): Boolean
    fun recordPause(): Boolean
    fun recordStop(context: Context): Boolean // 일반 저장(중지)
    fun recordTempSave(): Boolean
    fun recordTempReStart(): Boolean
    fun recordCancel(context: Context): Boolean
    fun recordDestroy()
}
```

- recordStart(context, 녹취 저장 위치, 녹취 임시저장 위치)
    - 녹취 시작
- recordResume()
    - 녹취 재시작
- recordPause()
    - 녹취 일시정지
- recordStop(context)
    - 녹취 종료(원본 파일, 임시 파일) 저장
- recordTempSave()
    - 임시저장 완료 - 완료 이후 서버로 임시저장파일 보낸 뒤, 아래 재시작(recordTempReStart)을 하면 됨.
- recordTempReStart()
    - 임시저장 재시작 - 이어서 새로 녹취(파일 넘버+1 되어 저장됨)[test_001.mp3 → test_002.mp3]
- recordCancel(context)
    - 서비스 종료 - 정상 종료(recordDestroy 호출됨)
- recordDestroy()
    - 서비스 종료 - 급하게 앱 종료 시 사용

### MainActivity에 인터페이스, ui 사용 예제 남겨놓음.
<img width="1083" alt="스크린샷 2023-07-13 오전 10 39 55" src="https://github.com/Nelef/RecordService/assets/40861980/bb948fcb-23fa-4c7b-af03-e9edf18cab77">

---
---

# 안드로이드 녹음 기능에 대한..
### MediaRecorder 과 AudioRecord 사용하여 녹취기능 제작. 차이점 정리.

## MediaRecorder 한계점..
MediaRecorder 을 기존에 사용하고 있었는데, 
<br>녹취는 그대로 유지하면서(통녹취 파일)
<br>동시에 녹취 중인 파일을 별도의 백업파일로 저장해야하는 상황 발생.
![스크린샷 2023-07-13 오전 9 08 17](https://github.com/Nelef/RecordService/assets/40861980/c46a9362-9f78-4ca1-b98f-e235d7643c90)
<br>→ 버퍼를 사용하지 않는 MediaRecorder은 무조건 녹취를 종료해야만 저장할 수 있음.
<br>→ 따라서 버퍼를 사용할 수 있는 AudioRecord로 변경.

참고 - [자료조사(2) - 안드로이드의 오디오를 녹음하는 세 가지 방법](https://techlog.gurucat.net/130)

## AudioRecord 한계점..
`bufferSize`(샘플레이트, 채널, 오디오포멧) 정도는 설정 가능하지만 기본적으로는 아날로그 파형을 디지털로 변환한 신호를 버퍼로 기록.
<br>→ PCM 파일로 저장됨.
<br>→ 이를 헤더부분만 추가시켜서 wav 파일로 간단히 만들 수 있지만 압축되지 않은 이 파일은 파일 크기가 큼.
<br>→ 따라서 인코딩해서 m4a, mp3 같은 파일 만들라면 딴거 필요함.

1. LAME MP3 라이브러리 써서 wav → mp3 로 인코딩을 하던가
2. MediaMuxer 써서 wav → mp3(aac) 로 인코딩을 하던가

LGPL 라이센스라서 상관은 없는데 따로 외부 라이브러리 사용 안해도 되는 2번 방법 채택.

Android의 **`MediaMuxer`** 클래스가 지원하는 인코딩 확장자 목록

- **`mp4`** : MPEG-4 컨테이너 포맷
- **`webm`** : WebM 컨테이너 포맷
- **`ts`** : MPEG-2 전송 스트림 컨테이너 포맷
- **`aac`** : AAC 오디오 스트림을 위한 .aac 파일
- **`mka`** : Matroska 오디오 스트림을 위한 Matroska 컨테이너

기존에도 aac 포맷으로 인코딩 되고, mp3 확장자로 저장하는 방식이라서 기존과 비슷하게 짤 수 있을 듯해서 코드 작성

# 과정(트러블슈팅)
- 2022.12 - mediaRecorder 이용하여 녹취 서비스 생성.
    - mediaRecorder 기능
        - 비트레이트 설정(128kbps)을 하면 자동으로 aac 파일로 인코딩되어 저장됨.
        - 대부분이 모듈화 되어 있어 코드가 적음.
    - mediaRecorder 한계
        - 안드로이드 7.0 미만 기기는 녹취 일시중지 기능 미지원.
        - 녹음되고 있는 버퍼를 직접 만질 수 없음.
<br><br>
- 2023.03 - 녹취 중 임시저장 기능을 넣기 위해 버퍼를 만질 수 있는 audioRecord로 변경
<br>인코딩은 mediaMuxer을 이용.
<br>[mediaRecorder -> audioRecord + mediaMuxer]
    - 당시 지원 가능한 Android 버전
        - Android 12, 13
    - 발생한 문제
        - Android 버전에 따른 문제 발생(앱 강제종료)
            - ~~Android 11 이하에서 presentationTimeUs(타임스탬프) 관련 문제 발생~~ → 2023.04 해결
            - ~~6.0, 7.1.1 파일헤더부분 128kb제한으로 인한 저장 관련 문제 발생~~ → 2023.06_1 해결
<br><br>
- 2023.04 - Android 11 이하 문제 해결
    - 문제 발생한 코드
        - `mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)`
    - 원인
        - mediaMuxer의 타임스탬프가 자동으로 순차대로 찍히지 않아 인코딩 과정에서 에러 발생하며 앱 강제종료됨.<br>(Android 12이상에서는 순차대로 찍혀서 정상작동)
    - 해결
        - bufferInfo에 타임스탬프(presentationTimeUs)정보를 시스템 시간을 이용해 주고 해결.
            ```kotlin
            val presentationTimeUs = System.nanoTime() / 1000
            bufferInfo.presentationTimeUs = presentationTimeUs
            ```
            
    - 추가 문제 발생
        - ~~일시 정지를 사용 시 파일 재생 시간과 실제 재생 시간이 다른 오류~~ → 2023.06_2 해결
<br><br>
- 2023.06_1 - Android 6.0 ~ 7.1.1 에서 파일헤더부분 문제 해결
    - 문제 발생한 코드
        - `mediaMuxer.start()` - 두번째 다시 시작 했을 때 발생.
    - 원인
        - 안드로이드 6.0 ~ 7.1.1에 내장되어 있는 mediaMuxer 버전의 aac 저장 코덱의 헤더가 128bytes 제한이 걸려있어, 다시 시작한 mediaMuxer의 헤더를 쓸 수 없음.
        - 안드로이드 구글 소스 참고 - [https://android.googlesource.com/platform/frameworks/av/+/f5943271b08f67939020c45340f2df06a5c39a18%5E%21](https://android.googlesource.com/platform/frameworks/av/+/f5943271b08f67939020c45340f2df06a5c39a18%5E%21)
    - 해결
        - `backupFormat: MediaFormat` 포멧을 백업하여 미리 저장해 두고 mediaMuxer의 요소로 주입하여 해결.
    - 테스트 시 문제 발생
        - Android 버전에 따른..
            - 6.0 - backupFormat 가능 (헤더 128bytes 제한)
            - 7.1.1 - backupFormat 가능 (헤더 128bytes 제한)
            - 8.0 - 둘 다 됨
            - 9.0 - 둘 다 됨
            - 10 - 둘 다 됨
            - 11 - originalFormat만 가능 (backupFormat 시 stop 불가)
            - 12 - originalFormat만 가능 (backupFormat 시 stop 불가)
            - 13 - originalFormat만 가능 (backupFormat 시 stop 불가)
    - 추가 해결
        - Android 버전별로 분기 처리 하여 해결(9.0 기준으로 짜름)
<br><br>
- 2023.06_2 - 일시 정지 사용 시 타임스탬프 오류 해결
    - 문제
        - 일시 정지 사용 시 저장 된 파일을 읽어보니 파일 재생 시간이 이상함.
    - 원인
        - 시스템시간을 타임스탬프로 활용하니 일시 정지 후 재개하면 시스템 시간이 순차대로 올라간게 아니라 끊겼다가 팍 올라가서 파일 재생 시간이 이상해짐.
    - 해결
        - 별도 timer 생성하여 presentationTimeUs의 값을 일시 정지 시 멈췄다가, 재개할 시 올라가도록 설정
    - 추가 문제 발생
        - ~~Android 13에서 파일 재생 시간과 실제 재생 시간이 다른 오류~~ → 2023.07 해결
<br><br>
- 2023.07 - Android 13 에서 타임스탬프 문제
    - 문제
        - 녹취 시간이 살짝 다름(10초 녹음 시 11초 타임스탬프..)
    - 원인
        - 기본적으로 Android 12 이상 mediaMuxer에서는 타임스탬프를 자동으로 주기 때문에 별도 타임스탬프는 필요하지 않았음.
        - 근데 하위호환 한다고 강제로 내가 만든 타임스탬프를 주입하니 정확하게 처리되는 mediaMuxer에 비해 달라버려서 발생하는 문제
    - 해결
        - 기본적으로 제공되는 mediaMuxer 타임스탬프를 쓰되, 타임스탬프 오류가 발생할 땐 내가 만든 타임스탬프 주입하도록 바꿈.
