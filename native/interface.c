#include "base64.h"
#include "util.h"
#include <jni.h>
#include <openssl/evp.h>
#include <string.h>

#define KEY_PREFIX "A8P20vWlvfSu3JMO6tBjgr05UvjHAh2x"
#define ERROR "CRYPTO_ERROR"

const EVP_CIPHER *cipher;
const EVP_MD *dgst = NULL;
const unsigned char *salt = NULL;

jstring
    Java_com_igexin_log_restapi_util_CryptoTool_decrypt(JNIEnv *env,
                                                        jobject obj,
                                                        jstring pkg_name,
                                                        jstring content) {
    const char *pkg_char = (*env)->GetStringUTFChars(env, pkg_name, NULL);
    char *key_contact = str_contact(pkg_char, KEY_PREFIX);
    char *key_char = base64_encode(key_contact, (int) strlen(key_contact));

    const char *cipher_text = (*env)->GetStringUTFChars(env, content, NULL);
    char *input = base64_decode(cipher_text, (int) strlen(cipher_text));

    char str[strlen(input)];
    char cipher_len[16];
    sscanf(input, "%[0-9]__%[^.]", cipher_len, str);
    int ciphertext_len = atoi(cipher_len);

    char *de_cipher_text = base64_decode(str, (int) strlen(str));

    unsigned char key[EVP_MAX_KEY_LENGTH];
    unsigned char iv[EVP_MAX_IV_LENGTH];

    OpenSSL_add_all_algorithms();

    cipher = EVP_get_cipherbyname("aes-256-cbc");
    dgst = EVP_get_digestbyname("md5");

    if (!cipher) {
        return (*env)->NewStringUTF(env, ERROR);
    }

    if (!dgst) {
        return (*env)->NewStringUTF(env, ERROR);
    }

    if (!EVP_BytesToKey(cipher, dgst, salt,
                        (unsigned char *) key_char,
                        (int) strlen(key_char), 1, key, iv)) {
        return (*env)->NewStringUTF(env, ERROR);
    }

    unsigned char *plain_text;
    EVP_CIPHER_CTX ctx;
    EVP_CIPHER_CTX_init(&ctx);

    if (!EVP_DecryptInit_ex(&ctx, EVP_aes_256_cbc(), NULL, key, iv)) {
        return (*env)->NewStringUTF(env, ERROR);
    };
    plain_text = malloc(ciphertext_len);

    int de_bytes_written = 0;
    if (!EVP_DecryptUpdate(&ctx, plain_text, &de_bytes_written, (const unsigned char *) de_cipher_text, ciphertext_len)) {
        return (*env)->NewStringUTF(env, ERROR);
    };

    if (!EVP_DecryptFinal_ex(&ctx, plain_text + de_bytes_written, &de_bytes_written)) {
        return (*env)->NewStringUTF(env, ERROR);
    };

    EVP_cleanup();
    EVP_CIPHER_CTX_cleanup(&ctx);

    (*env)->ReleaseStringUTFChars(env, pkg_name, pkg_char);
    (*env)->ReleaseStringUTFChars(env, content, cipher_text);
    free(key_contact);
    free(key_char);
    free(input);
    free(de_cipher_text);

    return (*env)->NewStringUTF(env, (const char *) plain_text);
}

jobject
    Java_com_igexin_log_restapi_util_CryptoTool_generateCert(JNIEnv *env, jobject obj, jint key_len, jstring key_str) {
    OpenSSL_add_all_algorithms();

    cipher = EVP_get_cipherbyname("aes-256-cbc");
    dgst = EVP_get_digestbyname("md5");

    if (!cipher) {
        EVP_cleanup();
        return NULL;
    }

    if (!dgst) {
        EVP_cleanup();
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

    EVP_cleanup();
    (*env)->ReleaseStringUTFChars(env, key_str, key_char);

    return value;
}

jbyteArray
    Java_com_getui_logful_server_util_CryptoTool_decryptUpdate(JNIEnv *env,
                                                               jobject obj,
                                                               jbyteArray key_data,
                                                               jbyteArray data,
                                                               jint data_len) {
    /*
    jboolean a;
    jboolean b;
    jboolean c;

    jbyte *byte1 = (*env)->GetByteArrayElements(env, key_data, &a);
    unsigned char *key = (unsigned char *) (byte1);

    jbyte *byte2 = (*env)->GetByteArrayElements(env, iv_data, &b);
    unsigned char *iv = (unsigned char *) (byte2);

    jbyte *byte3 = (*env)->GetByteArrayElements(env, data, &c);
    unsigned char *cipher_text = (unsigned char *) (byte3);
         */

    jboolean a;
    jbyte *key_byte = (*env)->GetByteArrayElements(env, key_data, &a);
    unsigned char *key = (unsigned char *) (key_byte);

    jboolean b;
    jbyte *data_byte = (*env)->GetByteArrayElements(env, data, &b);
    unsigned char *cipher_text = (unsigned char *) (data_byte);

    EVP_CIPHER_CTX ctx;
    EVP_CIPHER_CTX_init(&ctx);

    if (!EVP_DecryptInit_ex(&ctx, EVP_aes_256_ecb(), NULL, key, NULL)) {
        return NULL;
    }

    // Disable padding
    if (!EVP_CIPHER_CTX_set_padding(&ctx, 0)) {
        return NULL;
    }

    unsigned char *plain_text = malloc(data_len);
    int de_bytes_written = 0;
    if (!EVP_DecryptUpdate(&ctx, plain_text, &de_bytes_written, cipher_text, data_len)) {
        return NULL;
    };

    if (!EVP_DecryptFinal_ex(&ctx, plain_text + de_bytes_written, &de_bytes_written)) {
        return NULL;
    };

    (*env)->ReleaseByteArrayElements(env, key_data, key_byte, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, data, data_byte, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, data_len);
    (*env)->SetByteArrayRegion(env, result, 0, data_len, (jbyte *) plain_text);

    free(plain_text);

    EVP_CIPHER_CTX_cleanup(&ctx);

    return result;
}