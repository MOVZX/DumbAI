#include "log.h"
#include <stdlib.h>

bool g_debug_enabled = false;

void die(const char *msg)
{
    ALOGE("%s", msg);
    exit(1);
}
