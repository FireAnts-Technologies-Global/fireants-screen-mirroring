# Kế hoạch chuẩn hóa TopBar dùng chung

## Căn cứ thiết kế

- Figma: `Screen Mirroring 2 - 4/11/2026 (Copy)`, trang `UI`.
- Figwright liệt kê 23 instance `App bar` trên các frame thuộc 27 trạng thái đã audit; không xem đây
  là 23 component riêng.
- Baseline frame là 360×800. Status bar trong Figma cao 47dp.
- `Channels`/`Playlists`: title-only cao 24dp, x=16dp sau status bar, Inter Bold 20sp, màu
  `#162234`.
- `Import` và Player portrait: app bar cao 44dp tại y=55dp, padding ngang 16dp, khoảng cách
  back-title 12dp; nút Back 44×44dp, radius 14dp, viền `#7B96C6`, icon 20dp; title Inter Bold 20sp,
  `#162234`, căn trái. Import có vùng cân bằng 44dp phía cuối.
- `Media` và `Cast`: app bar cao 60dp tại y=47dp, padding ngang 16dp và dọc 8dp, khoảng cách 8dp;
  Back/action 44×44dp; title Inter Bold 22sp, căn giữa. Cast dùng action refresh viền radius 14dp;
  Media dùng action cast gradient, radius 22dp.
- `How to use`: cao 60dp tại y=47dp, padding ngang 16dp/dọc 8dp, gap 12dp; back 44×44dp radius 12dp,
  nền trắng và viền `#CADAF7`; title Inter Bold 19sp căn giữa; slot cuối 44dp để cân bằng.
- Figma dùng chung màu chữ `#162234`, nhưng có hai kiểu nút Back và nhiều cấu trúc action/title.
  Không ép các kiểu này vào cùng một cấu hình hình ảnh.
- Figwright `component_map`/`token_map` không nhận diện Android XML/View trong project (profile trả
  `framework=unknown`); `icon_map` cũng không có thư viện SVG. Vì vậy migration sẽ tham chiếu trực
  tiếp resource Android hiện có như Inter, màu/dimension Cast và VectorDrawable Figma; không tạo
  token tương đương một cách mù quáng.

## Kiểm kê Android

