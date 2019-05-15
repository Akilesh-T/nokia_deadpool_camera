package com.hmdglobal.app.camera.module;

import android.content.Context;
import com.hmdglobal.app.camera.CaptureModule;
import com.hmdglobal.app.camera.FyuseModule;
import com.hmdglobal.app.camera.LiveBokehModule;
import com.hmdglobal.app.camera.ManualModule;
import com.hmdglobal.app.camera.MicroVideoModule;
import com.hmdglobal.app.camera.MoreModule;
import com.hmdglobal.app.camera.NormalPhotoModule;
import com.hmdglobal.app.camera.NormalVideoModule;
import com.hmdglobal.app.camera.OptimizeBurstPhotoModule;
import com.hmdglobal.app.camera.PanoCaptureModule;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.SlowMotionModule;
import com.hmdglobal.app.camera.SquareModule;
import com.hmdglobal.app.camera.TimeLapsedModule;
import com.hmdglobal.app.camera.VideoCaptureIntentModule;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.ModuleManager;
import com.hmdglobal.app.camera.app.ModuleManager.ModuleAgent;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.GcamHelper;
import com.hmdglobal.app.camera.util.PhotoSphereHelper;
import com.hmdglobal.app.camera.util.RefocusHelper;

public class ModulesInfo {
    private static final boolean ENABLE_CAPTURE_MODULE = DebugPropertyHelper.isCaptureModuleEnabled();
    private static final Tag TAG = new Tag("ModulesInfo");

    public static void setupModules(Context context, ModuleManager moduleManager) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId);
        moduleManager.setDefaultModuleIndex(photoModuleId);
        registerVideoModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_video));
        registerPanoModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_pano));
        registerProModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_pro));
        registerLiveBokehModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_livebokeh));
        registerTimeSlapedModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_time_lapse));
        registerSquareModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_square));
        registerMoreModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_more));
        if (PhotoSphereHelper.hasLightCycleCapture(context)) {
            registerWideAngleModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_panorama));
            registerPhotoSphereModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_photosphere));
        }
        if (RefocusHelper.hasRefocusCapture(context)) {
            registerRefocusModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_refocus));
        }
        if (GcamHelper.hasGcamAsSeparateModule()) {
            registerGcamModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_gcam));
        }
    }

    public static void setupPhotoCaptureIntentModules(Context context, ModuleManager moduleManager) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId);
        moduleManager.setDefaultModuleIndex(photoModuleId);
    }

    public static void setupVideoCaptureIntentModules(Context context, ModuleManager moduleManager) {
        int videoModuleId = context.getResources().getInteger(R.integer.camera_mode_video_capture);
        registVideoCaptureModule(moduleManager, videoModuleId);
        moduleManager.setDefaultModuleIndex(videoModuleId);
    }

    private static void registerProModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MANUAL_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    return new ManualModule(app);
                }

                public boolean needAddToStrip() {
                    return CustomUtil.getInstance().isPanther();
                }
            });
        }
    }

    private static void registerLiveBokehModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return ModulesInfo.ENABLE_CAPTURE_MODULE ^ 1;
            }

            public ModuleController createModule(AppController app) {
                return new LiveBokehModule(app);
            }

            public boolean needAddToStrip() {
                return CustomUtil.getInstance().isPanther();
            }
        });
    }

    private static void registerPanoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return new PanoCaptureModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registerMoreModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MANUAL_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    return new MoreModule(app);
                }

                public boolean needAddToStrip() {
                    return false;
                }
            });
        }
    }

    private static void registerPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return ModulesInfo.ENABLE_CAPTURE_MODULE ^ 1;
            }

            public ModuleController createModule(AppController app) {
                if (ModulesInfo.ENABLE_CAPTURE_MODULE) {
                    return new CaptureModule(app);
                }
                return true ? new OptimizeBurstPhotoModule(app) : new NormalPhotoModule(app);
            }

            public boolean needAddToStrip() {
                return true;
            }
        });
    }

    private static void registerVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return new NormalVideoModule(app);
            }

            public boolean needAddToStrip() {
                return true;
            }
        });
    }

    private static void registerSquareModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return new SquareModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registVideoCaptureModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return new VideoCaptureIntentModule(app);
            }

            public boolean needAddToStrip() {
                return true;
            }
        });
    }

    private static void registerSlowMotionModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_SLOW_MOTION_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    Log.w(ModulesInfo.TAG, "create Slow Motion Module");
                    return new SlowMotionModule(app);
                }

                public boolean needAddToStrip() {
                    return false;
                }
            });
        }
    }

    private static void registerManualModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MANUAL_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    return new ManualModule(app);
                }

                public boolean needAddToStrip() {
                    return true;
                }
            });
        }
    }

    private static void registerParallaxModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_PARALLAX_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    Log.w(ModulesInfo.TAG, "create Parallax Module");
                    return new FyuseModule(app);
                }

                public boolean needAddToStrip() {
                    return true;
                }
            });
        }
    }

    private static void registerMicroVideoModule(ModuleManager moduleManager, final int moduleId) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MICRO_VIDEO_MODULE, false)) {
            moduleManager.registerModule(new ModuleAgent() {
                public int getModuleId() {
                    return moduleId;
                }

                public boolean requestAppForCamera() {
                    return true;
                }

                public ModuleController createModule(AppController app) {
                    Log.w(ModulesInfo.TAG, "create Slow MicroVideo Module");
                    return new MicroVideoModule(app);
                }

                public boolean needAddToStrip() {
                    return false;
                }
            });
        }
    }

    private static void registerWideAngleModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return PhotoSphereHelper.createWideAnglePanoramaModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registerPhotoSphereModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return PhotoSphereHelper.createPanoramaModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registerRefocusModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return RefocusHelper.createRefocusModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registerGcamModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return false;
            }

            public ModuleController createModule(AppController app) {
                return GcamHelper.createGcamModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }

    private static void registerTimeSlapedModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleAgent() {
            public int getModuleId() {
                return moduleId;
            }

            public boolean requestAppForCamera() {
                return true;
            }

            public ModuleController createModule(AppController app) {
                return new TimeLapsedModule(app);
            }

            public boolean needAddToStrip() {
                return false;
            }
        });
    }
}
