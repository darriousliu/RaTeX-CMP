// Tests the packaged raw WASM ABI, including allocation/free and error recovery.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const file = process.argv[2];
if (!file) throw new Error('Usage: node rust-bridge/tests/wasm-smoke.mjs <bridge.wasm>');
const { instance: { exports: api } } = await WebAssembly.instantiate(await readFile(file), {});
assert.equal(api.ratex_cmp_bridge_version(), 1);
assert.equal(api.ratex_cmp_alloc(0), 0);
assert.equal(api.ratex_cmp_alloc(32 * 1024 * 1024 + 1), 0);
const encoder = new TextEncoder();
const decoder = new TextDecoder('utf-8', { fatal: true });
function read(pointer) {
    const memory = new Uint8Array(api.memory.buffer);
    return decoder.decode(memory.subarray(pointer, memory.indexOf(0, pointer)));
}
function render(metrics, latex = '\\text{😊}') {
    const bytes = encoder.encode(JSON.stringify({ latex, displayMode: true, color: [0, 0, 0, 1], metrics }));
    const input = api.ratex_cmp_alloc(bytes.length);
    let output = 0;
    try {
        new Uint8Array(api.memory.buffer).set(bytes, input);
        output = api.ratex_cmp_parse_json(input, bytes.length);
        if (!output) throw new Error(read(api.ratex_cmp_last_error()));
        assert.equal(api.ratex_cmp_last_error(), 0);
        return JSON.parse(read(output));
    } finally {
        if (output) api.ratex_cmp_free(output);
        api.ratex_cmp_dealloc(input, bytes.length);
    }
}
const entry = { codePoint: 0x1f60a, width: 0.82, height: 0.53, depth: 0.12 };
assert.equal(render([entry]).width, 0.82);
for (const entries of [
    [{ ...entry, codePoint: 0xd800 }], [entry, entry], [{ ...entry, width: -1 }],
    [{ ...entry, reserved: 1 }], Array(4097).fill(entry)
]) assert.throws(() => render(entries));
assert.throws(() => render([entry], '\\notACommand'), /parse error/);
for (let i = 0; i < 200; i++) assert.equal(render([entry]).width, 0.82);
assert.deepEqual(render([entry], 'x+1'), render([], 'x+1'));
console.log('WASM bridge ABI: metrics, validation, ownership and recovery passed');
