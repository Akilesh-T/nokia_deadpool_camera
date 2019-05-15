package com.hmdglobal.app.camera.session;

import java.io.File;
import java.io.IOException;

public interface SessionStorageManager {
    File getSessionDirectory(String str) throws IOException;
}