| Nhóm header hiện có                                                     | Màn hình / trạng thái                                                     | File chính                                                                                                                                             | View ID / binding / hành vi cần giữ                                                                                      | Quyết định                                                                                                                                             |
|-------------------------------------------------------------------------|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Shared legacy `cast_layout_topbar` (RelativeLayout, cấu hình từ Kotlin) | Media Photos/Videos/Choose device/Photo preview; WebBrowser; VideoControl | `cast_layout_topbar.xml`, `cast_activity_media_picker.xml`, `cast_activity_web_browser.xml`, `cast_activity_video_control.xml`; các Activity tương ứng | `topBar.imgBack`, `tvTitle`, `imgCast`, `imgSwap`, `lnActionHome`, `tvTitleHome`; back/cast/swap listeners và title động | Chuyển sang component chung theo các variant actions/standard sau khi API giữ được binding và runtime state; ưu tiên Media làm kiểm chứng action slot. |
| Toolbar Cast với refresh                                                | Cast Searching/Empty/Lost/PIN                                             | `cast_activity_cast.xml`, `CastActivity.kt`                                                                                                            | `toolbar`, `btnBack`, `tvTitle`, `btnRefresh`, `imgRefresh`; callback refresh/back                                       | Dùng variant centered-actions; giữ nguyên ID bằng binding adapter/wrapper hoặc cập nhật tham chiếu an toàn.                                            |
| Toolbar Import                                                          | Import URL/File/Success                                                   | `cast_activity_add_channel.xml`, `AddChannelActivity.kt`                                                                                               | `topAppBar`, `icBack`, `tvTitle`, `headerSpacer`; back và tab pager                                                      | Dùng variant standard; giữ header spacer và navigation pager.                                                                                          |
| Toolbar Guide                                                           | Guide URL/Stream/File                                                     | `cast_activity_guide.xml`, `GuideActivity.kt`                                                                                                          | `topAppBar`, back image, `tvTitle`, tab layout; giữ tab/page mapping                                                     | Dùng variant standard; không đổi mapping/navigation.                                                                                                   |
| Header All Channels                                                     | AllChannel list/search/empty                                              | `cast_activity_all_channel.xml`, `AllChannelActivity.kt`                                                                                               | Back, title, subtitle/count và actions/search; giữ listener và trạng thái tìm kiếm                                       | Dùng variant actions nếu slot API giữ được subtitle và actions; nếu không, giữ header riêng làm ngoại lệ có lý do.                                     |
| Header Home                                                             | Home connected/disconnected                                               | `cast_fragment_home.xml`, `HomeFragment.kt`                                                                                                            | Connection state, Connect, premium/settings và navigation                                                                | Giữ nguyên: đây là nội dung trạng thái kết nối trong Figma, không phải App bar độc lập.                                                                |
| Title-only section header                                               | Channels/Playlists                                                        | `cast_fragment_channel.xml`, `cast_fragment_channel_all.xml`, `cast_fragment_channel_category.xml`                                                     | `header` và các binding/tab/list liên quan                                                                               | Thay bằng variant compact title nếu giữ nguyên vị trí và layout.                                                                                       |
| Title-only section header                                               | Settings                                                                  | `cast_fragment_setting.xml`, `SettingFragment.kt`                                                                                                      | `tvSettingsHeader`, navigation các mục Settings                                                                          | Thay bằng variant compact title nếu screenshot/regression giữ đúng Figma.                                                                              |
| Toolbar How to use                                                      | How to use                                                                | `cast_activity_how_to_use.xml`, `HowToUseActivity.kt`                                                                                                  | `toolbar`, `btnBack`, `tvTitle`, `toolbarSpacer`; back finish                                                            | Màn đại diện đầu tiên; dùng variant centered và kiểm chứng screenshot node `57:614`.                                                                   |
| Toolbar Web Cast                                                        | Screen mirroring qua Web Cast                                             | `cast_activity_web_cast.xml`, `WebCastActivity.kt`                                                                                                     | `toolbar`, `btnBack`, `tvTitle`, `toolbarSpacer`; back/MediaProjection flow                                              | Dùng variant standard/centered nếu không đổi inset; nếu không, giữ header hiện tại.                                                                    |
| Player toolbar/controller                                               | IPTV Player portrait/landscape, drawer, controls hidden                   | `cast_activity_play_list.xml`, `cast_view_exo_controller.xml`, `cast_view_exo_controller_full.xml`, `PlayListActivity.kt`                              | `toolbar`, `btnToolbarBack`, `tvToolbarTitle`, player controls/callbacks/fullscreen                                      | Giữ PLAYER là ngoại lệ: là overlay điều khiển playback với ẩn/hiện theo orientation, không phải app navigation bar.                                    |
| Language header                                                         | Language Activity mở từ Settings và luồng khởi động                       | `activity_language.xml`, `LanguageActivity.kt`                                                                                                         | `imgBack`, `tvTitle`, `btnNext`, trạng thái first-launch                                                                 | Giữ nguyên hoàn toàn vì layout được dùng cho startup Language Selection bị loại khỏi phạm vi.                                                          |
| Bottom sheet title                                                      | VideoListBottomSheet                                                      | `cast_layout_bottom_sheet_video_list.xml` và `VideoListBottomSheet.kt`                                                                                 | `tvVideoSheetTitle`, RecyclerView và dismiss/callback                                                                    | Chỉ kiểm kê; giữ title header riêng vì thuộc chrome của modal/sheet.                                                                                   |
| Dialog title/header                                                     | Dialogs PIN, rewarded, force update, import success, error, timer/end     | Layout dialog tương ứng                                                                                                                                | Dialog callbacks, cancelability, data và window behavior                                                                 | Không chuyển sang app TopBar; khác cấp điều hướng và có cấu trúc dialog riêng.                                                                         |
| Startup headers                                                         | Splash, Welcome, Onboarding                                               | Layout/Activity startup                                                                                                                                | Tất cả state liên quan startup                                                                                           | Loại trừ; không sửa/đổi style.                                                                                                                         |

## Component đề xuất

Tạo `FigmaTopBarView` dạng custom Android View với layout nội bộ dùng XML, không thêm
dependency/Compose. API cần hỗ trợ:

- `COMPACT_TITLE`, `STANDARD`, `CENTERED_ACTIONS`, `TRANSPARENT`; không đưa PLAYER vào component.
- Title, back icon/content description/listener, action slot cuối (và slot phụ nếu màn hiện có yêu
  cầu), alignment/cân bằng slot.
- Chiều cao, padding, style title và nền chọn theo variant/style resource; dùng lại Inter,
  màu/dimension, icon hiện có.
- Giữ target chạm 44dp và accessibility labels; chỉ cho phép multiline/title sizing theo đặc điểm
  Figma của từng màn.
