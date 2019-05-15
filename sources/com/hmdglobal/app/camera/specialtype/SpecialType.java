package com.hmdglobal.app.camera.specialtype;

import android.app.Activity;
import android.support.annotation.Nullable;
import com.hmdglobal.app.camera.R;

enum SpecialType {
    UNKNOWN,
    NONE,
    GDEPTH_TYPE(R.string.gdepth_type_id, R.string.gdepth_type_description, R.drawable.ic_gdepth_white_72, null, null, null, ConfigurationImpl.BADGE),
    BOKEH_TYPE(R.string.bokeh_type_id, R.string.bokeh_type_description, R.drawable.ic_bokeh_white_72, null, null, null, ConfigurationImpl.BADGE);
    
    @Nullable
    private final ConfigurationImpl configuration;
    final int descriptionResourceId;
    @Nullable
    private final Class<? extends Activity> editActivityClass;
    final int iconResourceId;
    @Nullable
    private final Class<? extends Activity> interactActivityClass;
    @Nullable
    private Class<? extends Activity> launchActivityClass;
    final int nameResourceId;

    private SpecialType(int nameResourceId, int descriptionResourceId, int iconResourceId, @Nullable Class<? extends Activity> editActivityClass, @Nullable Class<? extends Activity> interactActivityClass, @Nullable Class<? extends Activity> launchActivityClass, @Nullable ConfigurationImpl configuration) {
        this.nameResourceId = nameResourceId;
        this.descriptionResourceId = descriptionResourceId;
        this.iconResourceId = iconResourceId;
        this.editActivityClass = editActivityClass;
        this.interactActivityClass = interactActivityClass;
        this.launchActivityClass = launchActivityClass;
        this.configuration = configuration;
        if (configuration != null) {
            configuration.validate(this);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public ConfigurationImpl getConfiguration() {
        if (this.configuration != null) {
            return this.configuration;
        }
        throw new UnsupportedOperationException();
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public String getEditActivityClassName() {
        return getActivityClassName(this.editActivityClass);
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public String getInteractActivityClassName() {
        return getActivityClassName(this.interactActivityClass);
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public String getLaunchActivityClassName() {
        return getActivityClassName(this.launchActivityClass);
    }

    @Nullable
    private static String getActivityClassName(@Nullable Class<? extends Activity> clazz) {
        if (clazz == null) {
            return null;
        }
        return clazz.getName();
    }
}
