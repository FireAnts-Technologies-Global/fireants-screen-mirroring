package com.connectsdk.core;

import androidx.annotation.NonNull;

public class SubtitleInfo {
    private final String url;
    private final String mimeType;
    private final String label;
    private final String language;

    private SubtitleInfo(Builder builder) {
        url = builder.url;
        mimeType = builder.mimeType;
        label = builder.label;
        language = builder.language;
    }

    public String getUrl() {
        return url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getLabel() {
        return label;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubtitleInfo that = (SubtitleInfo) o;

        if (getUrl() != null ? !getUrl().equals(that.getUrl()) : that.getUrl() != null) {
            return false;
        }
        return !(getMimeType() != null ? !getMimeType().equals(that.getMimeType()) : that.getMimeType() != null);

    }

    @Override
    public int hashCode() {
        int result = getUrl() != null ? getUrl().hashCode() : 0;
        result = 31 * result + (getMimeType() != null ? getMimeType().hashCode() : 0);
        return result;
    }

    public static class Builder {
        private final String url;

        private String mimeType;
        private String label;
        private String language;

        public Builder(@NonNull String url) {
            this.url = url;
        }

        public Builder setMimeType(@NonNull String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setLabel(@NonNull String label) {
            this.label = label;
            return this;
        }

        public Builder setLanguage(@NonNull String language) {
            this.language = language;
            return this;
        }

        public SubtitleInfo build() {
            return new SubtitleInfo(this);
        }
    }
}