- ID bên trong component không thay thế callback hay navigation của màn; caller gắn listener tường
  minh.

## Rủi ro và thứ tự migration

1. Xác minh layout/component API hiện có và dependency ViewBinding.
2. Tạo component + styles/dimens/drawables cần thiết.
3. Migrate `HowToUseActivity` làm đại diện (Figma `57:614`), build/install, screenshot và kiểm tra
   Back.
4. Migrate Cast (`33:633`, `33:564`) và Media (`23:933`, `23:1014`) để xác minh centered title cùng
   action slot.
5. Migrate Import (`43:749`, `43:795`) và Guide; giữ nguyên tab/page/navigation.
6. Migrate compact title cho Channels/Playlists và Settings.
7. Xem xét AllChannel/WebCast và 3 consumer legacy; giữ ngoại lệ nếu API không mô tả đúng cấu trúc
   thật.
8. Chạy build/unit test, chụp ảnh các màn đã đổi và kiểm tra Back/action; rà lại bốn Player states
   cùng 18 trạng thái non-Figma mà không sửa chúng.

Rủi ro chính: tên binding trong include, window insets/status bar và toolbar đang được cấu hình bằng
Kotlin; một thay đổi view hierarchy có thể làm đổi vị trí nội dung hoặc ViewBinding. Bản working
tree trước khi migration đã có chỉnh sửa quảng cáo và tài nguyên Kotlin/Gradle đang tồn tại; các
thay đổi đó không thuộc phạm vi này và phải được giữ nguyên.

## Nhật ký thực hiện

- Đã thêm `FigmaTopBarView` và layout nội bộ XML. Component hiện hỗ trợ compact title, standard,
  centered, centered actions và transparent; cấu hình được title, kiểu Back, action icon/background,
  kích thước Back và listener.
- Đã truy nguyên compile blocker: ba import ViewBinding trong `LanguageActivity.kt`,
  `OnBoardingActivity.kt` và `SplashActivity.kt` đã bị xóa trong commit quảng cáo trước đó;
  namespace/applicationId và binding generation đều đúng. Chỉ thêm lại đúng ba import; không đổi UI,
  điều hướng hay hành vi startup.
- Đã xác minh How to use (Figma `57:614`) trên SM-A325F Android 13 qua lối Home → Tutorial.
  Screenshot `docs/figma/screenshots/shared-topbar-how-to-use-after.png`: TopBar 60dp, Back 44dp,
  title căn giữa, status bar/insets và nội dung giữ đúng vị trí theo baseline. Đã chạm Back và xác
  nhận trở về Home. Có quảng cáo native test ở vùng dưới nội dung; overlay AdMob validator được đóng
  trước khi chụp.
- Đã migrate Cast (`33:633`, cùng header cho các state Cast khác) sang centered-actions; giữ
  callback Back qua `AdsManager.loadAndShowInterBack`, action refresh vẫn gọi `viewModel.refresh()`
  và giữ animation xoay. Screenshot runtime: `docs/figma/screenshots/shared-topbar-cast-after.png`;
  đã chạm Refresh và Back. Back mở interstitial test như luồng hiện hữu; đóng test ad và xác nhận
  quay về Home. Chưa chụp lại Cast sau khi animation refresh kết thúc.
- Đã migrate Import (`43:749`, `43:795` và state dùng chung header) sang standard, giữ spacer phải
  44dp và back callback. Screenshot runtime `docs/figma/screenshots/shared-topbar-import-after.png`
  cho thấy header/back/tựa đề đúng cấu trúc; nội dung nhập và pager không đổi.
- Đã migrate Guide sang centered header và giữ callback Back/tab pager. Không tìm thấy callsite UI
  nào trong app gọi `IptvRoutes.startGuideActivity`; để runtime-verify, chỉ bật `exported=true` tạm
  trong DevDebug, mở Activity và chụp `docs/figma/screenshots/shared-topbar-guide-after.png`, sau đó
  khôi phục `exported=false` trong manifest. Screenshot xác nhận title/back và nội dung tab URL hiển
  thị bình thường; luồng người dùng hiện hành vẫn không có entry point.
- Đã migrate title header Channels/Playlists và Settings fragment sang compact title, giữ nguyên ID
  ràng buộc, vị trí và chiều cao layout. Screenshot runtime: `shared-topbar-channels-after.png`,
  `shared-topbar-settings-after.png`; cả hai hiển thị bình thường, không thấy lệch hệ thống/insets
  hoặc nội dung.
