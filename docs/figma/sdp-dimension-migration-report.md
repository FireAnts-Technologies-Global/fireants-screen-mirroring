# Kiểm kê và tiến độ chuyển dimension sang SDP

## Phạm vi và trạng thái ban đầu

- Dependency `com.intuit.sdp:sdp-android:1.1.1` đã có trong `app/build.gradle`; không cần thêm
  dependency.
- SDP cung cấp `_1sdp` đến `_600sdp`, cùng dimension âm và các qualifier `sw300dp`, `sw330dp`,
  `sw360dp`... tăng 30dp. Ví dụ `_16sdp` bằng 16dp ở qualifier gốc và 19.2dp ở `sw360dp`.
- SSP `1.1.1` hiện có trong Gradle; yêu cầu dự án là không dùng SSP cho text size. Sẽ gỡ dependency
  khi không còn tham chiếu.
- Working tree sạch khi bắt đầu.
- Đã kiểm kê 487 XML resource files dưới `app/src/main/res` và tìm kiếm 159 file Kotlin/Java trong
  source app.
- Phân loại XML trước migration phát hiện 1,792 giá trị dimension trực tiếp thuộc UI không phải
  quảng cáo: 1,620 `dp`, 171 `sp` và 1 `dip`. Ngoài ra có dimension `px` trong theme quảng cáo, được
  loại khỏi phạm vi.
- Có 15 giá trị UI không thể map trực tiếp vào `_Nsdp`: các giá trị phân số `0.25`, `0.3`, `0.75`,
  `1.5`, `31.937622`, `32.0625` và hai giá trị `1024dp`. Sẽ tạo custom SDP-scaled resources cho
  chúng.

## Điểm Kotlin/Java cần xử lý

- `FigmaTopBarView.kt`: text size chọn theo biến thể 19/20/22, hiện gán bằng API mặc định SP; chuyển
  sang pixel dimension `_19sdp`, `_20sdp`, `_22sdp` để tránh chuyển đổi SP hai lần.
- `MediaPickerActivity.kt`: kích thước icon 24 và khoảng cách 12 đang quy đổi bằng helper DP; đổi
  sang `getDimensionPixelSize(R.dimen._24sdp/_12sdp)`.
- `EditChannelPopup.kt`: khoảng cách popup 4dp đang dùng `applyDimension`; đổi sang SDP resource.
- `NumberPicker.java`: mặc định divider, kích thước, text size và chuyển đổi px↔SP/DP cần dùng SDP
  pixel resources. Thuộc tính explicit trong XML sẽ tiếp tục được ưu tiên.
- `AdExtension.kt` và `ScreenExt.dpToPx()`: được dùng cho kích thước quảng cáo, loại khỏi migration.
- `PlayListActivity` áp dụng system insets động; giá trị này không phải hardcoded dimension và sẽ
  giữ nguyên.

## Khu vực quảng cáo loại trừ

Không chỉnh sửa các ad template/layout: `cast_dialog_rewarded_ad.xml`,
`cast_layout_banner_control.xml`, `cast_layout_native_home.xml`,
`cast_layout_shimmer_native_medium.xml`, `cast_layout_shimmer_native_small.xml`,
`fragment_onboarding_native_full.xml`, toàn bộ `layout_native_*.xml`, `layout_shimmer_native_*.xml`
và `layout_prepare_ads.xml`.

Các drawable chỉ dùng cho ads cũng được loại trừ: `ads_native_common_bg.xml`,
`ads_native_common_cta.xml`, `adx_bg_native_home.xml`, `adx_bg_native_language.xml`,
`adx_border_cta_native_language.xml`, `bg_ad_icon.xml`, `bg_btn_native_cta.xml`,
`bg_native_ads.xml`, `border_cta_native_on_boarding.xml`, `border_cta_native_on_permission.xml`,
`cast_adx_bg_native_home.xml`, `cast_bg_ad_icon.xml`, `cast_bg_native_ad_icon.xml`,
`cast_bg_native_ads.xml` và các drawable `cast_bg_rewarded_*.xml`.

Trong layout màn hình có vùng quảng cáo, chỉ bỏ qua subtree của các container/include quảng cáo như
`flAds`, `frAds`, `frAdsBanner`, `fr_banner`, `fr_ads`, `layoutAds`, `layoutAdsFull`, `shimmerAds`,
`shimmerAdsFull`, `imgCloseAdsFull`, `txtAds` và include tới layout quảng cáo. Các thành phần nội
dung thường có chữ “banner” như `quickStartBanner` và `cardMirrorBanner` không bị xem là quảng cáo.
Style `adsCtaNativeCommon`, `adsBackgroundNativeCommon` và theme `AppTheme.Ads2` giữ nguyên.

## Kế hoạch migration

1. Chuyển dimension XML trực tiếp ở layout, drawable, values và các qualifier, bỏ qua ad
   file/style/subtree.
2. Chuyển text size tham chiếu `@dimen/_Nssp` sang `@dimen/_Nsdp` trong phần UI; không dùng SSP.
3. Giữ tên các custom dimension hiện có nhưng đổi định nghĩa sang SDP alias. Nếu dimension được dùng
   bởi cả ads và UI, tạo alias riêng cho UI và để ad dùng nguyên resource cũ.
4. Chuyển các dimension Kotlin/Java đã liệt kê sang pixel values lấy từ SDP resources.
5. Kiểm tra lại mọi occurrence còn lại, build/test, cài emulator và chụp screenshot các màn đại diện
   portrait/landscape nếu thiết bị khả dụng.

## Tiến độ

- [x] Audit dependency, qualifiers, resource XML và các điểm gán dimension trong Kotlin/Java.
- [ ] Chuyển XML UI và styles.
- [ ] Chuyển Kotlin/Java UI dimensions.
- [ ] Build, unit tests, screenshot/regression checks và kiểm tra residual occurrences.
