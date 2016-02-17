#include <jni.h>
#include <openssl/evp.h>
#include <string.h>

const EVP_CIPHER *cipher;
const EVP_MD *dgst = NULL;
const unsigned char *salt = NULL;

jobject
    Java_com_getui_logful_server_util_CryptoTool_security(JNIEnv *env, jclass obj, jstring key_str, jint key_len) {
    OpenSSL_add_all_algorithms();

    cipher = EVP_get_cipherbyname("aes-256-cbc");
    dgst = EVP_get_digestbyname("md5");

    if (!cipher) {
        return NULL;
    }

    if (!dgst) {
        return NULL;
    }

    const char *key_char = (*env)->GetStringUTFChars(env, key_str, NULL);

    unsigned char key[EVP_MAX_KEY_LENGTH];
    unsigned char iv[EVP_MAX_IV_LENGTH];

    jobject value = NULL;
    if (EVP_BytesToKey(cipher, dgst, salt,
                       (unsigned char *) key_char,
                       key_len, 1, key, iv)) {
        jclass cls = (*env)->FindClass(env, "com/getui/logful/server/entity/Certificate");
        jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "([B[B)V");

        jbyteArray keyArray = (*env)->NewByteArray(env, EVP_MAX_KEY_LENGTH);
        jbyteArray ivArray = (*env)->NewByteArray(env, EVP_MAX_IV_LENGTH);

        (*env)->SetByteArrayRegion(env, keyArray, 0, EVP_MAX_KEY_LENGTH, (jbyte *) key);
        (*env)->SetByteArrayRegion(env, ivArray, 0, EVP_MAX_IV_LENGTH, (jbyte *) iv);

        jvalue args[2];
        args[0].l = keyArray;
        args[1].l = ivArray;
        value = (*env)->NewObjectA(env, cls, constructor, args);
    }
    (*env)->ReleaseStringUTFChars(env, key_str, key_char);
    EVP_cleanup();
    return value;
}