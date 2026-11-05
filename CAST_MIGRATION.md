# Cast features on the shared ANR base

Source: `/Users/longtq/Downloads/TNG_Cast_TV`.

The destination retains its Splash → Language → Onboarding → Main entry flow.
`MainActivity` directly extends the shared ANR `BaseActivity` and contains the
original Home host plus consent and force-update handling. All activities,
fragments, dialogs, adapters and ViewModels use the shared ANR base components.

## Source organization

Paths below are relative to `app/src/main/java/com/anrstudio/template/`.

- `ui/component/main/`: Main host, Home UI/state and Settings tab.
- `ui/component/cast/`, `media/`, `webcast/`, `webbrowser/`, `videocontrol/`,
  `iptv/`, `howtouse/`: screens grouped by feature.
- `ui/component/custom/`: shared widgets, including the existing CurvedView.
- `ui/component/dialog/`: shared dialogs; the original feature rate UI is named
  `CastRateDialog` to distinguish it from the base's existing rate dialog.
- `ui/bases/` and `ui/bases/ext/`: the single shared base and UI helpers.
- `casting/`: Google Cast options, mirroring engine/manager and cast service.
- `navigation/`: AppNavigator contract and implementation.
- `app/`: GlobalApp, CastRuntime and feature constants.
- `ads/`: ANR SDK integration and feature placement configuration.
- `di/`: shared dependency injection modules.
- `utils/`: shared preference, connectivity, parsing and cast helpers.

The former `cast/feature`, `cast/ui`, `cast/core` and supporting wrapper
packages have been removed. Package paths, imports, manifest entries, service
references and custom XML view tags follow the new locations. Existing cast
service action strings and preference keys remain unchanged.

## Integration details

Migrated layouts, drawables, fonts, strings and themes retain their `cast_`
resource names to avoid replacing the base's resources. Folder reorganization
only changed custom view class references in eight layouts; their visual
attributes and content were not changed. ShapeView uses the base's existing
`com.genesys.shape` implementation.

Existing `data`, `domain`, `connectsdk` and `youtubeExtractor` modules are reused.
GlobalApp starts CastRuntime for the media HTTP server and cast service. The
manifest registers feature activities/services, the Google Cast provider,
notification permission and cleartext LAN media configuration.

`IptvBaseActivity` extends shared ANR `BaseActivityWithBanner` and retains only
IPTV connectivity/player recovery and back-ad actions. Binding, locale, insets,
back dispatch and banner scheduling belong to the shared base. The IPTV timer
preference store remains distinct from application language/consent preferences.

`ads/AdsManager.kt` is the only ad manager. Feature screens use its banner, native
and interstitial APIs with typed `AdRemoteConfig` placements. Native Home keeps
its original layout and uses the project's shared native renderer/configuration. The base's `ad_config*.json` controls `native_home`,
`banner_all`, `inter_home` and `inter_back`. Added debug placements use test IDs;
added release placements remain disabled until their IDs/flags are configured.
The pre-existing native_home configuration is preserved.

## Validation

Both `:app:assembleDevDebug` and `:app:assembleProdDebug` passed after the
reorganization. Package declarations match source directories. No device or
emulator was attached, so TV discovery, casting, mirroring, permissions and PiP
still require verification on devices.

## Ads consolidation and unused classes

Removed `CastAds` and `FeatureAdConfig`; no additional ads facade remains.
Home native loading uses AdsManager's existing native loader, config mapping and
LiveData/load-state pattern. Banner calls use AdsManager/BaseActivityWithBanner.
Feature interstitials use a generic AdsManager method taking AdUnitConfig and a
continuation, including disabled/purchased/offline/load/show failure paths.
Placement settings remain in the existing AdRemoteConfig and ad_config JSONs.

Removed unused DialogLoadingAd, the obsolete SharePrefUtils/first_open flag from
the old resume-ad flow, the unused Home ChannelAdapter, the unused Main
NoInternetDialog and the sample GuideModel. The active loading dialogs, network
dialogs, IPTV channel adapters and M3U parser remain in use.
