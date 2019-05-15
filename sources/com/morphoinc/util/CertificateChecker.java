package com.morphoinc.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.util.Base64;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.security.cert.CertificateException;
import javax.security.cert.CertificateExpiredException;
import javax.security.cert.CertificateNotYetValidException;
import javax.security.cert.X509Certificate;

public final class CertificateChecker {
    private static final PublicKey[] PUBLIC_KEYS = getPublicKeys();

    public enum Result {
        NO_ERROR,
        ERROR_ISSUER_DN,
        ERROR_EXPIRED,
        ERROR_NOT_YET_VALID,
        ERROR_INTERNAL,
        ERROR_INVALID_SIGNATURE
    }

    public static Result check(PackageInfo info) {
        Result ret = Result.ERROR_INTERNAL;
        Signature[] signatureArr = info.signatures;
        int length = signatureArr.length;
        Result ret2 = ret;
        int ret3 = 0;
        while (ret3 < length) {
            try {
                X509Certificate certificate = X509Certificate.getInstance(signatureArr[ret3].toByteArray());
                try {
                    certificate.checkValidity();
                    PublicKey[] publicKeyArr = PUBLIC_KEYS;
                    int length2 = publicKeyArr.length;
                    Result ret4 = ret2;
                    int ret5 = 0;
                    while (ret5 < length2) {
                        try {
                            certificate.verify(publicKeyArr[ret5]);
                            ret2 = Result.NO_ERROR;
                            break;
                        } catch (Exception e) {
                            ret4 = Result.ERROR_INVALID_SIGNATURE;
                            ret5++;
                        }
                    }
                    ret2 = ret4;
                    if (ret2 != Result.NO_ERROR) {
                        break;
                    }
                    ret3++;
                } catch (CertificateExpiredException e2) {
                    return Result.ERROR_EXPIRED;
                } catch (CertificateNotYetValidException e3) {
                    return Result.ERROR_NOT_YET_VALID;
                }
            } catch (CertificateException e4) {
                return Result.ERROR_INTERNAL;
            }
        }
        return ret2;
    }

    public static Result check(Context context) {
        try {
            return check(context.getPackageManager().getPackageInfo(context.getPackageName(), 64));
        } catch (Exception e) {
            return Result.ERROR_INTERNAL;
        }
    }

    private static PublicKey[] getPublicKeys() {
        String[] PUBLIC_KEY_TEXTS = new String[]{"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4HxK+21KNWEPA1wv6qpIVU4hohC9028md6bzUZrtmHA5HBl2un3QG6rxGkm4D6y/BSly4mV7aF4ZJ0YiwwVKHnoHs500I2XgoRn5ABLbvyfkBzS8sMChnPzxt3XLtzfGJ+uxueoZ84M361/52ciSsqYXc9OuF9vjsztAPkMlU97NS7ZNKnlLm51wp3UQu3FUwOGIRVVaOXnw+rDN1mGantIBBaLhWzeM3gUFFHDOBcoojKcOAWPfnKXrBrpnAM4X0nvf18zSvcaACDRIM5O0559qrOQR7GQEiiW85yFyEpOnaddQzPsNz11/T/vsnzw/k1vVxrfOWjnjWMzicXUHZQIDAQAB", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArsypPGKtGShMCRXvtminMzeH4DqVngWNAsgZUWvGvuZz/kFQODykKvUk3HuVbTEO17eSO0/HziqyaIAaNhVc7AwTnd+zT8rVjYKU1cRQ2LqEeVFKOzBBGlRwxj1K8wvlib2hqBmDmLagdFxD80rJE8cUgodiwbc84JUHqRsdLvIuXPmW8FYjrVqOHtzAgMRzpP+a7SYOJ792yg2Z2xME8hS9qn+79935/zcJhcTbmuLFgucvakPHWT1KS0XwFrn6FXRX2hFEn4yTIm/+qajE5Ytcoc5T+8P1Gb+5Rj7DOSlX87jV+g7gBTazoZQhaFGq9bXu3gxg/c0Shg27gQb8VwIDAQAB", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvbLNAjuXSUD7bFaFyapS+9dVY/QM3s8rGkSiok5CTtUneUDDnURRS1Xat15rEOblLoWUvOxI3RnAbaGO2L2JScgDjPZOqIgN/0h5f8niPjunKIdw6akPIFC01Qmdq0sb5N8O8gUnj8klV1QYYPHheDJ8roqmOnJhV6nC/aGysIY/cqBPWILK3odDKckAyB+Rb2i9PJgFzS8WbuPXwT6gC9wwkPqNaE2t/tLc6GXy/t8RQF5cHVPWr/gJc1CZ6LvfIWu7dg0lJpq4IAV2Ib2kYxmP99DBNN2nvZL+GD0enwX1LY2Nwipa3VYkgtTIE85RyYnolztZu5JMFqMDA33hsQIDAQAB"};
        PublicKey[] publicKeys = new PublicKey[PUBLIC_KEY_TEXTS.length];
        int i = 0;
        while (i < PUBLIC_KEY_TEXTS.length) {
            try {
                publicKeys[i] = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_TEXTS[i], 0)));
                i++;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return publicKeys;
    }
}
