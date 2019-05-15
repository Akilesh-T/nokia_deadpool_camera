package com.google.android.apps.photos.api;

public interface ProcessingMetadataQuery {
    public static final String MEDIA_STORE_ID = "media_store_id";
    public static final String PROGRESS_PERCENTAGE = "progress_percentage";
    public static final String PROGRESS_STATUS = "progress_status";

    public enum ProgressStatus {
        INDETERMINATE(1),
        DETERMINATE(2);
        
        private final int identifier;

        private ProgressStatus(int identifier) {
            this.identifier = identifier;
        }

        public int getIdentifier() {
            return this.identifier;
        }

        public static ProgressStatus fromIdentifier(int identifier) {
            if (identifier == DETERMINATE.getIdentifier()) {
                return DETERMINATE;
            }
            return INDETERMINATE;
        }
    }
}
