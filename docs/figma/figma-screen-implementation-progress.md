# Tiến độ triển khai các trạng thái Android có Figma

Ngày cập nhật: 2026-11-10

## Phạm vi và phương pháp

- Tiếp tục từ báo cáo trước, không quét lại từ đầu. Đối chiếu source hiện tại với danh sách 27 trạng thái đã có node ID.
- Figwright đã xuất screenshot tham chiếu cho 22 frame còn lại vào `docs/figma/figma-reference-export/`; 5 frame IPTV Player và Import URL Empty nằm trong `docs/figma/figma-references/`. Trang `Store Listing` chỉ là ảnh tiếp thị, không tính thành trạng thái Android.
- Các trạng thái startup Splash, Language Selection lúc khởi động, Onboarding/Intro và Welcome/Get Started bị loại. Settings/Language option và How to use vẫn thuộc phạm vi.
- `COMPLETED_AND_VERIFIED` chỉ được dùng khi có runtime screenshot đối chiếu. XML tồn tại hoặc build thành công không được tính là xác minh giao diện.
- Một số trạng thái phụ thuộc vào thiết bị cast, thư viện ảnh/video, file M3U hoặc một luồng IPTV thực sự. Không tạo luồng phát thành công giả và không nhập playlist để tạo dữ liệu ứng dụng.

## Tóm tắt

| Trạng thái | Số lượng |
|---|---:|
| Tổng trạng thái có Figma trong phạm vi | 27 |
| COMPLETED_AND_VERIFIED trước lượt này | 20 |
| UI_VERIFIED_WITH_FIXTURE trong lượt này | 7 |
| END_TO_END_VERIFIED trong 7 trạng thái vừa kiểm tra | 0 |
| Chưa có bằng chứng screenshot UI | 0 |
| PARTIALLY_IMPLEMENTED | 0 |
| NEEDS_VISUAL_FIX | 0 |
| Còn cần xác minh UI runtime | 0 |
| Còn chờ E2E với receiver / dịch vụ quảng cáo thật | 7 |

Đã đối chiếu bốn trạng thái IPTV Player bằng screenshot runtime sau lần chỉnh XML/controller trước. Trong lượt này, bảy trạng thái còn lại đều được mở trên emulator qua harness chỉ dành cho debug, chụp screenshot runtime và đối chiếu với frame Figma. Bảy trạng thái được đánh dấu `UI_VERIFIED_WITH_FIXTURE`; không trạng thái nào được đánh dấu `END_TO_END_VERIFIED`, vì fixture không thực hiện discovery/kết nối tới receiver, xác thực PIN hay yêu cầu rewarded ad.

## Checklist ưu tiên còn lại

| Ưu tiên | Screen / UI State | Node ID | Công việc E2E còn lại |
|---:|---|---|---|
| 1 | Home connected; Media Choose device; Cast searching, lost connection, PIN empty/entered, rewarded ad | `20:653`, `23:1014`, `33:564`, `33:679`, `33:725`, `33:808`, `33:891` | Chỉ cần chạy E2E khi có receiver thật; PIN cần mã từ TV; rewarded prompt cần ad flow khả dụng. Các trạng thái UI đều đã có screenshot fixture bên dưới. |

## Danh sách đầy đủ 27 trạng thái

