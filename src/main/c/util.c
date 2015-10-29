#include "util.h"

char *str_contact(const char *str1, const char *str2) {
    char *result;
    result = (char *) malloc(strlen(str1) + strlen(str2) + 1);
    if (!result) {
        printf("Error: malloc failed in concat! \n");
        exit(EXIT_FAILURE);
    }
    strcpy(result, str1);
    strcat(result, str2);
    return result;
}
