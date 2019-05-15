package com.hmdglobal.app.camera.specialtype.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.text.TextUtils;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public final class TrustedPartners {
    private static final String HASH_ALGORITHM = "SHA1";
    private static final String TAG = "TrustedPartners";
    private final PackageManager packageManager;
    private final Set<String> trustedPartnerCertificateHashes;

    public TrustedPartners(Context context, Set<String> trustedPartnerCertificateHashes) {
        this.packageManager = context.getPackageManager();
        this.trustedPartnerCertificateHashes = trustedPartnerCertificateHashes;
    }

    public boolean isTrustedApplication(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "null or empty package name; do not trust");
            }
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            PackageInfo info = this.packageManager.getPackageInfo(packageName, 64);
            if (info.signatures == null || info.signatures.length != 1) {
                if (Log.isLoggable(TAG, 5)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(info.signatures.length);
                    stringBuilder.append(" signatures found for package (");
                    stringBuilder.append(packageName);
                    stringBuilder.append("); do not trust");
                    Log.w(str, stringBuilder.toString());
                }
                return false;
            }
            try {
                MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
                digest.update(info.signatures[0].toByteArray());
                return this.trustedPartnerCertificateHashes.contains(HexConvert.bytesToHex(digest.digest()));
            } catch (NoSuchAlgorithmException e) {
                if (Log.isLoggable(TAG, 6)) {
                    Log.e(TAG, "unable to compute hash using SHA1; do not trust");
                }
                return false;
            }
        } catch (NameNotFoundException e2) {
            if (Log.isLoggable(TAG, 5)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("package not found (");
                stringBuilder.append(packageName);
                stringBuilder.append("); do not trust");
                Log.w(str, stringBuilder.toString());
            }
            return false;
        }
    }

    public boolean isTrustedAuthority(String authority) {
        ProviderInfo info = this.packageManager.resolveContentProvider(authority, 0);
        if (info != null) {
            return isTrustedApplication(info.packageName);
        }
        if (Log.isLoggable(TAG, 5)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("no provider found for ");
            stringBuilder.append(authority);
            stringBuilder.append("; do not trust");
            Log.w(str, stringBuilder.toString());
        }
        return false;
    }
}