| Screen / State | Figma Node ID | Android Files | Previous Status | Final Status | Screenshot Verified | Remaining Issues |
|---|---|---|---|---|---|---|
| Home — chưa kết nối TV | `13:564` | `HomeFragment.kt`; `cast_fragment_home.xml`; `cast_activity_home.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-home-no-tv-360x800.png` | Không. Đối chiếu cấu trúc Home no-TV với frame `13:564`. |
| Home — đã kết nối TV | `20:653` | `HomeFragment.kt`; `cast_fragment_home.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/home-connected.png` | Chỉ dựng nội dung Connected để kiểm tra hình ảnh; không kết nối receiver thật. |
| Channels — danh sách trống | `43:564` | `ChannelFragment.kt`; `ChannelAllFragment.kt`; `cast_fragment_channel.xml`; `cast_layout_empty_channels.xml` | COMPLETED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-channels-empty-360x800.png` | Không. |
| Channels — danh sách đã tải | `43:601` | `ChannelFragment.kt`; `ChannelAllFragment.kt`; `ChannelItemAdapter.kt`; `cast_fragment_channel_all.xml`; `cast_item_iptv_channel.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-channels-loaded-fixture.png` | Đã import fixture M3U có channel, mở danh sách và đối chiếu row/thumbnail. |
| Playlists — danh sách trống | `43:654` | `ChannelFragment.kt`; `ChannelCategoryFragment.kt`; `cast_fragment_channel_category.xml`; `cast_layout_empty_playlist.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-playlists-empty-360x800.png` | Không. |
| Playlists — danh sách đã tải | `43:691` | `ChannelCategoryFragment.kt`; `CategoryAdapter.kt`; `cast_fragment_channel_category.xml`; `cast_item_category.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-playlists-loaded-fixture.png` | Fixture playlist đã hiển thị và đối chiếu; fixture đã xóa khỏi DB sau chụp. |
| Import M3U — URL trống | `43:749` | `AddChannelActivity.kt`; `AddChannelUrlFragment.kt`; `cast_activity_add_channel.xml`; `cast_fragment_add_channel_url.xml` | NEEDS_VISUAL_FIX | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-import-url-empty-360x800.png` | Ô tên để trống đúng frame; placeholder đã sửa; status bar sáng theo nền Figma. Tên playlist gợi ý vẫn được áp dụng khi submit để giữ hành vi hiện có. |
| Import M3U — URL đã nhập | `43:772` | `AddChannelUrlFragment.kt`; `ValidationHelper.kt`; `cast_fragment_add_channel_url.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-import-url-entered-360x800.png` | Chỉ kiểm tra hiển thị text trên form; không submit URL mẫu và không kiểm tra phản hồi máy chủ. |
| Import M3U — File trống | `43:795` | `AddChannelFileFragment.kt`; `FileHelper.kt`; `cast_fragment_add_channel_file.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-import-file-empty-360x800.png` | Empty state và nút chọn file khớp cấu trúc frame. |
| Import M3U — File đã chọn | `43:818` | `AddChannelFileFragment.kt`; `FileHelper.kt`; `cast_fragment_add_channel_file.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-import-file-selected-fixture.png` | Chọn fixture `.m3u` trong system picker và xác minh tên file/selected state. |
| Import M3U — thành công | `43:850` (`43:882` backdrop) | `ImportStateHelper.kt`; `cast_dialog_import_success.xml`; `cast_bg_import_success_card.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-import-m3u-success-fixture.png` | Dialog được mở sau import fixture thực; playlist fixture đã dọn khỏi DB. |
| IPTV Player — portrait, điều khiển ẩn | `43:892` | `PlayListActivity.kt`; `cast_activity_play_list.xml`; `cast_view_exo_controller.xml` | PARTIALLY_IMPLEMENTED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/iptv-player-portrait-hidden-final.png` | Không. Đã ẩn cụm seek/play lớn và progress/Cast; giữ play nhỏ ở giữa và fullscreen như frame. |
| IPTV Player — portrait, điều khiển hiện | `43:932` | `PlayListActivity.kt`; `cast_activity_play_list.xml`; `cast_view_exo_controller.xml` | NEEDS_VISUAL_FIX | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/iptv-player-portrait-controls-final.png` | Không. Đã so khớp video frame, title, Cast, transport controls, progress bar, search và bốn hàng kênh. |
| IPTV Player — landscape | `43:980` | `PlayListActivity.kt`; `cast_activity_play_list.xml`; `cast_view_exo_controller_full.xml` | NEEDS_VISUAL_FIX | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/iptv-player-landscape-controller-final.png` | Không. Video phủ viewport; toolbar, transport controls, fullscreen và progress bar được đối chiếu ở 800×360 dp. |
| IPTV Player — landscape, channel drawer | `43:992` | `PlayListActivity.kt`; `cast_activity_play_list.xml`; `cast_view_exo_controller_full.xml`; `cast_item_tv_channel.xml`; `layout-land/cast_item_tv_channel.xml` | NEEDS_VISUAL_FIX | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/iptv-player-landscape-drawer-final.png` | Không. Drawer rộng 252 dp tại x=548 dp; search 228×42 dp, hai row 228×66 dp và khoảng cách 8 dp khớp frame. |
| Media — Photos | `23:933` (`39:601` gallery grid) | `MediaPickerActivity.kt`; `MediaItemAdapter.kt`; `cast_activity_media_picker.xml`; `cast_item_media.xml`; `cast_view_media_type_selector.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-media-photos-fixture.png` | Ảnh gốc lấy từ Figma đã index bằng MediaStore và hiển thị. Hai ảnh runtime có sẵn từ trước còn xuất hiện thêm trong grid. |
| Media — Videos | `23:968` (`39:669` gallery grid) | `MediaPickerActivity.kt`; `MediaItemAdapter.kt`; `cast_activity_media_picker.xml`; `cast_item_media.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-media-videos-fixture.png` | 8 video fixture H.264 có thumbnail/thời lượng xuất hiện; thumbnail dùng ảnh gốc Figma. |
| Media — Choose device | `23:1014` | `MediaPickerActivity.kt`; `DeviceAdapter.kt`; `cast_activity_media_picker.xml`; `cast_item_device.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/media-choose-device.png` | Popup, hàng thiết bị và gallery fixture được đối chiếu; không chọn thiết bị thật. Hàng được highlight vẫn hiện chevron thay cho check để không giả lập receiver đã kết nối. |
| Media — Photo preview | `23:1058` | `MediaPickerActivity.kt`; `cast_activity_media_picker.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-media-photo-preview-fixture.png` | Preview thực mở với ảnh fixture gốc Figma; không xác minh thao tác gửi ảnh tới receiver. |
| Cast — tìm kiếm và có thiết bị | `33:564` | `CastActivity.kt`; `DeviceAdapter.kt`; `cast_activity_cast.xml`; `cast_item_device.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/cast-searching-devices.png` | Đã dựng state Loading + HasDevices bằng danh sách fixture; không có discovery thật. Hàng đầu được chọn/highlight nhưng giữ chevron, không đánh dấu đã kết nối. |
| Cast — không tìm thấy thiết bị | `33:633` | `CastActivity.kt`; `cast_activity_cast.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-cast-no-device-360x800.png` | Đối chiếu trực tiếp với frame `33:633`; không có việc khớp hình còn lại. |
| Cast — mất kết nối | `33:679` | `CastActivity.kt`; `cast_activity_cast.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/cast-lost-connection.png` | Đã dựng state UI mất kết nối với nền status card và hành động Wi-Fi; không tạo kết nối rồi ngắt receiver thật. |
| Cast — PIN trống | `33:725` (`33:794` modal) | `PinDialog.kt`; `cast_dialog_pin.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/cast-pin-empty.png` | Dialog mở bằng callback no-op; không xác thực PIN. Giới hạn trường nhập tăng từ 4 lên 6 ký tự theo frame. |
| Cast — PIN đã nhập | `33:808` (`33:877` modal) | `PinDialog.kt`; `cast_dialog_pin.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/cast-pin-entered.png` | Giá trị `678910` chỉ được đưa vào input trong fixture; không nhấn Verify và không xác thực với TV. |
| Cast — rewarded ad | `33:891` (`33:960` modal) | `RewardedAdDialog.kt`; `cast_dialog_rewarded_ad.xml` | IMPLEMENTED_UNVERIFIED | UI_VERIFIED_WITH_FIXTURE | Có — `docs/figma/screenshots/runtime-seven/cast-rewarded-prompt.png` | Dialog mở với callback no-op; không yêu cầu/phát rewarded ad và không hoàn tất reward. |
| Settings — gồm tùy chọn Language | `57:564` | `SettingFragment.kt`; `MainActivity.kt`; `cast_fragment_setting.xml`; `cast_item_setting_option.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-settings-360x800.png` | Language ở trong Settings vẫn thuộc phạm vi; không mở luồng Language Selection lúc startup. |
| How to use | `57:614` | `HowToUseActivity.kt`; `cast_activity_how_to_use.xml`; `cast_item_how_to_use_step.xml` | IMPLEMENTED_UNVERIFIED | COMPLETED_AND_VERIFIED | Có — `docs/figma/screenshots/runtime-how-to-use-360x800.png` | Đã chụp và so với frame; banner và ba bước hiển thị. |

