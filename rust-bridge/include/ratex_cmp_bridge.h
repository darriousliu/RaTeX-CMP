#ifndef RATEX_CMP_BRIDGE_H
#define RATEX_CMP_BRIDGE_H
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif

/* Base-font-size em, not pre-scaled for TeX styles. Layout is 32 bytes on all ABIs. */
typedef struct {
    uint32_t code_point;
    uint32_t reserved; /* Must be zero. */
    double width;
    double height;
    double depth;
} RatexCmpGlyphMetrics;

uint32_t ratex_cmp_bridge_version(void); /* Currently 1. */
/* rgba: four normalized floats, or NULL for black. display_mode: 0 inline, 1 display.
 * entries may be NULL only if count == 0. Input memory is borrowed for this call.
 * Returns owned UTF-8 DisplayList JSON, or NULL on error. */
char* ratex_cmp_parse(const char* latex, int display_mode, const float* rgba,
                     const RatexCmpGlyphMetrics* entries, size_t count);
/* Borrowed thread-local error; read before the next parse on the same thread. */
const char* ratex_cmp_last_error(void);
/* Only free strings returned by ratex_cmp_parse. NULL is allowed. */
void ratex_cmp_free(char* json);

#ifdef __cplusplus
}
#endif
#endif