- Đã migrate Media (`23:933`, `23:1014` và các state dùng chung header): Photos/Videos dùng
  centered-actions với Cast; Photo Preview đổi sang standard, căn trái và ẩn action. Giữ callback
  Back, mở Cast/device panel, đổi title và cập nhật icon theo state. Runtime screenshot được
  chụp/xem trên thiết bị; đã xóa ảnh khỏi workspace vì ảnh chứa thumbnails từ MediaStore của thiết
  bị. Photo Preview chưa được mở để tránh thao tác với ảnh người dùng.
- Các header động `AllChannel` (search/subtitle/actions), `WebCast`, legacy shared
  `cast_layout_topbar` (WebBrowser/VideoControl) và Player landscape/controller giữ nguyên làm ngoại
  lệ; cấu trúc/state hoặc callback không khớp cấu hình component hiện tại. Dialogs/bottom sheets
  không thuộc app navigation TopBar. Splash, startup Language, Onboarding và Welcome không được
  redesign; các thay đổi đang có ở các tệp này ngoài phạm vi được giữ nguyên.
- Build/verification: `./gradlew :app:compileDevDebugKotlin` — thành công;
  `./gradlew :app:installDevDebug` — thành công, đã cài lên SM-A325F Android 13;
  `./gradlew :app:testDevDebugUnitTest` — thành công; `git diff --check` — thành công. Gradle vẫn in
  cảnh báo định dạng `%02d` của chuỗi timer ở một số locale; không chỉnh vì không liên quan.
- `./gradlew :app:lintDevDebug` — thất bại do lint backlog tồn tại trong project: 124 lỗi và 1.113
  cảnh báo. Lỗi đầu tiên là `MissingSuperCall` tại `OnBoardingActivity.kt:218` (startup flow ngoài
  phạm vi); không sửa lint ngoài phạm vi.
- Bản build trung gian từng vấp do enum `import` dùng từ khóa Java trong resource attr do migration
  này thêm; đổi tên thành `importStyle`, sau đó merge/compile/install đều thành công.

## Trạng thái sau lượt tiếp tục

| Màn / nhóm                                                   | Kết quả migration               | Runtime screenshot                   | Ghi chú                                                                    |
|--------------------------------------------------------------|---------------------------------|--------------------------------------|----------------------------------------------------------------------------|
| How to use                                                   | Đã migrate, kiểm chứng          | `shared-topbar-how-to-use-after.png` | Back đã kiểm tra; ảnh có native test ad phía dưới                          |
| Cast                                                         | Đã migrate, kiểm chứng chính    | `shared-topbar-cast-after.png`       | Refresh và Back được kích hoạt; Back hiển thị interstitial test vốn có     |
| Import Channels                                              | Đã migrate, kiểm chứng          | `shared-topbar-import-after.png`     | Đã chụp trạng thái URL mặc định                                            |
| Guide                                                        | Đã migrate, kiểm chứng runtime  | `shared-topbar-guide-after.png`      | Chụp bằng DevDebug tạm mở exported; manifest đã khôi phục `exported=false` |
| Media Photos/Videos                                          | Đã migrate, kiểm chứng header   | Không lưu ảnh                        | Chụp và xem runtime; bỏ ảnh vì chứa thumbnails MediaStore của thiết bị     |
| Media Photo Preview                                          | Đã migrate, chưa runtime verify | —                                    | Không mở ảnh người dùng để tạo state kiểm thử                              |
| Channels / Playlists                                         | Đã migrate, kiểm chứng          | `shared-topbar-channels-after.png`   | Header dùng chung nằm ở fragment cha                                       |
| Settings fragment                                            | Đã migrate, kiểm chứng          | `shared-topbar-settings-after.png`   | Giữ chiều cao 40dp hiện có để không đẩy danh sách                          |
| Media, AllChannel, WebCast, WebBrowser, VideoControl, Player | Giữ ngoại lệ                    | —                                    | Cần API/variant bổ sung và xác minh theo từng state trước khi thay         |

**Tổng quan:** 7 vị trí header đã chuyển sang component (How to use, Cast, Import, Guide, Media,
Channels/Playlists, Settings); 6 ảnh runtime được lưu. Media có ảnh runtime đã chụp/xem rồi xóa vì
chứa media người dùng. Media Photo Preview vẫn chưa runtime-verify. Các ngoại lệ có cấu trúc
động/chuyên biệt chưa được chuẩn hóa trong lượt này.