## Thay đổi lượt này

- `AddChannelUrlFragment.kt`: trường tên không tự điền; tên gợi ý được lưu riêng rồi dùng làm fallback khi gửi form, giữ hành vi auto-name hiện có.
- `cast_strings.xml`: khớp text Figma cho tiêu đề import, placeholder tên/URL và nhãn “Choose M3U file”.
- `AddChannelActivity.kt`: status bar sáng theo nền import, bỏ translucent status flag để màu nền hiển thị đúng.
- `cast_activity_play_list.xml`: Player portrait có margin ngang 16dp và landscape dùng `resize_mode="zoom"`; parent gap dưới player về 0dp.
- `PlayListActivity.kt`: khi vào fullscreen bỏ root top padding theo status bar, khi thoát khôi phục; giữ nguyên playback/event flow.
- `cast_view_exo_controller.xml`, `cast_view_exo_controller_full.xml`: các nút, progress, toolbar và hành vi hiển thị đã được đối chiếu với bốn trạng thái Player; hiện không còn mismatch bố cục đáng kể ở bốn frame.
- `PlayListActivity.kt`: timer portrait ẩn cụm seek/progress/Cast, hiện affordance play 16 dp và giữ fullscreen; khi controller hiện, các nút được khôi phục. Không đổi nguồn phát, chọn kênh, navigation hay callback playback.
- `cast_activity_play_list.xml`, `cast_ic_player_play_overlay.xml`: thêm biểu tượng play nhỏ cho trạng thái portrait controller ẩn.
- Bốn ảnh Player mới nhất được chụp từ emulator sau build cuối tại `docs/figma/screenshots/iptv-player-*-final.png`; đối chiếu lần lượt node `43:892`, `43:932`, `43:980`, `43:992`.
- `layout-land/cast_item_tv_channel.xml`: variant landscape 66dp với thumbnail 48dp; giữ nguyên View IDs và binding.
- `cast_dialog_pin.xml`: giới hạn trường nhập PIN đổi từ 4 thành 6 ký tự để khớp hướng dẫn/mẫu sáu chữ số trong Figma; không thay đổi kiểm tra hoặc callback xác thực.
- `cast_strings.xml`: subtitle PIN đổi thành “Enter the six-digit code shown on your TV.” theo Figma.
- `cast_activity_media_picker.xml`: nền `viewOverlay` chuyển sang trong suốt để gallery không bị dim khi popup thiết bị mở; view/click listener vẫn giữ để chạm ngoài popup có thể đóng.
- Harness `DebugCastUiHarnessActivity` và khai báo Activity trong debug manifest chỉ tồn tại trong lúc chụp; đã gỡ hoàn toàn sau đó. Dùng layout/binding, `DeviceAdapter`, `MediaItemAdapter`, `PinDialog` và `RewardedAdDialog` hiện có; mọi callback là no-op.
- Ảnh fixture Media dùng resource thumbnail đóng gói sẵn và không ghi MediaStore/DB. Các tên TV, IP, mã PIN chỉ tồn tại trong harness tạm; không có receiver giả được khởi tạo hoặc thao tác kết nối.
- Figwright đã xuất frame tham chiếu gốc cho 7 node vào `docs/figma/screenshots/figma-ref/`; screenshot runtime cuối nằm trong `docs/figma/screenshots/runtime-seven/`.
- Thêm ảnh runtime evidence ở `docs/figma/screenshots/*-fixture.png`; media/playlist fixture và harness debug đã được dọn sau capture.
- Thêm `cast_bg_player_control_button.xml`, `cast_bg_player_controller_toolbar.xml`, màu `cast_player_control_overlay`.
- Không sửa Figma, startup screens, business logic playback/casting/import, hoặc 18 trạng thái không có Figma. Không commit/push.

