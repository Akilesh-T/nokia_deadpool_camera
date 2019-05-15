package com.hmdglobal.app.camera.specialtype;

import com.google.android.apps.photos.api.Configuration;
import com.google.android.apps.photos.api.IconQuery;

enum ConfigurationImpl {
    BADGE(Configuration.BADGE) {
        /* Access modifiers changed, original: 0000 */
        public void validate(SpecialType specialType) {
            super.validate(specialType);
            boolean z = false;
            ConfigurationImpl.checkArgument(specialType.getEditActivityClassName() == null, "Edit activity must be null");
            ConfigurationImpl.checkArgument(specialType.getInteractActivityClassName() == null, "Interact activity must be null");
            if (specialType.getLaunchActivityClassName() == null) {
                z = true;
            }
            ConfigurationImpl.checkArgument(z, "Launch activity must be null");
        }
    },
    EDIT(Configuration.EDIT) {
        /* Access modifiers changed, original: 0000 */
        public void validate(SpecialType specialType) {
            super.validate(specialType);
            boolean z = false;
            ConfigurationImpl.checkArgument(specialType.getEditActivityClassName() != null, "Edit activity must not be null");
            ConfigurationImpl.checkArgument(specialType.getInteractActivityClassName() == null, "Interact activity must be null");
            if (specialType.getLaunchActivityClassName() == null) {
                z = true;
            }
            ConfigurationImpl.checkArgument(z, "Launch activity must be null");
        }
    },
    INTERACT(Configuration.INTERACT) {
        /* Access modifiers changed, original: 0000 */
        public void validate(SpecialType specialType) {
            super.validate(specialType);
            boolean z = false;
            ConfigurationImpl.checkArgument(specialType.getEditActivityClassName() == null, "Edit activity must be null");
            ConfigurationImpl.checkArgument(specialType.getInteractActivityClassName() != null, "Interact activity must not be null");
            if (specialType.getLaunchActivityClassName() == null) {
                z = true;
            }
            ConfigurationImpl.checkArgument(z, "Launch activity must be null");
        }
    },
    LAUNCH("launch") {
        /* Access modifiers changed, original: 0000 */
        public void validate(SpecialType specialType) {
            super.validate(specialType);
            boolean z = false;
            ConfigurationImpl.checkArgument(specialType.getEditActivityClassName() == null, "Edit activity must be null");
            ConfigurationImpl.checkArgument(specialType.getInteractActivityClassName() == null, "Interact activity must be null");
            if (specialType.getLaunchActivityClassName() != null) {
                z = true;
            }
            ConfigurationImpl.checkArgument(z, "Launch activity must not be null");
        }
    };
    
    private final String key;

    private ConfigurationImpl(String key) {
        this.key = key;
    }

    /* Access modifiers changed, original: 0000 */
    public String getKey() {
        return this.key;
    }

    /* Access modifiers changed, original: 0000 */
    public void validate(SpecialType specialType) {
        checkResourceId(specialType.descriptionResourceId, "description");
        checkResourceId(specialType.iconResourceId, IconQuery.PATH_ICON);
        checkResourceId(specialType.nameResourceId, "name");
    }

    private static void checkResourceId(int resourceId, String name) {
        boolean z = resourceId != 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" must be a valid resource id");
        checkArgument(z, stringBuilder.toString());
    }

    private static void checkArgument(boolean argument, String message) {
        if (!argument) {
            throw new IllegalArgumentException(message);
        }
    }
}
