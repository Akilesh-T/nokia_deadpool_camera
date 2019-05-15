package com.hmdglobal.app.camera.app;

import android.content.Context;
import android.util.SparseArray;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.ModuleManager.ModuleAgent;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import java.util.ArrayList;
import java.util.List;

public class ModuleManagerImpl implements ModuleManager {
    private static final Tag TAG = new Tag("ModuleManagerImpl");
    private int mDefaultModuleId = -1;
    private final SparseArray<ModuleAgent> mRegisteredModuleAgents = new SparseArray(2);

    public void registerModule(ModuleAgent agent) {
        if (agent != null) {
            int moduleId = agent.getModuleId();
            if (moduleId == -1) {
                throw new IllegalArgumentException("ModuleManager: The module ID can not be MODULE_INDEX_NONE");
            } else if (this.mRegisteredModuleAgents.get(moduleId) == null) {
                this.mRegisteredModuleAgents.put(moduleId, agent);
                return;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Module ID is registered already:");
                stringBuilder.append(moduleId);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        throw new NullPointerException("Registering a null ModuleAgent.");
    }

    public boolean unregisterModule(int moduleId) {
        if (this.mRegisteredModuleAgents.get(moduleId) == null) {
            return false;
        }
        this.mRegisteredModuleAgents.delete(moduleId);
        if (moduleId == this.mDefaultModuleId) {
            this.mDefaultModuleId = -1;
        }
        return true;
    }

    public List<ModuleAgent> getRegisteredModuleAgents() {
        List<ModuleAgent> agents = new ArrayList();
        for (int i = 0; i < this.mRegisteredModuleAgents.size(); i++) {
            agents.add((ModuleAgent) this.mRegisteredModuleAgents.valueAt(i));
        }
        return agents;
    }

    public List<Integer> getSupportedModeIndexList() {
        List<Integer> modeIndexList = new ArrayList();
        for (int i = 0; i < this.mRegisteredModuleAgents.size(); i++) {
            modeIndexList.add(Integer.valueOf(this.mRegisteredModuleAgents.keyAt(i)));
        }
        return modeIndexList;
    }

    public boolean setDefaultModuleIndex(int moduleId) {
        if (this.mRegisteredModuleAgents.get(moduleId) == null) {
            return false;
        }
        this.mDefaultModuleId = moduleId;
        return true;
    }

    public int getDefaultModuleIndex() {
        return this.mDefaultModuleId;
    }

    public ModuleAgent getModuleAgent(int moduleId) {
        ModuleAgent agent = (ModuleAgent) this.mRegisteredModuleAgents.get(moduleId);
        if (agent == null) {
            return (ModuleAgent) this.mRegisteredModuleAgents.get(this.mDefaultModuleId);
        }
        return agent;
    }

    public int getQuickSwitchToModuleId(int moduleId, SettingsManager settingsManager, Context context) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        int videoModuleId = context.getResources().getInteger(R.integer.camera_mode_video);
        int quickSwitchTo = moduleId;
        if (moduleId == photoModuleId || moduleId == context.getResources().getInteger(R.integer.camera_mode_gcam)) {
            quickSwitchTo = videoModuleId;
        } else if (moduleId == videoModuleId) {
            quickSwitchTo = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
        }
        if (this.mRegisteredModuleAgents.get(quickSwitchTo) != null) {
            return quickSwitchTo;
        }
        return moduleId;
    }
}