## Build, test và bằng chứng

- `./gradlew :app:installDevDebug` — **BUILD SUCCESSFUL**, APK cài lại thành công trên Pixel 10a emulator sau khi gỡ harness.
- `./gradlew :app:testDevDebugUnitTest` — **BUILD SUCCESSFUL**.
- `git diff --check` — **sạch** sau cập nhật báo cáo.
- 7 screenshot runtime fixture mới được chụp ở 360×800 dp (945×2100 px), ứng với từng trạng thái tại `docs/figma/screenshots/runtime-seven/`. Đã so từng ảnh với ảnh tham chiếu có cùng node ID trong `figma-ref/`.
- Home connected: bố cục, ảnh hero, thẻ và navigation khớp; fixture đặt nhãn Samsung TV và mô tả banner. Không có kết nối thật.
- Media Choose device: header, popup, 3 hàng và gallery fixture được xác minh. Nền gallery không còn bị dim; overlay vẫn nhận chạm ngoài popup. Hàng đầu được highlight nhưng vẫn hiển thị chevron (không check) vì không đánh dấu TV là connected; gallery dùng các thumbnail nội bộ có sẵn, không phải media thật của người dùng.
- Cast searching + devices: card, trạng thái tìm kiếm, danh sách và alternative connection được xác minh. Dữ liệu thiết bị là fixture, không phải discovery mạng; hàng đầu không được đánh dấu đã kết nối.
- Cast lost connection: status card, illustration, text, nút Wi-Fi và alternative connection được xác minh bằng state fixture; không tạo sự kiện mất kết nối từ receiver thật.
- PIN empty / entered: dialog và input state được xác minh; ảnh entered chỉ gán `678910` cho trường UI. Không nhấn Verify, không gọi callback xác thực và không có TV.
- Rewarded ad prompt: dialog/asset/text/nút được xác minh; không yêu cầu quảng cáo, không click và không phát reward.
- Đã xóa các file screenshot tạm trên emulator và gỡ Activity harness khỏi APK cuối. Không đổi font scale, kích thước hoặc density override đã có trước lượt này.
- Luồng H.264 kiểm thử cục bộ đã phát thực để lấy screenshot; M3U test và các playlist `PlayerAudit*` là dữ liệu chỉ trên emulator debug. Không ghi fixture vào tài nguyên hay code production. Trạng thái kích thước emulator được giữ nguyên như thiết lập đã có trước lượt kiểm tra này.

## Kết luận

**ĐÃ KIỂM CHỨNG UI 27/27; E2E 7/7 CÒN CHỜ THIẾT BỊ/DỊCH VỤ THẬT.** Bảy trạng thái còn lại đều có screenshot runtime fixture và được đối chiếu với Figma. Không trạng thái nào trong bảy được tuyên bố end-to-end verified. Chưa thể xác minh receiver discovery/kết nối/mất kết nối, PIN thật hoặc rewarded ad nếu không có receiver/TV và luồng quảng cáo khả dụng. Build, unit test và `git diff --check` hoàn tất thành công sau khi gỡ harness.
